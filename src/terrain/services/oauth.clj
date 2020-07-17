(ns terrain.services.oauth
  (:require [clojure.data.codec.base64 :as base64]
            [clojure.string :as string]
            [ring.util.http-response :as http-response]
            [terrain.clients.keycloak :as keycloak]
            [terrain.clients.oauth :as client]
            [terrain.util.service :as service]))

(defn- get-basic-auth-credentials [authorization]
  (when-let [header-fields (some-> authorization (string/split #" "))]
    (when (= (first header-fields) "Basic")
      (some-> (second header-fields)
              (.getBytes)
              base64/decode
              (String.)
              (string/split #":" 2)))))

(defn get-cas-token [authorization]
  (if-let [[username password] (get-basic-auth-credentials authorization)]
    (http-response/ok (update (client/get-token username password) :expires_in #(Integer/parseInt %)))
    (http-response/unauthorized)))

(defn get-keycloak-token [authorization]
  (if-let [[username password] (get-basic-auth-credentials authorization)]
    (http-response/ok (keycloak/get-token username password))
    (http-response/unauthorized)))

;; Make CAS the default identity provider for standard tokens for now.
(def get-token get-cas-token)

(defn get-keycloak-admin-token [authorization username]
  (if-let [[admin-username password] (get-basic-auth-credentials authorization)]
    (http-response/ok (-> (keycloak/get-token admin-username password)
                          :access_token
                          (keycloak/get-impersonation-token username)))
    (http-response/unauthorized)))

;; Make Keycloak the default identity provider for impersonation tokens.
(def get-admin-token get-keycloak-admin-token)
