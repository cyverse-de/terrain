(ns terrain.routes.metadata
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps
         :only [App
                AppCopyDocs
                AppCopySummary
                AppCreateDocs
                AppCreateRequest
                AppCreateSummary
                AppDeleteDocs
                AppDeleteSummary
                AppDeletionRequest
                AppDetails
                AppDetailsSummary
                AppDocumentation
                AppDocumentationDocs
                AppDocumentationSummary
                AppJobView
                AppJobViewDocs
                AppJobViewSummary
                AppLabelUpdateRequest
                AppLabelUpdateSummary
                AppListing
                AppListingSummary
                AppPreviewDocs
                AppPreviewRequest
                AppPreviewSummary
                AppsShredderDocs
                AppsShredderSummary
                AppUpdateRequest
                AppUpdateSummary
                StringAppIdParam
                SystemId]]
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
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.apps]
        [terrain.services.metadata.apps]
        [terrain.services.bootstrap]
        [terrain.util])
  (:require [common-swagger-api.routes]                     ;; Required for :description-file
            [terrain.clients.apps.raw :as apps]
            [terrain.clients.metadata :as metadata]
            [terrain.clients.metadata.raw :as metadata-client]
            [terrain.util.config :as config]
            [terrain.util.service :as service]))

(defn app-category-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/apps/categories" [:as {params :params}]
     (service/success-response (apps/get-app-categories params)))

   (GET "/apps/categories/:system-id/:category-id" [system-id category-id :as {params :params}]
     (service/success-response (apps/apps-in-category system-id category-id params)))))

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

(defn app-ontology-routes
  []
  (optional-routes
   [#(and (config/app-routes-enabled)
          (config/metadata-routes-enabled))]

   (GET "/apps/hierarchies" []
     (service/success-response (apps/get-app-category-hierarchies)))

   (GET "/apps/hierarchies/:root-iri" [root-iri :as {params :params}]
     (service/success-response (apps/get-app-category-hierarchy root-iri params)))

   (GET "/apps/hierarchies/:root-iri/apps" [root-iri :as {params :params}]
     (service/success-response (apps/get-hierarchy-app-listing root-iri params)))

   (GET "/apps/hierarchies/:root-iri/unclassified" [root-iri :as {params :params}]
     (service/success-response (apps/get-unclassified-app-listing root-iri params)))))

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
           :return AppListing
           :summary AppListingSummary
           :description-file "docs/apps/apps-listing.md"
           (ok (apps/search-apps params)))

      (POST "/pipelines" [:as {:keys [body]}]
            (service/success-response (apps/add-pipeline body)))

      (PUT "/pipelines/:app-id" [app-id :as {:keys [body]}]
           (service/success-response (apps/update-pipeline app-id body)))

      (POST "/pipelines/:app-id/copy" [app-id]
            (service/success-response (apps/copy-pipeline app-id)))

      (GET "/pipelines/:app-id/ui" [app-id]
           (service/success-response (apps/edit-pipeline app-id)))

      (POST "/shredder" []
            :body [body AppDeletionRequest]
            :summary AppsShredderSummary
            :description AppsShredderDocs
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
        :path-params [system-id :- SystemId]

        (POST "/" []
              :body [body AppCreateRequest]
              :return App
              :summary AppCreateSummary
              :description AppCreateDocs
              (ok (apps/create-app system-id body)))

        (POST "/arg-preview" []
              :body [body AppPreviewRequest]
              :summary AppPreviewSummary
              :description AppPreviewDocs
              (ok (apps/preview-args system-id body)))

        (context "/:app-id" []
          :path-params [app-id :- StringAppIdParam]

          (GET "/" []
               :summary AppJobViewSummary
               :return AppJobView
               :description AppJobViewDocs
               (ok (apps/get-app system-id app-id)))

          (DELETE "/" []
                  :summary AppDeleteSummary
                  :description AppDeleteDocs
                  (ok (apps/delete-app system-id app-id)))

          (PATCH "/" []
                 :body [body AppLabelUpdateRequest]
                 :return App
                 :summary AppLabelUpdateSummary
                 :description-file "docs/apps/app-label-update.md"
                 (ok (apps/relabel-app system-id app-id body)))

          (PUT "/" []
               :body [body AppUpdateRequest]
               :return App
               :summary AppUpdateSummary
               :description-file "docs/apps/app-update.md"
               (ok (apps/update-app system-id app-id body)))

          (POST "/copy" []
                :return App
                :summary AppCopySummary
                :description AppCopyDocs
                (ok (apps/copy-app system-id app-id)))

          (GET "/details" []
               :return AppDetails
               :summary AppDetailsSummary
               :description-file "docs/apps/app-details.md"
               (ok (apps/get-app-details system-id app-id)))

          (GET "/documentation" []
               :return AppDocumentation
               :summary AppDocumentationSummary
               :description AppDocumentationDocs
               (ok (apps/get-app-docs system-id app-id)))

          (POST "/documentation" [:as {:keys [body]}]
                (service/success-response (apps/add-app-docs system-id app-id body)))

          (PATCH "/documentation" [:as {:keys [body]}]
                 (service/success-response (apps/edit-app-docs system-id app-id body)))

          (DELETE "/favorite" []
                  (service/success-response (apps/remove-favorite-app system-id app-id)))

          (PUT "/favorite" []
               (service/success-response (apps/add-favorite-app system-id app-id)))

          (GET "/integration-data" []
               (service/success-response (apps/get-app-integration-data system-id app-id)))

          (GET "/is-publishable" []
               (service/success-response (apps/app-publishable? system-id app-id)))

          (POST "/publish" [:as {:keys [body]}]
                (service/success-response (apps/make-app-public system-id app-id body)))

          (DELETE "/rating" []
                  (service/success-response (apps/delete-rating system-id app-id)))

          (POST "/rating" [:as {body :body}]
                (service/success-response (apps/rate-app system-id app-id body)))

          (GET "/tasks" []
               (service/success-response (apps/list-app-tasks system-id app-id)))

          (GET "/tools" []
               (service/success-response (apps/get-tools-in-app system-id app-id)))

          (GET "/ui" []
               (service/success-response (apps/get-app-ui system-id app-id))))))))

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
