(ns terrain.routes.token
  (:require [common-swagger-api.routes]                              ;; Required for :description-file
            [common-swagger-api.schema :refer [context routes GET]]
            [terrain.services.oauth :as oauth]
            [terrain.routes.schemas.token :as token-schema]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare authorization username)

(defn token-routes
  []
  (routes
   (context "/token" []
     :tags ["token"]

     (GET "/" [:as {{:strs [authorization]} :headers}]
       :summary "Obtain OAuth Tokens"
       :return token-schema/AccessTokenResponse
       :description-file "docs/get-token.md"
       (oauth/get-token authorization))

     (GET "/keycloak" [:as {{:strs [authorization]} :headers}]
       :summary "Obtain Keycloak OIDC Tokens"
       :return token-schema/AccessTokenResponse
       :description-file "docs/get-token.md"
       (oauth/get-keycloak-token authorization)))))

(defn admin-token-routes
  []
  (routes
   (context "/admin/token" []
     :tags ["admin-token"]

     (GET "/" [:as {{:strs [authorization]} :headers}]
       :query [{:keys [username]} token-schema/AdminKeycloakTokenParams]
       :summary "Obtain Impersonation Tokens"
       :return token-schema/AccessTokenResponse
       :description-file "docs/get-admin-token.md"
       (oauth/get-admin-token authorization username))

     (GET "/keycloak" [:as {{:strs [authorization]} :headers}]
       :query [{:keys [username]} token-schema/AdminKeycloakTokenParams]
       :summary "Obtain Keycloak OIDC Impersonation Tokens"
       :return token-schema/AccessTokenResponse
       :description-file "docs/get-admin-token.md"
       (oauth/get-keycloak-admin-token authorization username)))))
