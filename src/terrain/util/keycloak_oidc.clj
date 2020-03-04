(ns terrain.util.keycloak-oidc
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.jwt :as jwt]
            [clojure-commons.response :as resp]
            [terrain.clients.keycloak :as kc]
            [terrain.util.config :as config]))

(defn user-from-token
  "Extracts user information from a Keycloak OIDC token."
  [{:keys [jwt-claims]}]
  {:shortUsername (:preferred_username jwt-claims)
   :username      (str (:preferred_username jwt-claims) "@" (config/uid-domain))
   :email         (:email jwt-claims)
   :firstName     (:given_name jwt-claims)
   :lastName      (:family_name jwt-claims)
   :commonName    (:name jwt-claims)
   :entitlement   (:entitlement jwt-claims)})

(def ^:private required-claims
  [:preferred_username :email :given_name :family_name :name :entitlement])

(defn- validate-claims
  "Verifies that all required claims are present in a JWT."
  [jwt]
  (let [missing (into [] (filter (comp nil? jwt) required-claims))]
    (when (seq missing)
      (throw+ (ex-info (str "Missing required JWT claims: " missing)
                       {:type :validation :cause :missing-fields})))))

(defn validate-token
  "Ring middleware to verify that a Keycloak OIDC token is valid. The JWT claims present in the token
   will be extracted and associated with the request."
  [handler token-fn]
  (fn [request]
    (try+
     (if-let [token (token-fn request)]
       (let [claims (jwt/jwk-validate (kc/get-oidc-certs) token)]
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
