(ns terrain.routes.apps.admin.apps
  (:require [common-swagger-api.routes]                     ;; for :description-file
            [common-swagger-api.schema :refer [context GET POST DELETE PATCH PUT]]
            [common-swagger-api.schema.apps.admin.categories
             :refer [AppCategorizationDocs
                     AppCategorizationRequest
                     AppCategorizationSummary]]
            [common-swagger-api.schema.apps :as apps-schema]
            [common-swagger-api.schema.apps.admin.apps :as schema]
            [common-swagger-api.schema.apps.permission :as permission-schema]
            [common-swagger-api.schema.integration-data
             :refer [IntegrationData
                    IntegrationDataIdPathParam]]
            [ring.util.http-response :refer [ok]]
            [terrain.clients.apps.raw :as apps]
            [terrain.routes.schemas.admin :refer [AdminAppSearchParams]]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Variable declarations to prevent unresolved symbol warnings for compojure bindings from clj-kondo.
(declare params)
(declare body)
(declare system-id)
(declare app-id)
(declare integration-data-id)
(declare version-id)

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

     (POST "/sharing" []
           :body [body permission-schema/AppSharingRequest]
           :return permission-schema/AppSharingResponse
           :summary permission-schema/AppSharingSummary
           :description permission-schema/AppSharingDocs
           (ok (apps/admin-share body)))

     (POST "/unsharing" []
           :body [body permission-schema/AppUnsharingRequest]
           :return permission-schema/AppUnsharingResponse
           :summary permission-schema/AppUnsharingSummary
           :description permission-schema/AppUnsharingDocs
           (ok (apps/admin-unshare body)))

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

       (DELETE "/blessing" []
         :summary schema/RemoveAppBlessingSummary
         :description schema/RemoveAppBlessingDescription
         (apps/admin-remove-app-blessing system-id app-id)
         (ok))

       (POST "/blessing" []
         :summary schema/BlessAppSummary
         :description schema/BlessAppDescription
         (apps/admin-bless-app system-id app-id)
         (ok))

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
         (ok (apps/admin-publish-app system-id app-id body)))

       (context "/versions/:version-id" []
                :path-params [version-id :- apps-schema/AppVersionIdParam]

                (PATCH "/" []
                       :body [body schema/AdminAppPatchRequest]
                       :return schema/AdminAppDetails
                       :summary schema/AdminAppVersionPatchSummary
                       :description-file "docs/apps/admin/app-label-update.md"
                       (ok (apps/admin-update-app-version system-id
                                                          app-id
                                                          version-id
                                                          body)))

                (GET "/details" []
                     :return schema/AdminAppDetails
                     :summary schema/AppVersionDetailsSummary
                     :description schema/AppVersionDetailsDocs
                     (ok (apps/get-admin-app-version-details system-id app-id version-id)))

                (PATCH "/documentation" []
                       :body [body apps-schema/AppDocumentationRequest]
                       :return apps-schema/AppDocumentation
                       :summary schema/AppVersionDocumentationUpdateSummary
                       :description schema/AppVersionDocumentationUpdateDocs
                       (ok (apps/admin-edit-app-version-docs system-id app-id version-id body)))

                (POST "/documentation" []
                      :body [body apps-schema/AppDocumentationRequest]
                      :return apps-schema/AppDocumentation
                      :summary schema/AppVersionDocumentationAddSummary
                      :description schema/AppVersionDocumentationAddDocs
                      (ok (apps/admin-add-app-version-docs system-id app-id version-id body)))

                (PUT "/integration-data/:integration-data-id" []
                     :path-params [integration-data-id :- IntegrationDataIdPathParam]
                     :return IntegrationData
                     :summary schema/AppVersionIntegrationDataUpdateSummary
                     :description schema/AppVersionIntegrationDataUpdateDocs
                     (ok (apps/update-app-version-integration-data system-id
                                                                   app-id
                                                                   version-id
                                                                   integration-data-id))))))))
