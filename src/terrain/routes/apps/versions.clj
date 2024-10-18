(ns terrain.routes.apps.versions
  (:require [common-swagger-api.schema :refer [context POST DELETE GET PATCH PUT]]
            [common-swagger-api.schema.apps :as schema]
            [common-swagger-api.schema.integration-data :as integration-schema]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [require-authentication]]
            [terrain.clients.apps.raw :as apps]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare system-id app-id version-id body)

(defn app-version-routes
  []

  (optional-routes
    [config/app-routes-enabled]

    (context "/apps/:system-id/:app-id/versions" []
             :tags ["app-versions"]
             :path-params [system-id :- schema/SystemId
                           app-id    :- schema/AppIdParam]

             (POST "/" []
                   :middleware [require-authentication]
                   :body [body schema/AppVersionRequest]
                   :return schema/App
                   :summary schema/AppVersionCreateSummary
                   :description schema/AppVersionCreateDocs
                   (ok (apps/create-app-version system-id app-id body)))

             (PUT "/" []
                  :middleware [require-authentication]
                  :body [body schema/AppVersionOrderRequest]
                  :return schema/App
                  :summary schema/AppVersionOrderSummary
                  :description schema/AppVersionOrderDocs
                  (ok (apps/set-app-versions-order system-id app-id body)))

             (context "/:version-id" []
                      :path-params [version-id :- schema/AppVersionIdParam]

                      (DELETE "/" []
                              :middleware [require-authentication]
                              :summary schema/AppVersionDeleteSummary
                              :description schema/AppVersionDeleteDocs
                              (ok (apps/delete-app-version system-id app-id version-id)))

                      (GET "/" []
                           :middleware [require-authentication]
                           :return schema/AppJobView
                           :summary schema/AppJobViewSummary
                           :description schema/AppJobViewDocs
                           (ok (apps/get-app-version system-id app-id version-id)))

                      (PATCH "/" []
                             :middleware [require-authentication]
                             :body [body schema/AppLabelUpdateRequest]
                             :return schema/App
                             :summary schema/AppLabelUpdateSummary
                             :description-file "docs/apps/app-label-update.md"
                             (ok (apps/relabel-app-version system-id app-id version-id body)))

                      (PUT "/" []
                           :middleware [require-authentication]
                           :body [body schema/AppUpdateRequest]
                           :return schema/App
                           :summary schema/AppUpdateSummary
                           :description schema/AppUpdateDocs
                           (ok (apps/update-app-version system-id app-id version-id body)))

                      (POST "/copy" []
                            :middleware [require-authentication]
                            :return schema/App
                            :summary schema/AppCopySummary
                            :description schema/AppCopyDocs
                            (ok (apps/copy-app-version system-id app-id version-id)))

                      (GET "/details" []
                           :return schema/AppDetails
                           :summary schema/AppDetailsSummary
                           :description schema/AppDetailsDocs
                           (ok (apps/get-app-version-details system-id app-id version-id)))

                      (GET "/documentation" []
                           :return schema/AppDocumentation
                           :summary schema/AppDocumentationSummary
                           :description schema/AppDocumentationDocs
                           (ok (apps/get-app-version-docs system-id app-id version-id)))

                      (PATCH "/documentation" []
                             :middleware [require-authentication]
                             :body [body schema/AppDocumentationRequest]
                             :return schema/AppDocumentation
                             :summary schema/AppDocumentationUpdateSummary
                             :description schema/AppDocumentationUpdateDocs
                             (ok (apps/edit-app-version-docs system-id app-id version-id body)))

                      (POST "/documentation" []
                            :middleware [require-authentication]
                            :body [body schema/AppDocumentationRequest]
                            :return schema/AppDocumentation
                            :summary schema/AppDocumentationAddSummary
                            :description schema/AppDocumentationAddDocs
                            (ok (apps/add-app-version-docs system-id app-id version-id body)))

                      (GET "/integration-data" []
                           :middleware [require-authentication]
                           :return integration-schema/IntegrationData
                           :summary schema/AppIntegrationDataSummary
                           :description schema/AppIntegrationDataDocs
                           (ok (apps/get-app-version-integration-data system-id app-id version-id)))

                      (GET "/tasks" []
                           :middleware [require-authentication]
                           :return schema/AppTaskListing
                           :summary schema/AppTaskListingSummary
                           :description schema/AppTaskListingDocs
                           (ok (apps/list-app-version-tasks system-id app-id version-id)))

                      (GET "/tools" []
                           :middleware [require-authentication]
                           :return schema/AppToolListing
                           :summary schema/AppToolListingSummary
                           :description schema/AppToolListingDocs
                           (ok (apps/get-tools-in-app-version system-id app-id version-id)))

                      (GET "/ui" []
                           :middleware [require-authentication]
                           :return schema/App
                           :summary schema/AppEditingViewSummary
                           :description schema/AppEditingViewDocs
                           (ok (apps/get-app-version-ui system-id app-id version-id)))))))
