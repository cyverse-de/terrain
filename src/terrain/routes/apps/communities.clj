(ns terrain.routes.apps.communities
  (:require [common-swagger-api.schema :refer [context DELETE POST]]
            [common-swagger-api.schema.apps
             :refer [AppCategoryMetadataAddRequest
                     AppCategoryMetadataDeleteRequest
                     AppIdParam]]
            [common-swagger-api.schema.apps.communities :as schema]
            [common-swagger-api.schema.metadata :refer [AvuList]]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [require-authentication]]
            [terrain.clients.apps.raw :as apps]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to get rid of lint warnings for path and query parameter bindings.
(declare body app-id)

(defn app-community-tag-routes
  []
  (optional-routes
    [#(and (config/app-routes-enabled)
           (config/metadata-routes-enabled))]

    (context "/apps/:app-id/communities" []
      :tags ["app-community-tags"]
      :path-params [app-id :- AppIdParam]

      (DELETE "/" []
              :middleware [require-authentication]
              :body [body AppCategoryMetadataDeleteRequest]
              :summary schema/AppCommunityMetadataDeleteSummary
              :description schema/AppCommunityMetadataDeleteDocs
              (ok (apps/remove-app-from-communities app-id body)))

      (POST "/" []
            :middleware [require-authentication]
            :body [body AppCategoryMetadataAddRequest]
            :return AvuList
            :summary schema/AppCommunityMetadataAddSummary
            :description schema/AppCommunityMetadataAddDocs
            (ok (apps/update-app-communities app-id body))))))
