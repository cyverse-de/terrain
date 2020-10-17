(ns terrain.routes.schemas.dashboard-aggregator
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema Any maybe optional-key]])
  (:import [java.util UUID]))

(defschema DashboardApp
  {:id                              (describe String "The app ID")
   :system_id                       (describe String "The system ID for the app")
   (optional-key :name)             (describe (maybe String) "The name of the app")
   (optional-key :description)      (describe (maybe String) "The description of the app")
   :username                        (describe String "The name of the user that created the app")
   (optional-key :wiki_url)         (describe (maybe String) "The URL to the wiki entry for the app")
   (optional-key :integration_date) (describe (maybe String) "The date the app was integrated. Milliseconds since epoch")
   (optional-key :edited_date)      (describe (maybe String) "The date the app was last edited")
   (optional-key :is_favorite)      (describe (maybe Boolean) "True if the user has marked the app as a favorite")
   (optional-key :is_public)        (describe (maybe Boolean) "True if the app is publicly accessible")})

(defschema DashboardAnalysis
  {:id                                (describe UUID "The analysis/job ID")
   :name                              (describe String "The name of the analysis")
   (optional-key :description)        (describe (maybe String) "The description of the analysis")
   :username                          (describe String "The name of the user that created the app")
   :app_id                            (describe String "The ID of the app used for the analysis")
   (optional-key :app_name)           (describe (maybe String) "The name of the app used for the analysis")
   (optional-key :app_description)    (describe (maybe String) "The description of the app used for the analysis")
   (optional-key :result_folder_path) (describe (maybe String) "The path to the analysis outputs")
   :start_date                        (describe (maybe String) "The date the analysis was started. Milliseconds since the epoch")
   (optional-key :end_date)           (describe (maybe String) "The date the analysis ended. Milliseconds since the epoch")
   (optional-key :planned_end_date)   (describe (maybe String) "The date the analysis was scheduled to end. VICE only. Milliseconds since the epoch")
   (optional-key :status)             (describe (maybe String) "The current status of the analysis")
   (optional-key :subdomain)          (describe (maybe String) "The subdomain assigned to the analysis. VICE only")
   (optional-key :parent_id)          (describe (maybe UUID) "The UUID of the parent analysis. Only for batch analyses")})

(defschema DashboardFeedItem
  {:id                     (describe String "The unique identifier for a feed. Probably not a UUID")
   :name                   (describe String "The name of the item in the feed. Not the name of the feed itself")
   :description            (describe String "Corresponds to the content snippet provided by the feed. Named description to match the other types")
   :link                   (describe String "Link to the source of the feed item. Probably leads to the website")
   :date_added             (describe String "The date the item was added to the feed")
   :publication_date       (describe String "The date the item was originally published")
   :author                 (describe String "The author of the item")
   (optional-key :content) (describe (maybe String) "The content of the item")})

(defschema DashboardFeeds
  {(optional-key :news)   (describe (maybe [DashboardFeedItem]) "The news feed")
   (optional-key :events) (describe (maybe [DashboardFeedItem]) "The events feed")
   (optional-key :videos) (describe (maybe [DashboardFeedItem]) "The videos feed")})

(defschema DashboardAggregatedApps
  {(optional-key :recentlyAdded) (describe (maybe [DashboardApp]) "Apps recently added by the user")
   :public                       (describe [DashboardApp] "Apps recently made public")})

(defschema DashboardAggregatedAnalyses
  {(optional-key :recent)  (describe (maybe [DashboardAnalysis]) "Analyses recent launched by the user")
   (optional-key :running) (describe (maybe [DashboardAnalysis]) "Analyses currently running for the user")})

(defschema DashboardAggregatorResponse
  {:apps                    (describe DashboardAggregatedApps "The app listings returned for the dashboard")
   (optional-key :analyses) (describe DashboardAggregatedAnalyses "The analysis listings returned for the dashboard")
   (optional-key :feeds)    (describe DashboardFeeds "Information from RSS feeds on the website")})

(defschema DashboardRequestParams
  {(optional-key :limit) (describe (maybe Long) "The number of responses to include in each field.")})
