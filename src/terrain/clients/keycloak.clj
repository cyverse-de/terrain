(ns terrain.clients.keycloak
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- keycloak-url
  "Builds a Keycloak URL with the given path components."
  [& components]
  (str (apply curl/url (config/keycloak-base-uri) "realms" (config/keycloak-realm) components)))

(defn authorization-endpoint
  "Returns the Keycloak OIDC authorization endpoint URL."
  []
  (keycloak-url "protocol" "openid-connect" "auth"))

(defn token-endpoint
  "Returns the Keycloak OIDC token endpoint URL."
  []
  (keycloak-url "protocol" "openid-connect" "token"))

(defn authorization-code-url
  "Builds the Keycloak authorization endpoint URL used to initiate the OIDC Authorization Code Flow."
  [state]
  (-> (curl/url (authorization-endpoint))
      (assoc :query {:response_type "code"
                     :client_id     (config/keycloak-client-id)
                     :redirect_uri  (config/oidc-redirect-uri)
                     :scope         (config/oidc-scopes)
                     :state         state})
      str))

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
  (:body (http/post (token-endpoint)
                    {:form-params {:grant_type    "password"
                                   :client_id     (config/keycloak-client-id)
                                   :client_secret (config/keycloak-client-secret)
                                   :username      username
                                   :password      password}
                     :as          :json})))

(defn get-impersonation-token
  "Obtains an impersonation token for troubleshooting purposes."
  [subject-token username]
  (:body (http/post (token-endpoint)
                    {:form-params {:grant_type           "urn:ietf:params:oauth:grant-type:token-exchange"
                                   :client_id            (config/keycloak-client-id)
                                   :client_secret        (config/keycloak-client-secret)
                                   :subject_token        subject-token
                                   :requested_token_type "urn:ietf:params:oauth:token-type:access_token"
                                   :requested_subject    username}
                     :as          :json})))

(defn exchange-code-for-token
  "Exchanges an OIDC authorization code for an access token using the Authorization Code grant."
  [code]
  (:body (http/post (token-endpoint)
                    {:form-params {:grant_type    "authorization_code"
                                   :client_id     (config/keycloak-client-id)
                                   :client_secret (config/keycloak-client-secret)
                                   :code          code
                                   :redirect_uri  (config/oidc-redirect-uri)}
                     :as          :json})))

(defn refresh-token
  "Obtains a new access token from a refresh token using the Refresh Token grant."
  [refresh-token]
  (:body (http/post (token-endpoint)
                    {:form-params {:grant_type    "refresh_token"
                                   :client_id     (config/keycloak-client-id)
                                   :client_secret (config/keycloak-client-secret)
                                   :refresh_token refresh-token}
                     :as          :json})))
