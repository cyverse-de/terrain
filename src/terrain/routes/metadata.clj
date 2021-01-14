(ns terrain.routes.metadata
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps.permission
         :only [AppPermissionListing
                AppPermissionListingDocs
                AppPermissionListingRequest
                AppPermissionListingSummary
                AppSharingDocs
                AppSharingRequest
                AppSharingResponse
                AppSharingSummary
                AppUnsharingDocs
                AppUnsharingRequest
                AppUnsharingResponse
                AppUnsharingSummary
                PermissionListerQueryParams]]
        [common-swagger-api.schema.apps.rating]
        [common-swagger-api.schema.integration-data :only [IntegrationData]]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [require-authentication]]
        [terrain.routes.schemas.apps]
        [terrain.services.metadata.apps :only [send-support-email]]
        [terrain.util])
  (:require [common-swagger-api.routes]                     ;; Required for :description-file
            [common-swagger-api.schema.apps :as schema]
            [terrain.clients.apps.raw :as apps]
            [terrain.clients.metadata :as metadata]
            [terrain.clients.metadata.raw :as metadata-client]
            [terrain.util.config :as config]
            [terrain.util.service :as service]))

(defn admin-category-routes
  []
  (optional-routes
   [#(and (config/admin-routes-enabled)
          (config/app-routes-enabled))]

   (GET "/apps/categories" [:as {params :params}]
     (service/success-response (apps/get-admin-app-categories params)))

   (GET "/apps/categories/search" [:as {params :params}]
     (service/success-response (apps/search-admin-app-categories params)))

   (POST "/apps/categories/:system-id" [system-id :as {:keys [body]}]
     (service/success-response (apps/add-category system-id body)))

   (DELETE "/apps/categories/:system-id/:category-id" [system-id category-id]
     (service/success-response (apps/delete-category system-id category-id)))

   (PATCH "/apps/categories/:system-id/:category-id" [system-id category-id :as {:keys [body]}]
     (service/success-response (apps/update-category system-id category-id body)))))

(defn admin-ontology-routes
  []
  (optional-routes
   [#(and (config/admin-routes-enabled)
          (config/app-routes-enabled)
          (config/metadata-routes-enabled))]

   (GET "/ontologies" []
     (service/success-response (apps/list-ontologies)))

   (POST "/ontologies" [:as request]
     (service/success-response (metadata/upload-ontology request)))

   (DELETE "/ontologies/:ontology-version" [ontology-version]
     (service/success-response (apps/delete-ontology ontology-version)))

   (GET "/ontologies/:ontology-version" [ontology-version]
     (service/success-response (metadata-client/get-ontology-hierarchies ontology-version)))

   (POST "/ontologies/:ontology-version" [ontology-version]
     (service/success-response (apps/set-ontology-version ontology-version)))

   (DELETE "/ontologies/:ontology-version/:root-iri" [ontology-version root-iri]
     (service/success-response (metadata-client/delete-app-category-hierarchy ontology-version root-iri)))

   (GET "/ontologies/:ontology-version/:root-iri" [ontology-version root-iri :as {params :params}]
     (service/success-response (apps/get-app-category-hierarchy ontology-version root-iri params)))

   (PUT "/ontologies/:ontology-version/:root-iri" [ontology-version root-iri]
     (service/success-response (metadata-client/save-ontology-hierarchy ontology-version root-iri)))

   (GET "/ontologies/:ontology-version/:root-iri/apps" [ontology-version root-iri :as {params :params}]
     (service/success-response (apps/get-hierarchy-app-listing ontology-version root-iri params)))

   (GET "/ontologies/:ontology-version/:root-iri/unclassified" [ontology-version root-iri :as {params :params}]
     (service/success-response (apps/get-unclassified-app-listing ontology-version root-iri params)))))

(defn admin-app-community-routes
  []
  (optional-routes
    [#(and (config/app-routes-enabled)
           (config/metadata-routes-enabled))]

    (GET "/apps/communities/:community-id/apps" [community-id]
      (service/success-response (apps/admin-get-apps-in-community community-id)))))

