(ns terrain.routes.apps.tools
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps :only [AppListing ToolAppListingResponses]]
        [common-swagger-api.schema.integration-data :only [IntegrationData]]
        [ring.util.http-response :only [ok]]
        [terrain.util])
  (:require [common-swagger-api.routes]                     ;; for :description-file
            [common-swagger-api.schema.apps.permission :as perm-schema]
            [common-swagger-api.schema.tools :as schema]
            [compojure.api.middleware :as middleware]
            [terrain.clients.apps.raw :as apps]
            [terrain.util.config :as config]))

(defn tool-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/tools" []
      :tags ["tools"]

      (GET "/" []
           :query [params schema/ToolSearchParams]
           :return schema/ToolListing
           :summary schema/ToolListingSummary
           :description schema/ToolListingDocs
           (ok (apps/list-tools params)))

      (POST "/" []
            :middleware [schema/coerce-tool-import-requests]
            :body [body schema/PrivateToolImportRequest]
            :responses schema/PrivateToolImportResponses
            :summary schema/ToolAddSummary
            :description-file "docs/tools/tool-add.md"
            (ok (apps/create-private-tool body)))

      (POST "/permission-lister" []
            :query [params perm-schema/PermissionListerQueryParams]
            :body [body perm-schema/ToolIdList]
            :responses perm-schema/ToolPermissionsListingResponses
            :summary schema/ToolPermissionsListingSummary
            :description schema/ToolPermissionsListingDocs
            (ok (apps/list-tool-permissions body params)))

      (POST "/sharing" []
            :body [body perm-schema/ToolSharingRequest]
            :return perm-schema/ToolSharingResponse
            :summary perm-schema/ToolSharingSummary
            :description perm-schema/ToolSharingDocs
            (ok (apps/share-tool body)))

      (POST "/unsharing" []
            :body [body perm-schema/ToolUnsharingRequest]
            :return perm-schema/ToolUnsharingResponse
            :summary perm-schema/ToolUnsharingSummary
            :description perm-schema/ToolUnsharingDocs
            (ok (apps/unshare-tool body)))

      (context "/:tool-id" []
        :path-params [tool-id :- schema/ToolIdParam]

        (DELETE "/" []
                :query [params schema/PrivateToolDeleteParams]
                :coercion middleware/no-response-coercion
                :responses schema/ToolDeleteResponses
                :summary schema/ToolDeleteSummary
                :description schema/ToolDeleteDocs
                (ok (apps/delete-private-tool tool-id params)))

        (GET "/" []
             :responses schema/ToolDetailsResponses
             :summary schema/ToolDetailsSummary
             :description schema/ToolDetailsDocs
             (ok (apps/get-tool tool-id)))

        (PATCH "/" []
               :middleware [schema/coerce-tool-import-requests]
               :body [body schema/PrivateToolUpdateRequest]
               :responses schema/ToolUpdateResponses
               :summary schema/ToolUpdateSummary
               :description-file "docs/tools/tool-update.md"
               (ok (apps/update-private-tool tool-id body)))

        (GET "/apps" []
             :responses ToolAppListingResponses
             :summary schema/ToolAppListingSummary
             :description schema/ToolAppListingDocs
             (ok (apps/get-apps-by-tool tool-id)))

        (GET "/integration-data" []
             :return IntegrationData
             :summary schema/ToolIntegrationDataListingSummary
             :description schema/ToolIntegrationDataListingDocs
             (ok (apps/get-tool-integration-data tool-id)))))))
