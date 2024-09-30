(ns terrain.routes.apps.pipelines
  (:require [common-swagger-api.schema :refer [context POST PUT GET]]
            [common-swagger-api.schema.apps :refer [AppIdParam AppVersionIdParam]]
            [common-swagger-api.schema.apps.pipeline :as pipeline-schema]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [require-authentication]]
            [terrain.clients.apps.raw :as apps]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare body app-id version-id)

(defn app-pipeline-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/apps/pipelines" []
      :tags ["app-pipelines"]

      (POST "/" []
            :middleware [require-authentication]
            :body [body pipeline-schema/PipelineCreateRequest]
            :return pipeline-schema/Pipeline
            :summary pipeline-schema/PipelineCreateSummary
            :description pipeline-schema/PipelineCreateDocs
            (ok (apps/add-pipeline body)))

      (context "/:app-id" []
        :path-params [app-id :- AppIdParam]

        (PUT "/" []
             :middleware [require-authentication]
             :body [body pipeline-schema/PipelineUpdateRequest]
             :return pipeline-schema/Pipeline
             :summary pipeline-schema/PipelineUpdateSummary
             :description pipeline-schema/PipelineUpdateDocs
             (ok (apps/update-pipeline app-id body)))

        (POST "/copy" []
              :middleware [require-authentication]
              :return pipeline-schema/Pipeline
              :summary pipeline-schema/PipelineCopySummary
              :description pipeline-schema/PipelineCopyDocs
              (ok (apps/copy-pipeline app-id)))

        (GET "/ui" []
             :middleware [require-authentication]
             :return pipeline-schema/Pipeline
             :summary pipeline-schema/PipelineEditingViewSummary
             :description pipeline-schema/PipelineEditingViewDocs
             (ok (apps/edit-pipeline app-id)))

        (context "/versions" []

                 (POST "/" []
                       :body [body pipeline-schema/PipelineVersionRequest]
                       :return pipeline-schema/Pipeline
                       :summary pipeline-schema/PipelineVersionCreateSummary
                       :description pipeline-schema/PipelineVersionCreateDocs
                       (ok (apps/add-pipeline-version app-id body)))

                 (context "/:version-id" []
                          :path-params [version-id :- AppVersionIdParam]

                          (PUT "/" []
                               :body [body pipeline-schema/PipelineUpdateRequest]
                               :return pipeline-schema/Pipeline
                               :summary pipeline-schema/PipelineVersionUpdateSummary
                               :description pipeline-schema/PipelineVersionUpdateDocs
                               (ok (apps/update-pipeline-version app-id version-id body)))

                          (POST "/copy" []
                                :return pipeline-schema/Pipeline
                                :summary pipeline-schema/PipelineVersionCopySummary
                                :description pipeline-schema/PipelineVersionCopyDocs
                                (ok (apps/copy-pipeline-version app-id version-id)))

                          (GET "/ui" []
                               :return pipeline-schema/Pipeline
                               :summary pipeline-schema/PipelineVersionEditingViewSummary
                               :description pipeline-schema/PipelineVersionEditingViewDocs
                               (ok (apps/edit-pipeline-version app-id version-id)))))))))
