(ns terrain.clients.keycloak
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [otel.otel :as otel]
            [terrain.util.config :as config]))

(defn- keycloak-url
  "Builds a Keycloak URL with the given path components."
  [& components]
  (str (apply curl/url (config/keycloak-base-uri) "realms" (config/keycloak-realm) components)))

(defn get-oidc-certs
  "Retrieves a list of active public certificates from Keycloak."
  []
  (otel/with-span [s ["get-oidc-certs" {:kind :client}]]
    (-> (http/get (keycloak-url "protocol" "openid-connect" "certs")
                  {:as :json})
        :body
        :keys)))

(defn get-token
  "Obtains an authorization token."
  [username password]
  (otel/with-span [s ["get-token" {:kind :client}]]
    (:body (http/post (keycloak-url "protocol" "openid-connect" "token")
                      {:form-params {:grant_type    "password"
                                     :client_id     (config/keycloak-client-id)
                                     :client_secret (config/keycloak-client-secret)
                                     :username      username
                                     :password      password}
                       :as          :json}))))

(defn get-impersonation-token
  "Obtains an impersonation token for troubleshooting purposes."
  [subject-token username]
  (otel/with-span [s ["get-impersonation-token" {:kind :client}]]
    (:body (http/post (keycloak-url "protocol" "openid-connect" "token")
                      {:form-params {:grant_type           "urn:ietf:params:oauth:grant-type:token-exchange"
                                     :client_id            (config/keycloak-client-id)
                                     :client_secret        (config/keycloak-client-secret)
                                     :subject_token        subject-token
                                     :requested_token_type "urn:ietf:params:oauth:token-type:access_token"
                                     :requested_subject    username}
                       :as          :json}))))