(defn apps-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/apps" []
      :tags ["apps"]

      (GET "/" []
           :query [params AppSearchParams]
           :return schema/AppListing
           :summary schema/AppListingSummary
           :description schema/AppListingDocs
           (ok (apps/search-apps params)))

      (POST "/shredder" []
            :middleware [require-authentication]
            :body [body schema/AppDeletionRequest]
            :summary schema/AppsShredderSummary
            :description schema/AppsShredderDocs
            (ok (apps/delete-apps body)))

      (POST "/permission-lister" []
            :middleware [require-authentication]
            :query [params PermissionListerQueryParams]
            :body [body AppPermissionListingRequest]
            :return AppPermissionListing
            :summary AppPermissionListingSummary
            :description AppPermissionListingDocs
            (ok (apps/list-permissions body params)))

      (POST "/sharing" []
            :middleware [require-authentication]
            :body [body AppSharingRequest]
            :return AppSharingResponse
            :summary AppSharingSummary
            :description AppSharingDocs
            (ok (apps/share body)))

      (POST "/unsharing" []
            :middleware [require-authentication]
            :body [body AppUnsharingRequest]
            :return AppUnsharingResponse
            :summary AppUnsharingSummary
            :description AppUnsharingDocs
            (ok (apps/unshare body)))

      (context "/:system-id" []
        :path-params [system-id :- schema/SystemId]

        (POST "/" []
              :middleware [require-authentication]
              :body [body schema/AppCreateRequest]
              :return schema/App
              :summary schema/AppCreateSummary
              :description schema/AppCreateDocs
              (ok (apps/create-app system-id body)))

        (POST "/arg-preview" []
              :middleware [require-authentication]
              :body [body schema/AppPreviewRequest]
              :summary schema/AppPreviewSummary
              :description schema/AppPreviewDocs
              (ok (apps/preview-args system-id body)))

        (context "/:app-id" []
          :path-params [app-id :- schema/StringAppIdParam]

          (GET "/" []
               :middleware [require-authentication]
               :summary schema/AppJobViewSummary
               :return schema/AppJobView
               :description schema/AppJobViewDocs
               (ok (apps/get-app system-id app-id)))

          (DELETE "/" []
                  :middleware [require-authentication]
                  :summary schema/AppDeleteSummary
                  :description schema/AppDeleteDocs
                  (ok (apps/delete-app system-id app-id)))

          (PATCH "/" []
                 :middleware [require-authentication]
                 :body [body schema/AppLabelUpdateRequest]
                 :return schema/App
                 :summary schema/AppLabelUpdateSummary
                 :description-file "docs/apps/app-label-update.md"
                 (ok (apps/relabel-app system-id app-id body)))

          (PUT "/" []
               :middleware [require-authentication]
               :body [body schema/AppUpdateRequest]
               :return schema/App
               :summary schema/AppUpdateSummary
               :description schema/AppUpdateDocs
               (ok (apps/update-app system-id app-id body)))

          (POST "/copy" []
                :middleware [require-authentication]
                :return schema/App
                :summary schema/AppCopySummary
                :description schema/AppCopyDocs
                (ok (apps/copy-app system-id app-id)))

          (GET "/listing" []
                :summary schema/SingleAppListingSummary
                :return schema/AppListing
                :description schema/SingleAppListingDocs
                (ok (apps/list-single-app system-id app-id)))

          (GET "/details" []
               :return schema/AppDetails
               :summary schema/AppDetailsSummary
               :description schema/AppDetailsDocs
               (ok (apps/get-app-details system-id app-id)))

          (GET "/documentation" []
               :return schema/AppDocumentation
               :summary schema/AppDocumentationSummary
               :description schema/AppDocumentationDocs
               (ok (apps/get-app-docs system-id app-id)))

          (PATCH "/documentation" []
                 :middleware [require-authentication]
                 :body [body schema/AppDocumentationRequest]
                 :return schema/AppDocumentation
                 :summary schema/AppDocumentationUpdateSummary
                 :description schema/AppDocumentationUpdateDocs
                 (ok (apps/edit-app-docs system-id app-id body)))

          (POST "/documentation" []
                :middleware [require-authentication]
                :body [body schema/AppDocumentationRequest]
                :return schema/AppDocumentation
                :summary schema/AppDocumentationAddSummary
                :description schema/AppDocumentationAddDocs
                (ok (apps/add-app-docs system-id app-id body)))

          (DELETE "/favorite" []
                  :middleware [require-authentication]
                  :summary schema/AppFavoriteDeleteSummary
                  :description schema/AppFavoriteDeleteDocs
                  (ok (apps/remove-favorite-app system-id app-id)))

          (PUT "/favorite" []
               :middleware [require-authentication]
               :summary schema/AppFavoriteAddSummary
               :description schema/AppFavoriteAddDocs
               (ok (apps/add-favorite-app system-id app-id)))

          (GET "/integration-data" []
               :middleware [require-authentication]
               :return IntegrationData
               :summary schema/AppIntegrationDataSummary
               :description schema/AppIntegrationDataDocs
               (ok (apps/get-app-integration-data system-id app-id)))

          (GET "/is-publishable" []
               :middleware [require-authentication]
               :return schema/AppPublishableResponse
               :summary schema/AppPublishableSummary
               :description schema/AppPublishableDocs
               (ok (apps/app-publishable? system-id app-id)))

          (POST "/publish" []
                :middleware [require-authentication]
                :body [body schema/PublishAppRequest]
                :summary schema/PublishAppSummary
                :description schema/PublishAppDocs
                (ok (apps/make-app-public system-id app-id body)))

          (DELETE "/rating" []
                  :middleware [require-authentication]
                  :return RatingResponse
                  :summary schema/AppRatingDeleteSummary
                  :description schema/AppRatingDeleteDocs
                  (ok (apps/delete-rating system-id app-id)))

          (POST "/rating" []
                :middleware [require-authentication]
                :body [body RatingRequest]
                :return RatingResponse
                :summary schema/AppRatingSummary
                :description schema/AppRatingDocs
                (ok (apps/rate-app system-id app-id body)))

          (GET "/tasks" []
               :middleware [require-authentication]
               :return schema/AppTaskListing
               :summary schema/AppTaskListingSummary
               :description schema/AppTaskListingDocs
               (ok (apps/list-app-tasks system-id app-id)))

          (GET "/tools" []
               :middleware [require-authentication]
               :return schema/AppToolListing
               :summary schema/AppToolListingSummary
               :description schema/AppToolListingDocs
               (ok (apps/get-tools-in-app system-id app-id)))

          (GET "/ui" []
               :middleware [require-authentication]
               :return schema/App
               :summary schema/AppEditingViewSummary
               :description schema/AppEditingViewDocs
               (ok (apps/get-app-ui system-id app-id))))))))

