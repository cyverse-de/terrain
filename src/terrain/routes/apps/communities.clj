(ns terrain.routes.apps.communities
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps
         :only [AppCategoryMetadataAddRequest
                AppCategoryMetadataDeleteRequest
                AppIdParam]]
        [common-swagger-api.schema.metadata :only [AvuList]]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [require-authentication]]
        [terrain.util :only [optional-routes]])
  (:require [common-swagger-api.schema.apps.communities :as schema]
            [terrain.clients.apps.raw :as apps]
            [terrain.util.config :as config]))

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
