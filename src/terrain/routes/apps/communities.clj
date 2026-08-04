(ns terrain.routes.apps.communities
  (:require [common-swagger-api.schema :refer [context DELETE POST]]
            [common-swagger-api.schema.apps :refer [AppIdParam]]
            [common-swagger-api.schema.apps.communities :as schema]
            [common-swagger-api.schema.metadata :refer [AvuList]]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [require-authentication]]
            [terrain.clients.apps.raw :as apps]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to get rid of lint warnings for path and query parameter bindings.
(declare body app-id community-id)

(defn app-community-tag-routes
  []
  (optional-routes
   [#(and (config/app-routes-enabled)
          (config/metadata-routes-enabled))]

   (context "/apps/:app-id/communities" []
     :tags ["app-community-tags"]
     :path-params [app-id :- AppIdParam]

     (DELETE "/:community-id" []
       :middleware [require-authentication]
       :path-params [community-id :- schema/CommunityIdPathParam]
       :summary schema/AppCommunityDeleteSummary
       :description schema/AppCommunityDeleteDocs
       (ok (apps/remove-app-from-community app-id community-id)))

     (DELETE "/" []
       :middleware [require-authentication]
       :body [body schema/AppCommunityListRequest]
       :summary schema/AppCommunityMetadataDeleteSummary
       :description schema/AppCommunityMetadataDeleteDocs
       (ok (apps/remove-app-from-communities app-id body)))

     (POST "/" []
       :middleware [require-authentication]
       :body [body schema/AppCommunityListRequest]
       :return AvuList
       :summary schema/AppCommunityAddSummary
       :description schema/AppCommunityAddDocs
       (ok (apps/update-app-communities app-id body))))))
