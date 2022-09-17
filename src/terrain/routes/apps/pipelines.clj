(ns terrain.routes.apps.pipelines
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps :only [AppIdParam AppVersionIdParam]]
        [common-swagger-api.schema.apps.pipeline]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [require-authentication]]
        [terrain.util :only [optional-routes]])
  (:require [terrain.clients.apps.raw :as apps]
            [terrain.util.config :as config]))

(defn app-pipeline-routes
  []
  (optional-routes
    [config/app-routes-enabled]
    
    (context "/apps/pipelines" []
      :tags ["app-pipelines"]

      (POST "/" []
            :middleware [require-authentication]
            :body [body PipelineCreateRequest]
            :return Pipeline
            :summary PipelineCreateSummary
            :description PipelineCreateDocs
            (ok (apps/add-pipeline body)))

      (context "/:app-id" []
        :path-params [app-id :- AppIdParam]

        (PUT "/" []
             :middleware [require-authentication]
             :body [body PipelineUpdateRequest]
             :return Pipeline
             :summary PipelineUpdateSummary
             :description PipelineUpdateDocs
             (ok (apps/update-pipeline app-id body)))

        (POST "/copy" []
              :middleware [require-authentication]
              :return Pipeline
              :summary PipelineCopySummary
              :description PipelineCopyDocs
              (ok (apps/copy-pipeline app-id)))

        (GET "/ui" []
             :middleware [require-authentication]
             :return Pipeline
             :summary PipelineEditingViewSummary
             :description PipelineEditingViewDocs
             (ok (apps/edit-pipeline app-id)))

        (context "/versions" []

                 (POST "/" []
                       :body [body PipelineVersionRequest]
                       :return Pipeline
                       :summary PipelineVersionCreateSummary
                       :description PipelineVersionCreateDocs
                       (ok (apps/add-pipeline-version app-id body)))

                 (context "/:version-id" []
                          :path-params [version-id :- AppVersionIdParam]

                          (PUT "/" []
                               :body [body PipelineUpdateRequest]
                               :return Pipeline
                               :summary PipelineVersionUpdateSummary
                               :description PipelineVersionUpdateDocs
                               (ok (apps/update-pipeline-version app-id version-id body)))

                          (POST "/copy" []
                                :return Pipeline
                                :summary PipelineVersionCopySummary
                                :description PipelineVersionCopyDocs
                                (ok (apps/copy-pipeline-version app-id version-id)))

                          (GET "/ui" []
                               :return Pipeline
                               :summary PipelineVersionEditingViewSummary
                               :description PipelineVersionEditingViewDocs
                               (ok (apps/edit-pipeline-version app-id version-id)))))))))
