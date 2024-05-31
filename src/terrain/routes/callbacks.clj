(ns terrain.routes.callbacks
  (:require [common-swagger-api.schema :refer [context describe POST]]
            [common-swagger-api.schema.analyses :refer [AnalysisIdPathParam]]
            [common-swagger-api.schema.callbacks :refer [TapisJobStatusUpdate]]
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

             (POST "/tapis-job/:job-id" []
                   :path-params [job-id :- AnalysisIdPathParam]
                   :body [body (describe TapisJobStatusUpdate "The updated job status information.")]
                   :summary "Update the status of a Tapis analysis."
                   :description "The DE registers this endpoint as a callback when it submits jobs to Tapis."
                   (ok (apps/update-tapis-job-status job-id body))))))
