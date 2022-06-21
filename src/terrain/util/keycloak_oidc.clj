(ns terrain.util.keycloak-oidc
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.string :as string]
            [clojure-commons.jwt :as jwt]
            [clojure-commons.response :as resp]
            [terrain.clients.keycloak :as kc]
            [terrain.util.config :as config]
            [otel.otel :as otel]))

(defn- is-service-account?
  [{:keys [preferred_username]}]
  (string/starts-with? preferred_username "service-account-"))

(defn user-from-token
  "Extracts user information from a Keycloak OIDC token."
  [{:keys [jwt-claims]}]
  (if (is-service-account? jwt-claims)
    nil
    {:shortUsername (:preferred_username jwt-claims)
     :username      (str (:preferred_username jwt-claims) "@" (config/uid-domain))
     :email         (:email jwt-claims)
     :firstName     (:given_name jwt-claims)
     :lastName      (:family_name jwt-claims)
     :commonName    (:name jwt-claims)
     :entitlement   (:entitlement jwt-claims)}))

(defn service-account-from-token
  "Extract service account information from a Keycloak OIDC token."
  [{:keys [jwt-claims]}]
  (if (is-service-account? jwt-claims)
    {:username (:preferred_username jwt-claims)
     :roles (:roles (:realm_access jwt-claims))}
    nil))

(def ^:private required-claims
  [:preferred_username :email :given_name :family_name :name :entitlement])

(def ^:private required-service-account-claims
  [:preferred_username :entitlement :realm_access])

(defn- validate-claims
  "Verifies that all required claims are present in a JWT."
  [jwt]
  (let [req-claims (if (is-service-account? jwt)
                       required-service-account-claims
                       required-claims)
        missing (into [] (filter (comp nil? jwt) req-claims))]
    (when (seq missing)
      (throw+ (ex-info (str "Missing required JWT claims: " missing)
                       {:type :validation :cause :missing-fields})))))

(def cached-certs (atom nil))
(def cached-certs-time (atom nil))
(def cache-ttl (* 24 60 60 1000)) ;; 1 day, milliseconds

(defn- update-cert-cache
  []
  (otel/with-span [s ["update-cert-cache"]]
    (let [new-certs (kc/get-oidc-certs)]
      (reset! cached-certs-time (System/currentTimeMillis))
      (reset! cached-certs new-certs))))

(defn- validate-with-cache
  [token]
  (if (and (sequential? @cached-certs)
           (number? @cached-certs-time)
           (< (- (System/currentTimeMillis) @cached-certs-time) cache-ttl)) ;; (now - cached-time) < cache-ttl
    (try+
      (jwt/jwk-validate @cached-certs token)
      (catch Object _
        (jwt/jwk-validate (update-cert-cache) token)))
    (jwt/jwk-validate (update-cert-cache) token)))

(defn validate-token
  "Ring middleware to verify that a Keycloak OIDC token is valid. The JWT claims present in the token
   will be extracted and associated with the request."
  [handler token-fn]
  (fn [request]
    (try+
     (if-let [token (token-fn request)]
       (let [claims (validate-with-cache token)]
         (validate-claims claims)
         (handler (assoc request :jwt-claims claims)))
       (resp/unauthorized "No Keycloak OIDC token found in request."))
     (catch [:type :validation] _
       (resp/forbidden (.getMessage (:throwable &throw-context)))))))

(defn validate-group-membership
  "Ring middleware to verify that a user authenticated with Keycloak belongs to one of the admin groups."
  [handler allowed-groups-fn]
  (fn [request]
    (let [allowed-groups (allowed-groups-fn)
          actual-groups  (get-in request [:jwt-claims :entitlement] [])]
      (if (some (partial contains? (set allowed-groups)) actual-groups)
        (handler request)
        (resp/forbidden "You are not in one of the admin groups.")))))
