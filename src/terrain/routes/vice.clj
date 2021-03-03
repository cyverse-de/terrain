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
       (ok (vice/admin-get-resources filter)))

     (GET "/async-data" []
       :query [params vice-schema/AsyncDataParams]
       :return vice-schema/AsyncData
       :summary "Get data that is generated asynchronously"
       :description "Get data for the VICE analysis that is generated asynchronously"
       (ok (vice/async-data params)))

     (context "/analyses" []
       (context "/:analysis-id" []
         :path-params [analysis-id :- vice-schema/AnalysisID]

         (GET "/time-limit" []
           :return vice-schema/TimeLimit
           :summary "Get current time limit"
           :description "Gets the current time limit set for the analysis"
           (ok (vice/admin-get-time-limit analysis-id)))

         (POST "/time-limit" []
           :return vice-schema/TimeLimit
           :summary "Extend the time limit"
           :description "Extends the time limit for the analysis by 3 days"
           (ok (vice/admin-set-time-limit analysis-id)))

         (POST "/save-and-exit" []
           :summary "Upload outputs and exit"
           :description "Terminates the analysis after uploading the output files to the data store"
           (ok (vice/admin-cancel-analysis analysis-id)))

         (POST "/exit" []
           :summary "Exit without saving"
           :description "Terminates the analysis without uploading files to the data store"
           (ok (vice/admin-exit analysis-id)))

         (POST "/save-output-files" []
           :summary "Upload files without exiting"
           :description "Uploads output files for the analysis to the data store without terminating the analysis"
           (ok (vice/admin-save-output-files analysis-id)))

         (POST "/download-input-files" []
           :summary "Download input files"
           :description "Downloads input files to the analysis container without changing the status of the analysis"
           (ok (vice/admin-download-input-files analysis-id)))

         (GET "/external-id" []
           :return vice-schema/ExternalIDResponse
           :summary "Get external UUID"
           :description "Returns the external UUID associated with the analysis. VICE analyses only have a single external UUID"
           (ok (vice/admin-external-id analysis-id)))))

     (context "/:host" []
       :path-params [host :- vice-schema/Host]

       (GET "/url-ready" []
         :return vice-schema/URLReady
         :summary vice-schema/URLReadySummary
         :description vice-schema/URLReadyDescription
         (ok (vice/admin-url-ready host)))

       (GET "/description" []
         :return vice-schema/FullResourceListing
         :summary vice-schema/DescriptionSummary
         :description vice-schema/AdminDescriptionDescription
         (ok (vice/admin-get-description-by-host host)))))))

(defn vice-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (context "/vice" []
     :tags ["vice"]

     (GET "/resources" []
       :query [filter vice-schema/NonAdminFilterParams]
       :return vice-schema/FullResourceListing
       :summary "List Kubernetes resources deployed in the cluster"
       :description "Lists all Kubernetes resources associated with an analysis running in the cluster for a user"
       (ok (vice/get-resources filter)))

     (context "/:host" []
       :path-params [host :- vice-schema/Host]

       (GET "/url-ready" []
         :return vice-schema/URLReady
         :summary vice-schema/URLReadySummary
         :description vice-schema/URLReadyDescription
         (ok (vice/url-ready host)))

       (GET "/description" []
         :return vice-schema/FullResourceListing
         :summary vice-schema/DescriptionSummary
         :description vice-schema/DescriptionDescription
         (ok (vice/get-description-by-host host)))))))

