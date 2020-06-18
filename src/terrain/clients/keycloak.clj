(ns terrain.clients.keycloak
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- keycloak-url
  "Builds a Keycloak URL with the given path components."
  [& components]
  (str (apply curl/url (config/keycloak-base-uri) "realms" (config/keycloak-realm) components)))

(defn get-oidc-certs
  "Retrieves a list of active public certificates from Keycloak."
  []
  (-> (http/get (keycloak-url "protocol" "openid-connect" "certs")
                {:as :json})
      :body
      :keys))

(defn get-token
  "Obtains an authorization token."
  [username password]
  (:body (http/post (keycloak-url "protocol" "openid-connect" "token")
                    {:form-params {:grant_type    "password"
                                   :client_id     (config/keycloak-client-id)
                                   :client_secret (config/keycloak-client-secret)
                                   :username      username
                                   :password      password}
                     :as          :json})))
