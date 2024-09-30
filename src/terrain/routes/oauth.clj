(ns terrain.routes.oauth
  (:require [common-swagger-api.schema :refer [routes GET DELETE]]
            [terrain.clients.apps.raw :as apps]
            [terrain.util.service :as service]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare api-name params)

(defn secured-oauth-routes
  "These routes are callback and general information routes for OAuth authorization codes. The callback needs to
   be secured because we need to associate the access token that we obtain using the authorization code with the
   user. Because of this. These can't be direct callback routes. Instead, the callbacks need to go through a
   servlet in the Discovery Environment backend."
  []
  (routes
   (GET "/oauth/access-code/:api-name" [api-name :as {params :params}]
     (service/success-response (apps/get-oauth-access-token api-name params)))

   (GET "/oauth/redirect-uris" []
     (service/success-response (apps/get-oauth-redirect-uris)))

   (GET "/oauth/token-info/:api-name" [api-name]
     (service/success-response (apps/get-oauth-token-info api-name)))

   (DELETE "/oauth/token-info/:api-name" [api-name]
     (service/success-response (apps/delete-oauth-token-info api-name)))))

;; An alias for OAuth routes without the "/secured" context.
(def oauth-routes secured-oauth-routes)

(defn oauth-admin-routes
  "These routes are general OAuth information routes designed for administrators. They're primarily intended
   for troubleshooting."
  []
  (routes
   (GET "/oauth/token-info/:api-name" [api-name :as {params :params}]
     (service/success-response (apps/get-admin-oauth-token-info api-name params)))

   (DELETE "/oauth/token-info/:api-name" [api-name :as {params :params}]
     (service/success-response (apps/delete-admin-oauth-token-info api-name params)))))
