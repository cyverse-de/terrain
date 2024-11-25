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
  ([username]
   (get-user username (get-token)))
  ([username token]
   (let [user-data (http/get (keycloak-admin-url "users")
                             {:query-params {:username username
                                             :exact true}
                              :headers {:authorization (str "Bearer " token)}
                              :as :json})]
     ; the 'exact' query parameter doesn't seem to work on all keycloak versions, so we filter it
     (->> user-data
          (filter (fn [user] (= (:username user) username)))
          first))))

; https://www.keycloak.org/docs-api/26.0.5/rest-api/#_get_adminrealmsrealmusersuser_idsessions
(defn get-user-session
  "Obtains information about the user's current session from keycloak.
  
  This will be a list of maps, which will include user ID, ip address, session ID, and clients at least."
  ([user-id]
   (get-user-session user-id (get-token)))
  ([user-id token]
   (:body (http/get (keycloak-admin-url "users" user-id "sessions")
                    {:headers {:authorization (str "Bearer " token)}
                     :as :json}))))

(defn get-user-session-by-username
  "Same as `get-user-session`, but by username by way of a request to `get-user` first."
  ([username]
   (get-user-session-by-username username (get-token)))
  ([username token]
   (get-user-session (:id (get-user username token)) token)))
