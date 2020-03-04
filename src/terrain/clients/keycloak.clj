(ns terrain.clients.keycloak
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- keycloak-url
  "Builds a Keycloak URL with the given path components."
  [& components]
  (str (apply curl/url (config/keycloak-base-uri) "auth" "realms" (config/keycloak-realm) components)))

(defn get-oidc-certs
  "Retrieves a list of active public certificates from Keycloak."
  []
  (-> (http/get (keycloak-url "protocol" "openid-connect" "certs")
                {:as :json})
      :body
      :keys))
