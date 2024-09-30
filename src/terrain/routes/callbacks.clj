(ns terrain.routes.callbacks
  (:require [common-swagger-api.schema :refer [context describe POST]]
            [common-swagger-api.schema.analyses :refer [AnalysisIdPathParam]]
            [common-swagger-api.schema.callbacks :refer [AgaveJobStatusUpdateParams AgaveJobStatusUpdate]]
            [ring.util.http-response :refer [ok]]
            [terrain.clients.apps.raw :as apps]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare job-id body params)

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
