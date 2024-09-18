(ns terrain.routes.callbacks
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.analyses :only [AnalysisIdPathParam]]
        [common-swagger-api.schema.callbacks :only [TapisJobStatusUpdate]]
        [ring.util.http-response :only [ok]]
        [terrain.util])
  (:require [terrain.clients.apps.raw :as apps]
            [terrain.util.config :as config]))

(defn callback-routes
  "Routes for making calls back into the DE web services."
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/callbacks" []
             :tags ["callbacks"]

             (POST "/tapis-job/:job-id" []
                   :path-params [job-id :- AnalysisIdPathParam]
                   :body [body (describe TapisJobStatusUpdate "The updated job status information.")]
                   :summary "Update the status of a Tapis analysis."
                   :description "The DE registers this endpoint as a callback when it submits jobs to Tapis."
                   (ok (apps/update-tapis-job-status job-id body))))))
