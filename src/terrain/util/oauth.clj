(ns terrain.util.oauth
  (:use [slingshot.slingshot :only [try+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.response :as resp]
            [terrain.clients.oauth :as oauth]
            [terrain.util.config :as config]))

(defn user-from-oauth-profile
  [{{:keys [id attributes]} :oauth-profile}]
  {:shortUsername id
   :username               (str id "@" (config/uid-domain))
   :email                  (first (:email attributes))
   :firstName              (first (:firstName attributes))
   :lastName               (first (:lastName attributes))
   :commonName             (first (:name attributes))})

(def ^:private required-fields [[:attributes :email] [:id]])

(defn- validate-oauth-profile
  [profile]
  (let [missing?       (fn [ks] (empty? (get-in profile ks)))
        missing-fields (into [] (filter missing? required-fields))]
    (when (seq missing-fields)
      (throw (ex-info (str "Missing required OAuth profile fields: " missing-fields)
                      {:type :validation :cause :missing-fields})))))

(defn validate-oauth-token
  [handler token-fn]
  (fn [request]
    (try+
     (if-let [token (token-fn request)]
       (let [profile (oauth/get-profile token)]
         (validate-oauth-profile profile)
         (handler (assoc request :oauth-profile profile)))
       (resp/unauthorized "No OAuth token found."))
     (catch [:status 401] _
       (log/warn (:throwable &throw-context) "Invalid or expired OAuth token.")
       (resp/unauthorized "Invalid or expired OAuth token."))
     (catch [:type :validation] _
       (resp/forbidden (.getMessage (:throwable &throw-context))))
     (catch Object _
       (log/error (:throwable &throw-context) "Unexpected exception.")
       (resp/error-response ce/ERR_UNCHECKED_EXCEPTION "Received an unexpected exception.")))))
