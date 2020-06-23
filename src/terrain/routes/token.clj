(ns terrain.routes.token
  (:use [common-swagger-api.schema :only [context routes GET]]
        [terrain.routes.schemas.token]
        [terrain.services.oauth :as oauth])
  (:require [common-swagger-api.routes]))                   ;; Required for :description-file

(defn token-routes
  []
  (routes
   (context "/token" []
     :tags ["token"]

     (GET "/" [:as {{:strs [authorization]} :headers}]
       :summary "Obtain OAuth Tokens"
       :return AccessTokenResponse
       :description-file "docs/get-token.md"
       (oauth/get-token authorization))

     (GET "/cas" [:as {{:strs [authorization]} :headers}]
       :summary "Obtain OAuth Tokens"
       :return AccessTokenResponse
       :description-file "docs/get-token.md"
       (oauth/get-cas-token authorization))

     (GET "/keycloak" [:as {{:strs [authorization]} :headers}]
       :summary "Obtain Keycloak OIDC Tokens"
       :return AccessTokenResponse
       :description-file "docs/get-token.md"
       (oauth/get-keycloak-token authorization)))))

(defn admin-token-routes
  []
  (routes
   (context "/admin/token" []
     :tags ["admin-token"]

     (GET "/" [:as {{:strs [authorization]} :headers}]
       :query [{:keys [username]} AdminKeycloakTokenParams]
       :summary "Obtain Impersonation Tokens"
       :return AccessTokenResponse
       :description-file "docs/get-admin-token.md"
       (oauth/get-admin-token authorization username))

     (GET "/keycloak" [:as {{:strs [authorization]} :headers}]
       :query [{:keys [username]} AdminKeycloakTokenParams]
       :summary "Obtain Keycloak OIDC Impersonation Tokens"
       :return AccessTokenResponse
       :description-file "docs/get-admin-token.md"
       (oauth/get-keycloak-admin-token authorization username)))))
