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
        [terrain.routes.schemas.apps]
        [terrain.services.metadata.apps]
        [terrain.services.bootstrap]
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

(defn app-community-routes
  []
  (optional-routes
    [#(and (config/app-routes-enabled)
           (config/metadata-routes-enabled))]

    (GET "/apps/communities/:community-id/apps" [community-id]
      (service/success-response (apps/apps-in-community community-id)))

    (DELETE "/apps/:app-id/communities" [app-id :as {:keys [body]}]
      (service/success-response (apps/remove-app-from-communities app-id body)))

    (POST "/apps/:app-id/communities" [app-id :as {:keys [body]}]
      (service/success-response (apps/update-app-communities app-id body)))))

(defn admin-app-community-routes
  []
  (optional-routes
    [#(and (config/app-routes-enabled)
           (config/metadata-routes-enabled))]

    (GET "/apps/communities/:community-id/apps" [community-id]
      (service/success-response (apps/admin-get-apps-in-community community-id)))))

(defn admin-apps-routes
  []
  (optional-routes
   [#(and (config/admin-routes-enabled)
          (config/app-routes-enabled))]

   (GET "/apps" [:as {:keys [params]}]
     (service/success-response (apps/admin-get-apps params)))

   (POST "/apps" [:as {:keys [body]}]
     (service/success-response (apps/categorize-apps body)))

   (POST "/apps/shredder" [:as {:keys [body]}]
     (service/success-response (apps/permanently-delete-apps body)))

   (DELETE "/apps/:system-id/:app-id" [system-id app-id]
     (service/success-response (apps/admin-delete-app system-id app-id)))

   (PATCH "/apps/:system-id/:app-id" [system-id app-id :as {:keys [body]}]
     (service/success-response (apps/admin-update-app system-id app-id body)))

   (GET "/apps/:system-id/:app-id/details" [system-id app-id]
     (service/success-response (apps/get-admin-app-details system-id app-id)))

   (POST "/apps/:system-id/:app-id/documentation" [system-id app-id :as {:keys [body]}]
     (service/success-response (apps/admin-add-app-docs system-id app-id body)))

   (PATCH "/apps/:system-id/:app-id/documentation" [system-id app-id :as {:keys [body]}]
     (service/success-response (apps/admin-edit-app-docs system-id app-id body)))

   (PUT "/apps/:system-id/:app-id/integration-data/:integration-data-id" [system-id app-id integration-data-id]
     (service/success-response (apps/update-app-integration-data system-id app-id integration-data-id)))))

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
            :body [body schema/AppDeletionRequest]
            :summary schema/AppsShredderSummary
            :description schema/AppsShredderDocs
            (ok (apps/delete-apps body)))

      (POST "/permission-lister" []
            :query [params PermissionListerQueryParams]
            :body [body AppPermissionListingRequest]
            :return AppPermissionListing
            :summary AppPermissionListingSummary
            :description AppPermissionListingDocs
            (ok (apps/list-permissions body params)))

      (POST "/sharing" []
            :body [body AppSharingRequest]
            :return AppSharingResponse
            :summary AppSharingSummary
            :description AppSharingDocs
            (ok (apps/share body)))

      (POST "/unsharing" []
            :body [body AppUnsharingRequest]
            :return AppUnsharingResponse
            :summary AppUnsharingSummary
            :description AppUnsharingDocs
            (ok (apps/unshare body)))

      (context "/:system-id" []
        :path-params [system-id :- schema/SystemId]

        (POST "/" []
              :body [body schema/AppCreateRequest]
              :return schema/App
              :summary schema/AppCreateSummary
              :description schema/AppCreateDocs
              (ok (apps/create-app system-id body)))

        (POST "/arg-preview" []
              :body [body schema/AppPreviewRequest]
              :summary schema/AppPreviewSummary
              :description schema/AppPreviewDocs
              (ok (apps/preview-args system-id body)))

        (context "/:app-id" []
          :path-params [app-id :- schema/StringAppIdParam]

          (GET "/" []
               :summary schema/AppJobViewSummary
               :return schema/AppJobView
               :description schema/AppJobViewDocs
               (ok (apps/get-app system-id app-id)))

          (DELETE "/" []
                  :summary schema/AppDeleteSummary
                  :description schema/AppDeleteDocs
                  (ok (apps/delete-app system-id app-id)))

          (PATCH "/" []
                 :body [body schema/AppLabelUpdateRequest]
                 :return schema/App
                 :summary schema/AppLabelUpdateSummary
                 :description-file "docs/apps/app-label-update.md"
                 (ok (apps/relabel-app system-id app-id body)))

          (PUT "/" []
               :body [body schema/AppUpdateRequest]
               :return schema/App
               :summary schema/AppUpdateSummary
               :description schema/AppUpdateDocs
               (ok (apps/update-app system-id app-id body)))

          (POST "/copy" []
                :return schema/App
                :summary schema/AppCopySummary
                :description schema/AppCopyDocs
                (ok (apps/copy-app system-id app-id)))

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
                 :body [body schema/AppDocumentationRequest]
                 :return schema/AppDocumentation
                 :summary schema/AppDocumentationUpdateSummary
                 :description schema/AppDocumentationUpdateDocs
                 (ok (apps/edit-app-docs system-id app-id body)))

          (POST "/documentation" []
                :body [body schema/AppDocumentationRequest]
                :return schema/AppDocumentation
                :summary schema/AppDocumentationAddSummary
                :description schema/AppDocumentationAddDocs
                (ok (apps/add-app-docs system-id app-id body)))

          (DELETE "/favorite" []
                  :summary schema/AppFavoriteDeleteSummary
                  :description schema/AppFavoriteDeleteDocs
                  (ok (apps/remove-favorite-app system-id app-id)))

          (PUT "/favorite" []
               :summary schema/AppFavoriteAddSummary
               :description schema/AppFavoriteAddDocs
               (ok (apps/add-favorite-app system-id app-id)))

          (GET "/integration-data" []
               :return IntegrationData
               :summary schema/AppIntegrationDataSummary
               :description schema/AppIntegrationDataDocs
               (ok (apps/get-app-integration-data system-id app-id)))

          (GET "/is-publishable" []
               :return schema/AppPublishableResponse
               :summary schema/AppPublishableSummary
               :description schema/AppPublishableDocs
               (ok (apps/app-publishable? system-id app-id)))

          (POST "/publish" []
                :body [body schema/PublishAppRequest]
                :summary schema/PublishAppSummary
                :description schema/PublishAppDocs
                (ok (apps/make-app-public system-id app-id body)))

          (DELETE "/rating" []
                  :return RatingResponse
                  :summary schema/AppRatingDeleteSummary
                  :description schema/AppRatingDeleteDocs
                  (ok (apps/delete-rating system-id app-id)))

          (POST "/rating" []
                :body [body RatingRequest]
                :return RatingResponse
                :summary schema/AppRatingSummary
                :description schema/AppRatingDocs
                (ok (apps/rate-app system-id app-id body)))

          (GET "/tasks" []
               :return schema/AppTaskListing
               :summary schema/AppTaskListingSummary
               :description schema/AppTaskListingDocs
               (ok (apps/list-app-tasks system-id app-id)))

          (GET "/tools" []
               :return schema/AppToolListing
               :summary schema/AppToolListingSummary
               :description schema/AppToolListingDocs
               (ok (apps/get-tools-in-app system-id app-id)))

          (GET "/ui" []
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

(defn app-avu-routes
  []
  (optional-routes
   [#(and (config/app-routes-enabled)
          (config/metadata-routes-enabled))]

   (GET "/apps/:app-id/metadata" [app-id]
     (service/success-response (apps/list-avus app-id)))

   (POST "/apps/:app-id/metadata" [app-id :as {:keys [body]}]
     (service/success-response (apps/update-avus app-id body)))

   (PUT "/apps/:app-id/metadata" [app-id :as {:keys [body]}]
     (service/success-response (apps/set-avus app-id body)))))

(defn analysis-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/analyses" [:as {:keys [params]}]
     (service/success-response (apps/list-jobs params)))

   (POST "/analyses" [:as {:keys [body]}]
     (service/success-response (apps/submit-job body)))

   (POST "/analyses/permission-lister" [:as {:keys [body params]}]
     (service/success-response (apps/list-job-permissions body params)))

   (POST "/analyses/sharing" [:as {:keys [body]}]
     (service/success-response (apps/share-jobs body)))

   (POST "/analyses/unsharing" [:as {:keys [body]}]
     (service/success-response (apps/unshare-jobs body)))

   (PATCH "/analyses/:analysis-id" [analysis-id :as {body :body}]
          (service/success-response (apps/update-job analysis-id body)))

   (DELETE "/analyses/:analysis-id" [analysis-id]
     (service/success-response (apps/delete-job analysis-id)))

   (POST "/analyses/shredder" [:as {:keys [body]}]
     (service/success-response (apps/delete-jobs body)))

   (GET "/analyses/:analysis-id/history" [analysis-id]
     (service/success-response (apps/get-job-history analysis-id)))

   (GET "/analyses/:analysis-id/parameters" [analysis-id]
     (service/success-response (apps/get-job-params analysis-id)))

   (GET "/analyses/:analysis-id/relaunch-info" [analysis-id]
     (service/success-response (apps/get-job-relaunch-info analysis-id)))

   (GET "/analyses/:analysis-id/steps" [analysis-id]
     (service/success-response (apps/list-job-steps analysis-id)))

   (POST "/analyses/:analysis-id/stop" [analysis-id :as {:keys [params]}]
     (service/success-response (apps/stop-job analysis-id params)))))

