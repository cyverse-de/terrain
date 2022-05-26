(ns terrain.routes.callbacks
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.analyses :only [AnalysisIdPathParam]]
        [common-swagger-api.schema.callbacks :only [AgaveJobStatusUpdateParams AgaveJobStatusUpdate]]
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

     (POST "/agave-job/:job-id" []
       :path-params [job-id :- AnalysisIdPathParam]
       :body [body (describe AgaveJobStatusUpdate "The updated job status information.")]
       :query [params AgaveJobStatusUpdateParams]
       :summary "Update the status of an Agave analysis."
       :description "The DE registers this endpoint as a callback when it submts jobs to Agave."
       (ok (apps/update-agave-job-status job-id body params))))))
