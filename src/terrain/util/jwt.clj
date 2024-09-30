(ns terrain.util.jwt
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.jwt :as jwt]
            [clojure-commons.response :as resp]
            [slingshot.slingshot :refer [try+]]
            [terrain.util.config :as config]))

(def ^:private jwt-generator
  (memoize
   (fn []
     (jwt/generator (config/jwt-opts)))))

(def ^:private jwt-validator
  (memoize
   (fn []
     (jwt/validator (config/jwt-opts)))))

(defn jwt-user-from-terrain-user
  [terrain-user]
  {:user        (:shortUsername terrain-user)
   :email       (:email terrain-user)
   :given-name  (:firstName terrain-user)
   :family-name (:lastName terrain-user)
   :common-name (:commonName terrain-user)})

(defn terrain-user-from-jwt-user
  [jwt-user]
  {:shortUsername (:user jwt-user)
   :username      (str (:user jwt-user) "@" (config/uid-domain))
   :email         (:email jwt-user)
   :firstName     (:given-name jwt-user)
   :lastName      (:family-name jwt-user)
   :commonName    (or (:common-name jwt-user)
                      (str (:given-name jwt-user) " " (:family-name jwt-user)))})

(defn terrain-user-from-jwt-claims
  ([jwt-claims]
     (terrain-user-from-jwt-claims jwt-claims jwt/user-from-default-assertion))
  ([jwt-claims user-extraction-fn]
     (terrain-user-from-jwt-user (user-extraction-fn jwt-claims))))

(defn generate-jwt
  [user]
  ((jwt-generator) (jwt-user-from-terrain-user user)))

(defn add-auth-header
  ([user]
     (add-auth-header user {}))
  ([user headers]
     (add-auth-header user headers :X-Iplant-De-Jwt))
  ([user headers header-name]
     (assoc headers
       header-name (generate-jwt user))))

(defn validate-group-membership
  [handler allowed-groups-fn]
  (fn [request]
    (let [allowed-groups (allowed-groups-fn)
          actual-groups  (get-in request [:jwt-claims :org.iplantc.de:entitlement] [])]
      (if (some (partial contains? (set allowed-groups)) actual-groups)
        (handler request)
        (resp/forbidden "You are not in one of the admin groups.")))))

(def ^:private required-fields #{:user :email})

(defn- validate-claims
  [claims user-extraction-fn]
  (let [user    (user-extraction-fn claims)
        missing (into [] (filter (comp string/blank? user) required-fields))]
    (when (seq missing)
      (throw (ex-info (str "Missing required JWT fields: " missing)
                      {:type :validation :cause :missing-fields})))))

(defn validate-jwt-assertion
  ([handler assertion-fn]
     (validate-jwt-assertion handler assertion-fn jwt/user-from-default-assertion))
  ([handler assertion-fn user-extraction-fn]
     (fn [request]
       (try+
        (if-let [assertion (assertion-fn request)]
          (let [claims ((jwt-validator) assertion)]
            (validate-claims claims user-extraction-fn)
            (log/info "user authenticated using a legacy JWT assertion")
            (handler (assoc request :jwt-claims claims)))
          (resp/unauthorized "Custom JWT header not found."))
        (catch [:type :validation] _
          (resp/forbidden (.getMessage (:throwable &throw-context))))))))