(defn admin-reference-genomes-routes
  []
  (optional-routes
   [#(and (config/admin-routes-enabled)
          (config/app-routes-enabled))]

   (POST "/reference-genomes" [:as req]
     (add-reference-genome req))

   (DELETE "/reference-genomes/:reference-genome-id" [reference-genome-id :as {:keys [params]}]
     (apps/admin-delete-reference-genome reference-genome-id params))

   (PATCH "/reference-genomes/:reference-genome-id" [reference-genome-id :as req]
          (update-reference-genome req reference-genome-id))))

(defn reference-genomes-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/reference-genomes" [:as {params :params}]
     (service/success-response (apps/list-reference-genomes params)))

   (GET "/reference-genomes/:reference-genome-id" [reference-genome-id]
     (service/success-response (apps/get-reference-genome reference-genome-id)))))

(defn admin-tool-routes
  []
  (optional-routes
   [#(and (config/admin-routes-enabled)
          (config/app-routes-enabled))]

   (GET "/tools" [:as {:keys [params]}]
     (service/success-response (apps/admin-list-tools params)))

   (POST "/tools" [:as {:keys [body]}]
     (import-tools body))

   (DELETE "/tools/:tool-id" [tool-id]
     (apps/admin-delete-tool tool-id))

   (GET "/tools/:tool-id" [tool-id]
     (service/success-response (apps/admin-get-tool tool-id)))

   (PATCH "/tools/:tool-id" [tool-id :as {:keys [params body]}]
     (apps/admin-update-tool tool-id params body))

   (GET "/tools/:tool-id/apps" [tool-id]
     (service/success-response (apps/admin-get-apps-by-tool tool-id)))

   (PUT "/tools/:tool-id/integration-data/:integration-data-id" [tool-id integration-data-id]
     (service/success-response (apps/update-tool-integration-data tool-id integration-data-id)))

   (POST "/tools/:tool-id/publish" [tool-id :as {:keys [body]}]
     (apps/admin-publish-tool tool-id body))

   (GET "/tool-requests" [:as {params :params}]
     (admin-list-tool-requests params))

   (DELETE "/tool-requests/status-codes/:status-code-id" [status-code-id]
     (apps/admin-delete-tool-request-status-code status-code-id))

   (GET "/tool-requests/:request-id" [request-id]
     (get-tool-request request-id))

   (DELETE "/tool-requests/:request-id" [request-id]
     (apps/admin-delete-tool-request request-id))

   (POST "/tool-requests/:request-id/status" [request-id :as req]
     (update-tool-request req request-id))))

