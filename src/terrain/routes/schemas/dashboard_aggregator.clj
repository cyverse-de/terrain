(ns terrain.routes.schemas.dashboard-aggregator
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema Any maybe optional-key]])
  (:import [java.util UUID]))

(defschema DashboardApp
  {:id                              (describe UUID "The app ID")
   :name                            (describe String "The name of the app")
   :description                     (describe (maybe String) "The description of the app")
   (optional-key :wiki_url)         (describe (maybe String) "The URL to the wiki entry for the app")
   (optional-key :integration_date) (describe (maybe Long) "The date the app was integrated. Milliseconds since epoch")
   (optional-key :edited_date)      (describe (maybe Long) "The date the app was last edited")})

(defschema DashboardAnalysis
  {:id                                (describe UUID "The analysis/job ID")
   :name                              (describe String "The name of the analysis")
   :description                       (describe (maybe String) "The description of the analysis")
   :app_id                            (describe UUID "The ID of the app used for the analysis")
   :app_name                          (describe String "The name of the app used for the analysis")
   :app_description                   (describe (maybe String) "The description of the app used for the analysis")
   (optional-key :result_folder_path) (describe (maybe String) "The path to the analysis outputs")
   :start_date                        (describe (maybe Long) "The date the analysis was started. Milliseconds since the epoch")
   (optional-key :end_date)           (describe (maybe Long) "The date the analysis ended. Milliseconds since the epoch")
   (optional-key :planned_end_date)   (describe (maybe Long) "The date the analysis was scheduled to end. VICE only. Milliseconds since the epoch")
   (optional-key :status)             (describe (maybe String) "The current status of the analysis")
   (optional-key :subdomain)          (describe (maybe String) "The subdomain assigned to the analysis. VICE only")
   (optional-key :parent_id)          (describe (maybe UUID) "The UUID of the parent analysis. Only for batch analyses")})

(defschema DashboardAggregatedApps
  {(optional-key :recentlyAdded) (describe (maybe [DashboardApp]) "Apps recently added by the user")
   :public                       (describe [DashboardApp] "Apps recently made public")})

(defschema DashboardAggregatedAnalyses
  {(optional-key :recent)  (describe (maybe [DashboardAnalysis]) "Analyses recent launched by the user")
   (optional-key :running) (describe (maybe [DashboardAnalysis]) "Analyses currently running for the user")})

(defschema DashboardAggregatorResponse
 {:apps                    (describe DashboardAggregatedApps "The app listings returned for the dashboard")
  (optional-key :analyses) (describe DashboardAggregatedAnalyses "The analysis listings returned for the dashboard")})

(defschema DashboardRequestParams
  {(optional-key :limit) (describe (maybe Integer) "The number of responses to include in each field.")})