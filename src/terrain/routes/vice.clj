(ns terrain.routes.vice
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.services.user-info]
        [terrain.util])
  (:require [terrain.clients.app-exposer :as vice]
            [terrain.routes.schemas.vice :as vice-schema]
            [terrain.util.config :as config]))

(defn admin-vice-routes
  []
  (optional-routes
    [config/app-routes-enabled]
    
    (context "/vice" []
      :tags ["admin-vice"]
      
      (GET "/resources" []
        :query [filter vice-schema/FilterParams]
        :return vice-schema/FullResourceListing
        :summary "List Kubernetes resources deployed in the cluster"
        :description "Lists all Kubernetes resources associated with an analysis running in the cluster."
        (ok (vice/get-resources filter)))

      (GET "/async-data" []
        :query [params vice-schema/AsyncDataParams]
        :return vice-schema/AsyncData
        :summary "Get data that is generated asynchronously"
        :description "Get data for the VICE analysis that is generated asynchronously"
        (ok (vice/async-data params)))
      
      (GET "/analyses/:analysis-id/time-limit" []
        :path-params [analysis-id :- vice-schema/AnalysisID]
        :return vice-schema/TimeLimit
        :summary "Get current time limit"
        :description "Gets the current time limit set for the analysis"
        (ok (vice/admin-get-time-limit analysis-id)))
        
      (POST "/analyses/:analysis-id/time-limit" []
        :path-params [analysis-id :- vice-schema/AnalysisID]
        :return vice-schema/TimeLimit
        :summary "Extend the time limit"
        :description "Extends the time limit for the analysis by 3 days"
        (ok (vice/admin-set-time-limit analysis-id)))
        
      (POST "/analyses/:analysis-id/save-and-exit" []
        :path-params [analysis-id :- vice-schema/AnalysisID]
        :summary "Upload outputs and exit"
        :description "Terminates the analysis after uploading the output files to the data store"
        (ok (vice/admin-cancel-analysis analysis-id)))
      
      (POST "/analyses/:analysis-id/exit" []
        :path-params [analysis-id :- vice-schema/AnalysisID]
        :summary "Exit without saving"
        :description "Terminates the analysis without uploading files to the data store"
        (ok (vice/admin-exit analysis-id)))
      
      (POST "/analyses/:analysis-id/save-output-files" []
        :path-params [analysis-id :- vice-schema/AnalysisID]
        :summary "Upload files without exiting"
        :description "Uploads output files for the analysis to the data store without terminating the analysis"
        (ok (vice/admin-save-output-files analysis-id)))
      
      (POST "/analyses/:analysis-id/download-input-files" []
        :path-params [analysis-id :- vice-schema/AnalysisID]
        :summary "Download input files"
        :description "Downloads input files to the analysis container without changing the status of the analysis"
        (ok (vice/admin-download-input-files analysis-id))))))