(defn tool-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/tools" [:as {:keys [params]}]
     (service/success-response (apps/list-tools params)))

   (POST "/tools" [:as {:keys [body]}]
     (service/success-response (apps/create-private-tool body)))

   (POST "/tools/permission-lister" [:as {:keys [body params]}]
     (service/success-response (apps/list-tool-permissions body params)))

   (POST "/tools/sharing" [:as {:keys [body]}]
     (service/success-response (apps/share-tool body)))

   (POST "/tools/unsharing" [:as {:keys [body]}]
     (service/success-response (apps/unshare-tool body)))

   (DELETE "/tools/:tool-id" [tool-id :as {:keys [params]}]
     (apps/delete-private-tool tool-id params))

   (GET "/tools/:tool-id" [tool-id]
     (service/success-response (apps/get-tool tool-id)))

   (PATCH "/tools/:tool-id" [tool-id :as {:keys [body]}]
     (apps/update-private-tool tool-id body))

   (GET "/tools/:tool-id/apps" [tool-id]
     (service/success-response (apps/get-apps-by-tool tool-id)))

   (GET "/tools/:tool-id/integration-data" [tool-id]
     (service/success-response (apps/get-tool-integration-data tool-id)))

   (GET "/tool-requests" []
     (list-tool-requests))

   (POST "/tool-requests" [:as req]
     (submit-tool-request req))

   (GET "/tool-requests/status-codes" [:as {params :params}]
     (list-tool-request-status-codes params))))

(defn misc-metadata-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (POST "/support-email" [:as {body :body}]
     (send-support-email body))))

(defn secured-metadata-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/bootstrap" [:as req]
     (bootstrap req))

   (GET "/logout" [:as {params :params}]
     (logout params))))

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
