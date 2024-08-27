(ns terrain.util.oauth
  (:use [slingshot.slingshot :only [try+]])
  (:require [clojure-commons.exception-util :as cx]
            [clojure-commons.response :as resp]
            [ring.util.http-response :as http-resp]
            [terrain.clients.oauth :as oauth]
            [terrain.util.config :as config]))

(defn user-from-oauth-profile
  [{{:keys [id attributes]} :oauth-profile}]
  (let [get-attr (fn [k] (let [v (get attributes k)] (if (sequential? v) (first v) v)))]
    {:shortUsername id
     :username      (str id "@" (config/uid-domain))
     :email         (get-attr :email)
     :firstName     (get-attr :firstName)
     :lastName      (get-attr :lastName)
     :commonName    (get-attr :name)}))

(def ^:private required-fields [[:attributes :email] [:id]])

(defn- validate-oauth-profile
  [profile]
  (let [missing?       (fn [ks] (empty? (get-in profile ks)))
        missing-fields (into [] (filter missing? required-fields))]
    (when (seq missing-fields)
      (throw (ex-info (str "Missing required OAuth profile fields: " missing-fields)
                      {:type :validation :cause :missing-fields})))))

(defn validate-group-membership
  [handler allowed-groups-fn]
  (fn [request]
    (let [allowed-groups (allowed-groups-fn)
          actual-groups  (get-in request [:oauth-profile :attributes :entitlement] [])]
      (if (some (partial contains? (set allowed-groups)) actual-groups)
        (handler request)
        (resp/forbidden "You are not in one of the admin groups.")))))

(defn get-oauth-profile
  [token]
  (try+
   (doto (oauth/get-profile token)
     (validate-oauth-profile))
   (catch [:status 401] _
     (cx/unauthorized "Invalid or expired OAuth token."))
   (catch [:type :validation] _
     (cx/forbidden (.getMessage (:throwable &throw-context))))
   (catch Object _
     (cx/internal-system-error "Received an unexpected exception."))))

(defn validate-oauth-token
  [handler token-fn]
  (fn [request]
    (if-let [token (token-fn request)]
      (let [profile (get-oauth-profile token)]
        (handler (assoc request :oauth-profile profile)))
      (resp/unauthorized "No OAuth token found."))))
