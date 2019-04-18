(ns terrain.routes.apps.metadata
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps :only [AppIdParam]]
        [common-swagger-api.schema.metadata
         :only [AvuList
                AvuListRequest
                SetAvuRequest]]
        [ring.util.http-response :only [ok]]
        [terrain.util :only [optional-routes]])
  (:require [common-swagger-api.schema.apps.metadata :as schema]
            [terrain.clients.apps.raw :as apps]
            [terrain.util.config :as config]))

(defn app-avu-routes
  []
  (optional-routes
    [#(and (config/app-routes-enabled)
           (config/metadata-routes-enabled))]

    (context "/apps/:app-id/metadata" []
      :tags ["app-metadata"]
      :path-params [app-id :- AppIdParam]

      (GET "/" []
           :return AvuList
           :summary schema/AppMetadataListingSummary
           :description schema/AppMetadataListingDocs
           (ok (apps/list-avus app-id)))

      (POST "/" []
            :body [body AvuListRequest]
            :return AvuList
            :summary schema/AppMetadataUpdateSummary
            :description schema/AppMetadataUpdateDocs
            (ok (apps/update-avus app-id body)))

      (PUT "/" []
           :body [body SetAvuRequest]
           :return AvuList
           :summary schema/AppMetadataSetSummary
           :description schema/AppMetadataSetDocs
           (ok (apps/set-avus app-id body))))))
