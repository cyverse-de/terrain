(ns terrain.routes.apps.admin.tools
  (:require [common-swagger-api.routes]                     ;; for :description-file
            [common-swagger-api.schema :refer [context GET POST DELETE PATCH PUT]]
            [common-swagger-api.schema.apps.admin.apps :refer [ToolAdminAppListingResponses]]
            [common-swagger-api.schema.integration-data
             :refer [IntegrationData
                     IntegrationDataIdPathParam]]
            [common-swagger-api.schema.tools :as schema]
            [common-swagger-api.schema.tools.admin :as admin-schema]
            [compojure.api.middleware :as middleware]
            [ring.util.http-response :refer [ok]]
            [terrain.clients.apps.raw :as apps]
            [terrain.services.metadata.apps :as apps-services]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to avoid lint warnings for path and query parameter bindings.
(declare params body tool-id integration-data-id status-code-id request-id)

(defn admin-tool-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/app-routes-enabled))]

    (context "/tools" []
      :tags ["admin-tools"]

      (GET "/" []
           :query [params schema/ToolSearchParams]
           :return schema/ToolListing
           :summary schema/ToolListingSummary
           :description admin-schema/ToolListingDocs
           (ok (apps/admin-list-tools params)))

      (POST "/" []
            :middleware [schema/coerce-tool-list-import-request]
            :body [body admin-schema/ToolsImportRequest]
            :responses admin-schema/ToolsImportResponses
            :summary admin-schema/ToolsImportSummary
            :description-file "docs/tools/admin/tools-import.md"
            (ok (apps-services/import-tools body)))

      (context "/:tool-id" []
        :path-params [tool-id :- schema/ToolIdParam]

        (DELETE "/" []
                :coercion middleware/no-response-coercion
                :responses admin-schema/ToolDeleteResponses
                :summary admin-schema/ToolDeleteSummary
                :description admin-schema/ToolDeleteDocs
                (ok (apps/admin-delete-tool tool-id)))

        (GET "/" []
             :query [params schema/ToolDetailsParams]
             :responses admin-schema/ToolDetailsResponses
             :summary schema/ToolDetailsSummary
             :description admin-schema/ToolDetailsDocs
             (ok (apps/admin-get-tool tool-id params)))

        (PATCH "/" []
               :query [params admin-schema/ToolUpdateParams]
               :middleware [schema/coerce-tool-import-requests]
               :body [body admin-schema/ToolUpdateRequest]
               :responses admin-schema/ToolUpdateResponses
               :summary admin-schema/ToolUpdateSummary
               :description-file "docs/tools/admin/tool-update.md"
               (ok (apps/admin-update-tool tool-id params body)))

        (GET "/apps" []
             :responses ToolAdminAppListingResponses
             :summary schema/ToolAppListingSummary
             :description schema/ToolAppListingDocs
             (ok (apps/admin-get-apps-by-tool tool-id)))

        (PUT "/integration-data/:integration-data-id" []
             :path-params [integration-data-id :- IntegrationDataIdPathParam]
             :return IntegrationData
             :summary admin-schema/ToolIntegrationUpdateSummary
             :description admin-schema/ToolIntegrationUpdateDocs
             (ok (apps/update-tool-integration-data tool-id integration-data-id)))

        (POST "/publish" []
              :middleware [schema/coerce-tool-import-requests]
              :body [body admin-schema/ToolUpdateRequest]
              :responses admin-schema/ToolPublishResponses
              :summary admin-schema/ToolPublishSummary
              :description admin-schema/ToolPublishDocs
              (ok (apps/admin-publish-tool tool-id body)))))))

(defn admin-tool-request-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/app-routes-enabled))]

    (context "/tool-requests" []
      :tags ["admin-tool-requests"]

      (GET "/" []
           :query [params schema/ToolRequestListingParams]
           :return schema/ToolRequestListing
           :summary schema/ToolInstallRequestListingSummary
           :description admin-schema/ToolInstallRequestListingDocs
           (ok (apps/admin-list-tool-requests params)))

      (DELETE "/status-codes/:status-code-id" []
              :path-params [status-code-id :- schema/ToolRequestStatusCodeId]
              :summary admin-schema/ToolInstallRequestStatusCodeDeleteSummary
              :description admin-schema/ToolInstallRequestStatusCodeDeleteDocs
              (ok (apps/admin-delete-tool-request-status-code status-code-id)))

      (context "/:request-id" []
        :path-params [request-id :- schema/ToolRequestIdParam]

        (DELETE "/" []
                :summary admin-schema/ToolInstallRequestDeleteSummary
                :description admin-schema/ToolInstallRequestDeleteDocs
                (ok (apps/admin-delete-tool-request request-id)))

        (GET "/" []
             :return schema/ToolRequestDetails
             :summary admin-schema/ToolInstallRequestDetailsSummary
             :description admin-schema/ToolInstallRequestDetailsDocs
             (ok (apps/admin-get-tool-request request-id)))

        (POST "/status" []
              :body [body admin-schema/ToolRequestStatusUpdate]
              :return schema/ToolRequestDetails
              :summary admin-schema/ToolInstallRequestStatusUpdateSummary
              :description admin-schema/ToolInstallRequestStatusUpdateDocs
              (ok (apps/admin-update-tool-request body request-id)))))))
