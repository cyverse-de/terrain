(ns terrain.routes.oauth
  (:require
   [common-swagger-api.schema :refer [context DELETE GET]]
   [common-swagger-api.schema.oauth :as oauth-schema]
   [ring.util.http-response :refer [ok]]
   [terrain.clients.apps.raw :as apps]
   [terrain.util :refer [optional-routes]]
   [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare api-name params)

(defn secured-oauth-routes
  "These routes are callback and general information routes for OAuth authorization codes. The callback needs to
   be secured because we need to associate the access token that we obtain using the authorization code with the
   user."
  []
  (optional-routes
   [config/app-routes-enabled]

   (context "/oauth" []
     :tags ["oauth"]

     (GET "/access-code/:api-name" []
       :path-params [api-name :- oauth-schema/ApiName]
       :query [params oauth-schema/OAuthCallbackQueryParams]
       :return oauth-schema/OAuthCallbackResponse
       :summary oauth-schema/GetAccessCodeSummary
       :description oauth-schema/GetAccessCodeDescription
       (ok (apps/get-oauth-access-token api-name params)))

     (GET "/redirect-uris" []
       :summary oauth-schema/GetRedirectUrisSummary
       :description oauth-schema/GetRedirectUrisDescription
       :return oauth-schema/RedirectUrisResponse
       (ok (apps/get-oauth-redirect-uris)))

     (GET "/token-info/:api-name" []
       :path-params [api-name :- oauth-schema/ApiName]
       :return oauth-schema/TokenInfo
       :summary oauth-schema/GetTokenInfoSummary
       :description oauth-schema/GetTokenInfoDescription
       (ok (apps/get-oauth-token-info api-name)))

     (DELETE "/token-info/:api-name" []
       :path-params [api-name :- oauth-schema/ApiName]
       :summary oauth-schema/DeleteTokenInfoSummary
       :description oauth-schema/DeleteTokenInfoDescription
       (ok (apps/delete-oauth-token-info api-name))))))

;; An alias for OAuth routes without the "/secured" context.
(def oauth-routes secured-oauth-routes)

(defn oauth-admin-routes
  "These routes are general OAuth information routes designed for administrators. They're primarily intended
   for troubleshooting."
  []
  (optional-routes
   [#(and (config/admin-routes-enabled)
          (config/app-routes-enabled))]

   (context "/oauth/token-info/:api-name" []
     :tags ["admin-oauth"]
     :path-params [api-name :- oauth-schema/ApiName]

     (GET "/" []
       :query [params oauth-schema/TokenInfoProxyParams]
       :return oauth-schema/AdminTokenInfo
       :summary oauth-schema/AdminGetTokenInfoSummary
       :description oauth-schema/AdminGetTokenInfoDescription
       (ok (apps/get-admin-oauth-token-info api-name params)))

     (DELETE "/" []
       :query [params oauth-schema/TokenInfoProxyParams]
       :summary oauth-schema/AdminDeleteTokenInfoSummary
       :description oauth-schema/AdminDeleteTokenInfoDescription
       (ok (apps/delete-admin-oauth-token-info api-name params))))))
