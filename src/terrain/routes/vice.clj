(ns terrain.routes.vice
  (:require [common-swagger-api.schema :refer [context GET POST PATCH DELETE]]
            [ring.util.http-response :refer [ok]]
            [terrain.clients.app-exposer :as vice]
            [terrain.routes.schemas.vice :as vice-schema]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare params analysis-id host id)

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

     (context "/operators" []
       :tags ["admin-vice-operators"]

       (GET "/" []
         :return vice-schema/OperatorAdminSummaryList
         :summary "List registered operators"
         :description "Returns id, name, URL, base_url, tls_skip_verify, and priority for all operators in the database."
         (ok (vice/admin-list-operators)))

       (POST "/" []
         :body [body vice-schema/OperatorConfig]
         :return vice-schema/OperatorAdminSummary
         :summary "Register a new operator"
         :description "Adds a new operator to the database and returns the persisted row, including its server-assigned UUID."
         (ok (vice/admin-create-operator body)))

       (GET "/capacities" []
         :return vice-schema/OperatorCapacityList
         :summary "Get live operator capacities"
         :description "Queries each configured operator's capacity endpoint in parallel and returns the results."
         (ok (vice/admin-get-operator-capacities)))

       (context "/id/:id" []
         :path-params [id :- vice-schema/OperatorIDParam]

         (PATCH "/" []
           :body [body vice-schema/UpdateOperatorRequest]
           :return vice-schema/OperatorAdminSummary
           :summary "Update an operator"
           :description "Partial update of an operator identified by UUID. Only fields supplied in the body are changed."
           (ok (vice/admin-update-operator id body)))

         (DELETE "/" []
           :summary "Delete an operator"
           :description "Removes the operator with the given UUID. Idempotent. Fails if jobs still reference the operator."
           (ok (vice/admin-delete-operator id)))))

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
