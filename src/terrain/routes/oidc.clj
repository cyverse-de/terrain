(ns terrain.routes.oidc
  (:require [common-swagger-api.schema :refer [context routes GET POST]]
            [terrain.services.oidc :as oidc]))

(defn oidc-routes
  []
  (routes
   (context "/oidc" []
     :tags ["oidc"]

     (GET "/login" [:as request]
       :summary "Initiate OIDC Authorization Code Flow"
       :description "Redirects the browser to Keycloak to begin the OIDC Authorization Code Flow. Upon successful
       authentication, Keycloak redirects back to the callback endpoint."
       (oidc/login request))

     (GET "/callback" [:as request]
       :summary "OIDC Authorization Code Flow Callback"
       :description "Handles the redirect from Keycloak after authentication. Verifies the state parameter,
       exchanges the authorization code for tokens, stores the tokens in HttpOnly cookies, and redirects to the
       configured landing URI."
       (oidc/callback request))

     (POST "/refresh" [:as request]
       :summary "Refresh OIDC Tokens"
       :description "Uses the refresh token stored in the request's cookies to obtain a fresh access token and
       resets the token cookies."
       (oidc/refresh request))

     (GET "/logout" [:as request]
       :summary "OIDC Logout"
       :description "Clears the OIDC token cookies and redirects the browser to the configured landing URI."
       (oidc/logout request)))))
