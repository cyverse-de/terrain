(ns terrain.services.oidc
  (:require [ring.util.http-response :as http-response]
            [terrain.clients.keycloak :as keycloak]
            [terrain.util.config :as config]
            [terrain.util.keycloak-oidc :as keycloak-oidc])
  (:import [java.security SecureRandom]
           [java.util Base64]))

(def ^:private state-cookie
  "The name of the short-lived cookie used to guard against CSRF during the Authorization Code Flow."
  "de-oidc-state")

(def ^:private ^SecureRandom secure-random (SecureRandom.))

(defn- generate-state
  "Generates a random, URL-safe state value for the Authorization Code Flow."
  []
  (let [buf (byte-array 32)]
    (.nextBytes secure-random buf)
    (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) buf)))

(defn- token-cookie
  "Builds a cookie map for an OIDC token."
  [value max-age]
  {:value     value
   :path      "/"
   :http-only true
   :secure    (config/secure-cookies?)
   :same-site :lax
   :max-age   max-age})

(defn- expired-cookie
  "Builds a cookie map that immediately expires a cookie."
  []
  {:value     ""
   :path      "/"
   :http-only true
   :secure    (config/secure-cookies?)
   :same-site :lax
   :max-age   0})

(defn login
  "Initiates the OIDC Authorization Code Flow by redirecting the browser to Keycloak. A random state value is
   generated and stored in a short-lived cookie so that it can be verified when Keycloak redirects back to the
   callback endpoint."
  [_]
  (let [state (generate-state)]
    (-> (http-response/found (keycloak/authorization-code-url state))
        (assoc-in [:cookies state-cookie] (token-cookie state 300)))))

(defn callback
  "Handles the redirect from Keycloak after the user authenticates. The state parameter is verified against the
   state cookie, the authorization code is exchanged for tokens, the tokens are stored in HttpOnly cookies, and
   the browser is redirected to the configured landing URI."
  [{:keys [params cookies]}]
  (let [code         (:code params)
        state        (:state params)
        cookie-state (get-in cookies [state-cookie :value])]
    (cond
      (or (nil? code) (nil? state))
      (http-response/bad-request "Missing authorization code or state parameter.")

      (or (nil? cookie-state) (not= state cookie-state))
      (http-response/bad-request "Invalid OIDC state parameter.")

      :else
      (let [{:keys [access_token refresh_token expires_in refresh_expires_in]}
            (keycloak/exchange-code-for-token code)]
        (-> (http-response/found (config/oidc-landing-uri))
            (assoc-in [:cookies keycloak-oidc/access-token-cookie] (token-cookie access_token expires_in))
            (assoc-in [:cookies keycloak-oidc/refresh-token-cookie] (token-cookie refresh_token refresh_expires_in))
            (assoc-in [:cookies state-cookie] (expired-cookie)))))))

(defn refresh
  "Obtains a fresh access token using the refresh token stored in the request's cookies and resets the token
   cookies."
  [{:keys [cookies]}]
  (if-let [refresh-token (get-in cookies [keycloak-oidc/refresh-token-cookie :value])]
    (let [{:keys [access_token refresh_token expires_in refresh_expires_in]}
          (keycloak/refresh-token refresh-token)]
      (-> (http-response/ok {:expires_in expires_in})
          (assoc-in [:cookies keycloak-oidc/access-token-cookie] (token-cookie access_token expires_in))
          (assoc-in [:cookies keycloak-oidc/refresh-token-cookie] (token-cookie refresh_token refresh_expires_in))))
    (http-response/unauthorized "No refresh token found in request.")))

(defn logout
  "Clears the OIDC token cookies and redirects the browser to the configured landing URI."
  [_]
  (-> (http-response/found (config/oidc-landing-uri))
      (assoc-in [:cookies keycloak-oidc/access-token-cookie] (expired-cookie))
      (assoc-in [:cookies keycloak-oidc/refresh-token-cookie] (expired-cookie))))
