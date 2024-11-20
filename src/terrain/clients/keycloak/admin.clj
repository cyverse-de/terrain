(ns terrain.clients.keycloak.admin
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- keycloak-admin-url
  "Builds a Keycloak admin API URL with the given path components."
  [& components]
  (str (apply curl/url (config/keycloak-admin-base-uri) "realms" (config/keycloak-realm) components)))

(defn- keycloak-admin-token-url
  "Like keycloak-admin-url but for the 'master' realm to get a token to use with the API"
  [& components]
  (str (apply curl/url (config/keycloak-admin-base-uri) "realms" "master" components)))

(defn get-token
  "Obtains authorization token data for the admin service account."
  []
  (:body (http/post (keycloak-admin-token-url "protocol" "openid-connect" "token")
                    {:form-params {:grant_type    "client_credentials"
                                   :client_id     (config/keycloak-client-id)
                                   :client_secret (config/keycloak-client-secret)}
                     :as          :json})))

; https://www.keycloak.org/docs-api/26.0.5/rest-api/#_get_adminrealmsrealmusers
(defn get-user
  "Obtains user information from keycloak
  
  This will be a map including keys at least :username and :id, which should be
  what we need to make further requests"
  [username]
  (let [user-data (http/get (keycloak-admin-url "users")
                            {:query-params {:username username
                                            :exact true}
                             :headers {:authorization "Bearer " (get-token)}
                             :as :json})]
    ; the 'exact' query parameter doesn't seem to work on all keycloak versions, so we filter it
    (->> user-data
         (filter (fn [user] (= (:username obj) username)))
         first)))
