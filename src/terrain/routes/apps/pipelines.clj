(ns terrain.routes.apps.pipelines
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps :only [AppIdParam]]
        [common-swagger-api.schema.apps.pipeline]
        [ring.util.http-response :only [ok]]
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
            :body [body PipelineCreateRequest]
            :return Pipeline
            :summary PipelineCreateSummary
            :description PipelineCreateDocs
            (ok (apps/add-pipeline body)))

      (context "/:app-id" []
        :path-params [app-id :- AppIdParam]

        (PUT "/" []
             :body [body PipelineUpdateRequest]
             :return Pipeline
             :summary PipelineUpdateSummary
             :description PipelineUpdateDocs
             (ok (apps/update-pipeline app-id body)))

        (POST "/copy" []
              :return Pipeline
              :summary PipelineCopySummary
              :description PipelineCopyDocs
              (ok (apps/copy-pipeline app-id)))

        (GET "/ui" []
             :return Pipeline
             :summary PipelineEditingViewSummary
             :description PipelineEditingViewDocs
             (ok (apps/edit-pipeline app-id)))))))