(defn admin-app-avu-routes
  []
  (optional-routes
   [#(and (config/admin-routes-enabled)
          (config/app-routes-enabled)
          (config/metadata-routes-enabled))]

   (GET "/apps/:app-id/metadata" [app-id]
     (service/success-response (apps/admin-list-avus app-id)))

   (POST "/apps/:app-id/metadata" [app-id :as {:keys [body]}]
     (service/success-response (apps/admin-update-avus app-id body)))

   (PUT "/apps/:app-id/metadata" [app-id :as {:keys [body]}]
     (service/success-response (apps/admin-set-avus app-id body)))))

(defn misc-metadata-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (POST "/support-email" [:as {body :body}]
     :tags ["support"]
     :body [body SupportEmailRequest]
     :summary SupportEmailSummary
     :description SupportEmailDescription
     (send-support-email body)
     (ok))))

(defn admin-integration-data-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/integration-data" [:as {:keys [params]}]
     (service/success-response (apps/list-integration-data params)))

   (POST "/integration-data" [:as {:keys [body]}]
     (service/success-response (apps/add-integration-data body)))

   (GET "/integration-data/:integration-data-id" [integration-data-id]
     (service/success-response (apps/get-integration-data integration-data-id)))

   (PUT "/integration-data/:integration-data-id" [integration-data-id :as {:keys [body]}]
     (service/success-response (apps/update-integration-data integration-data-id body)))

   (DELETE "/integration-data/:integration-data-id" [integration-data-id]
     (service/success-response (apps/delete-integration-data integration-data-id)))))

(defn admin-workspace-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/workspaces" [:as {:keys [params]}]
     (service/success-response (apps/admin-list-workspaces params)))

   (DELETE "/workspaces" [:as {:keys [params]}]
     (service/success-response (apps/admin-delete-workspaces params)))))
