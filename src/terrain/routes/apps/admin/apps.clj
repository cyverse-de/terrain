(ns terrain.routes.apps.admin.apps
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps.admin.categories
         :only [AppCategorizationDocs
                AppCategorizationRequest
                AppCategorizationSummary]]
        [common-swagger-api.schema.integration-data
         :only [IntegrationData
                IntegrationDataIdPathParam]]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.admin :only [AdminAppSearchParams]]
        [terrain.util :only [optional-routes]])
  (:require [common-swagger-api.routes]                     ;; for :description-file
            [common-swagger-api.schema.apps :as apps-schema]
            [common-swagger-api.schema.apps.admin.apps :as schema]
            [terrain.clients.apps.raw :as apps]
            [terrain.util.config :as config]))

(defn admin-apps-routes
  []
  (optional-routes
   [#(and (config/admin-routes-enabled)
          (config/app-routes-enabled))]

   (context "/apps" []
     :tags ["admin-apps"]

     (GET "/" []
       :query [params AdminAppSearchParams]
       :summary apps-schema/AppListingSummary
       :return schema/AdminAppListing
       :description schema/AppListingDocs
       (ok (apps/admin-get-apps params)))

     (POST "/" []
       :body [body AppCategorizationRequest]
       :summary AppCategorizationSummary
       :description AppCategorizationDocs
       (ok (apps/categorize-apps body)))

     (GET "/publication-requests" []
       :query [params schema/AppPublicationRequestSearchParams]
       :summary schema/AppPublicationRequestsSummary
       :description schema/AppPublicationRequestsDocs
       :return schema/AppPublicationRequestListing
       (ok (apps/list-app-publication-requests params)))

     (POST "/shredder" []
       :body [body apps-schema/AppDeletionRequest]
       :summary schema/AppShredderSummary
       :description schema/AppShredderDocs
       (ok (apps/permanently-delete-apps body)))

     (context "/:system-id/:app-id" []
       :path-params [system-id :- apps-schema/SystemId
                     app-id :- apps-schema/StringAppIdParam]

       (DELETE "/" []
         :summary apps-schema/AppDeleteSummary
         :description schema/AppDeleteDocs
         (ok (apps/admin-delete-app system-id app-id)))

       (PATCH "/" []
         :body [body schema/AdminAppPatchRequest]
         :return schema/AdminAppDetails
         :summary schema/AdminAppPatchSummary
         :description-file "docs/apps/admin/app-label-update.md"
         (ok (apps/admin-update-app system-id app-id body)))

       (GET "/details" []
         :return schema/AdminAppDetails
         :summary apps-schema/AppDetailsSummary
         :description schema/AppDetailsDocs
         (ok (apps/get-admin-app-details system-id app-id)))

       (PATCH "/documentation" []
         :body [body apps-schema/AppDocumentationRequest]
         :return apps-schema/AppDocumentation
         :summary apps-schema/AppDocumentationUpdateSummary
         :description schema/AppDocumentationUpdateDocs
         (ok (apps/admin-edit-app-docs system-id app-id body)))

       (POST "/documentation" []
         :body [body apps-schema/AppDocumentationRequest]
         :return apps-schema/AppDocumentation
         :summary apps-schema/AppDocumentationAddSummary
         :description schema/AppDocumentationAddDocs
         (ok (apps/admin-add-app-docs system-id app-id body)))

       (PUT "/integration-data/:integration-data-id" []
         :path-params [integration-data-id :- IntegrationDataIdPathParam]
         :return IntegrationData
         :summary schema/AppIntegrationDataUpdateSummary
         :description schema/AppIntegrationDataUpdateDocs
         (ok (apps/update-app-integration-data system-id app-id integration-data-id)))

       (POST "/publish" []
         :body [body apps-schema/PublishAppRequest]
         :summary apps-schema/PublishAppSummary
         :description apps-schema/PublishAppDocs
         (ok (apps/admin-publish-app system-id app-id body)))))))
