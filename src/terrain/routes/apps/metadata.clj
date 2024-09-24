(ns terrain.routes.apps.metadata
  (:require [common-swagger-api.schema :refer [context GET POST PUT]]
            [common-swagger-api.schema.apps :refer [AppIdParam]]
            [common-swagger-api.schema.apps.metadata :as schema]
            [common-swagger-api.schema.metadata
             :refer [AvuList
                     AvuListRequest
                     SetAvuRequest]]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [require-authentication]]
            [terrain.clients.apps.raw :as apps]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare app-id body)

(defn app-avu-routes
  []
  (optional-routes
    [#(and (config/app-routes-enabled)
           (config/metadata-routes-enabled))]

    (context "/apps/:app-id/metadata" []
      :tags ["app-metadata"]
      :path-params [app-id :- AppIdParam]

      (GET "/" []
           :middleware [require-authentication]
           :return AvuList
           :summary schema/AppMetadataListingSummary
           :description schema/AppMetadataListingDocs
           (ok (apps/list-avus app-id)))

      (POST "/" []
            :middleware [require-authentication]
            :body [body AvuListRequest]
            :return AvuList
            :summary schema/AppMetadataUpdateSummary
            :description schema/AppMetadataUpdateDocs
            (ok (apps/update-avus app-id body)))

      (PUT "/" []
           :middleware [require-authentication]
           :body [body SetAvuRequest]
           :return AvuList
           :summary schema/AppMetadataSetSummary
           :description schema/AppMetadataSetDocs
           (ok (apps/set-avus app-id body))))))
