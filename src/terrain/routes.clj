(ns terrain.routes
  (:use [cheshire.core :as cheshire]
        [clojure-commons.lcase-params :only [wrap-lcase-params]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [common-swagger-api.schema]
        [ring.middleware.keyword-params :only [wrap-keyword-params]]
        [service-logging.middleware :only [wrap-logging clean-context]]
        [terrain.auth.user-attributes]
        [terrain.middleware :only [wrap-context-path-adder wrap-query-param-remover]]
        [terrain.routes.admin]
        [terrain.routes.analyses]
        [terrain.routes.apps.admin.apps]
        [terrain.routes.apps.admin.reference-genomes]
        [terrain.routes.apps.admin.tools]
        [terrain.routes.apps.categories]
        [terrain.routes.apps.communities]
        [terrain.routes.apps.elements]
        [terrain.routes.apps.metadata]
        [terrain.routes.apps.pipelines]
        [terrain.routes.apps.reference-genomes]
        [terrain.routes.apps.tools]
        [terrain.routes.bags]
        [terrain.routes.bootstrap]
        [terrain.routes.dashboard-aggregator]
        [terrain.routes.data]
        [terrain.routes.fileio]
        [terrain.routes.filesystem]
        [terrain.routes.filesystem.exists]
        [terrain.routes.filesystem.navigation]
        [terrain.routes.filesystem.stats]
        [terrain.routes.filesystem.tickets]
        [terrain.routes.groups]
        [terrain.routes.metadata]
        [terrain.routes.misc]
        [terrain.routes.notification]
        [terrain.routes.permanent-id-requests]
        [terrain.routes.pref]
        [terrain.routes.session]
        [terrain.routes.user-info]
        [terrain.routes.collaborator]
        [terrain.routes.search]
        [terrain.routes.coge]
        [terrain.routes.oauth]
        [terrain.routes.favorites]
        [terrain.routes.tags]
        [terrain.routes.token]
        [terrain.routes.webhooks]
        [terrain.routes.comments]
        [terrain.routes.requests]
        [terrain.routes.settings]
        [terrain.routes.vice]
        [terrain.util :as util]
        [terrain.util.transformers :as transform])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.exception :as cx]
            [compojure.route :as route]
            [service-logging.thread-context :as tc]
            [terrain.util :as util]
            [terrain.util.service :as service]
            [terrain.util.config :as config]))

(defn- wrap-user-info
  [handler]
  (fn [request]
    (let [user-info (transform/add-current-user-to-map {})]
      (log/log 'AccessLogger :trace nil "entering wrap-user-info")
      (if (nil? (:user user-info))
        (handler request)
        (tc/with-logging-context {:user-info (cheshire/encode user-info)}
          (handler request))))))

(defn optionally-authenticated-routes
  "Returns a list of routes that may be called with or without authentication credentials. Note that this
   set of routes shouldn't contain a call to `route/not-found` because we want the call to fall through to
   the next set of routes (`secured-routes-no-context`) if nothing matches."
  []
  (util/flagged-routes
   (app-ontology-routes)
   (apps-routes)
   (dashboard-aggregator-routes)
   (filesystem-stat-routes)
   (quicklaunch-routes)
   (secured-data-routes)
   (secured-filesystem-routes)
   (secured-search-routes)))

; Add new secured routes to this function, not to (secured-routes).
; This function allows for secured routes without the /secured content/prefix,
; which is what the -no-context refers to.
(defn secured-routes-no-context
  []
  (util/flagged-routes
   (app-category-routes)
   (app-avu-routes)
   (app-comment-routes)
   (app-community-routes)
   (app-community-tag-routes)
   (app-elements-routes)
   (app-pipeline-routes)
   (analysis-routes)
   (coge-routes)
   (collaborator-list-routes)
   (community-routes)
   (team-routes)
   (subject-routes)
   (reference-genomes-routes)
   (tool-routes)
   (tool-request-routes)
   (permanent-id-request-routes)
   (webhook-routes)
   (misc-metadata-routes)
   (oauth-routes)
   (request-routes)
   (bag-routes)
   (route/not-found (service/unrecognized-path-response))))

; The old way of adding secured routes. Prepends /secured to the URL
; path. Add new secured endpoints to (secured-routes-no-context), not
; here.
(defn secured-routes
  []
  (util/flagged-routes
   (secured-notification-routes)
   (secured-bootstrap-routes)
   (secured-pref-routes)
   (secured-user-info-routes)
   (secured-data-routes)
   (secured-session-routes)
   (secured-fileio-routes)
   (filesystem-navigation-routes)
   (filesystem-existence-routes)
   (filesystem-stat-routes)
   (filesystem-ticket-routes)
   (secured-filesystem-routes)
   (secured-filesystem-metadata-routes)
   (secured-search-routes)
   (secured-oauth-routes)
   (secured-favorites-routes)
   (secured-tag-routes)
   (data-comment-routes)
   (route/not-found (service/unrecognized-path-response))))

(defn admin-routes
  []
  (util/flagged-routes
   (secured-admin-routes)
   (admin-data-comment-routes)
   (admin-category-routes)
   (admin-apps-routes)
   (admin-app-avu-routes)
   (admin-app-comment-routes)
   (admin-app-community-routes)
   (admin-comment-routes)
   (admin-community-routes)
   (admin-filesystem-metadata-routes)
   (admin-groups-routes)
   (admin-notification-routes)
   (admin-ontology-routes)
   (admin-reference-genomes-routes)
   (admin-tool-routes)
   (admin-tool-request-routes)
   (admin-permanent-id-request-routes)
   (oauth-admin-routes)
   (admin-integration-data-routes)
   (admin-workspace-routes)
   (admin-user-info-routes)
   (admin-request-routes)
   (admin-setting-routes)
   (admin-vice-routes)
   (route/not-found (service/unrecognized-path-response))))

(defn unsecured-routes
  []
  (util/flagged-routes
   (admin-token-routes)
   (token-routes)
   (unsecured-misc-routes)
   (unsecured-notification-routes)))

(def admin-handler
  (middleware
   [authenticate-current-user
    require-authentication
    wrap-user-info
    validate-current-user
    wrap-logging]
   (admin-routes)))

(def secured-routes-handler
  (middleware
   [authenticate-current-user
    require-authentication
    wrap-user-info
    wrap-logging]
   (secured-routes)))

(def optionally-authenticated-routes-handler
  (middleware
   [authenticate-current-user
    wrap-user-info
    wrap-logging]
   (optionally-authenticated-routes)))

(def secured-routes-no-context-handler
  (middleware
   [authenticate-current-user
    require-authentication
    wrap-user-info
    wrap-logging]
   (secured-routes-no-context)))

(def unsecured-routes-handler
  (middleware
   [wrap-logging]
   (unsecured-routes)))

(defn- terrain-routes
  []
  (util/flagged-routes
   unsecured-routes-handler
   (context "/admin" [] admin-handler)
   (context "/secured" [] secured-routes-handler)
   optionally-authenticated-routes-handler
   secured-routes-no-context-handler))

(defn- site-handler
  [routes-fn]
  (-> (routes-fn)
      wrap-keyword-params
      wrap-lcase-params
      wrap-query-params
      clean-context))

(def ^:private security-definitions
  {:basic  {:type "basic"}
   :bearer {:type "apiKey"
            :name "Authorization"
            :in   "header"}})

(defapi app
  {:exceptions cx/exception-handlers}
  (swagger-routes
   {:ui       (str "/terrain" config/docs-uri)
    :spec     "/terrain/swagger.json"
    :data     {:info                {:title       "Discovery Environment API"
                                     :description "Documentation for the Discovery Environment REST API"
                                     :version     "0.1.0"}
               :tags                [{:name "admin", :description "General Admin Endpoints"}
                                     {:name "admin-apps", :description "App Administration Endpoints"}
                                     {:name "admin-comments", :description "Comment Administration Endpoints"}
                                     {:name "admin-communities", :description "Community Administration Endpoints"}
                                     {:name "admin-filesystem", :description "File System Administration Endpoints"}
                                     {:name "admin-reference-genomes", :description "Admin Reference Genome Endpoints"}
                                     {:name "admin-requests", :description "Admin Request Endpoints"}
                                     {:name "admin-settings", :description "Admin Setting Endpoints"}
                                     {:name "admin-token", :description "Admin OAuth Tokens"}
                                     {:name "admin-tools", :description "Admin Tool Endpoints"}
                                     {:name "admin-tool-requests", :description "Admin Tool Request Endpoints"}
                                     {:name "admin-user-info", :description "User Info Administration Endpoints"}
                                     {:name "admin-vice", :description "VICE Administration Endpoints"}
                                     {:name "analyses", :description "Analysis Endpoints"}
                                     {:name "analyses-quicklaunches", :description "Quick Launch Endpoints"}
                                     {:name "apps", :description, "Apps Endpoints"}
                                     {:name "app-categories", :description "App Category Endpoints"}
                                     {:name "app-communities", :description "App Community Endpoints"}
                                     {:name "app-community-tags", :description "App Community Tag Endpoints"}
                                     {:name "app-hierarchies", :description "App Hierarchy Endpoints"}
                                     {:name "app-element-types", :description, "App Element Endpoints"}
                                     {:name "app-metadata", :description "App Metadata Endpoints"}
                                     {:name "app-pipelines", :description "App Pipeline Endpoints"}
                                     {:name "bootstrap", :description "Bootstrap Endpoints"}
                                     {:name "coge", :description "CoGe Endpoints"}
                                     {:name "collaborator-lists", :description "Collaborator List Endpoints"}
                                     {:name "communities", :description "Community Endpoints"}
                                     {:name "dashboard", :description "Dashboard Aggregator Endpoints"}
                                     {:name "data", :description "Data Endpoints"}
                                     {:name "favorites", :description "Favorites Endpoints"}
                                     {:name "fileio", :description "File Input/Output Endpoints"}
                                     {:name "filesystem", :description "Filesystem Endpoints"}
                                     {:name "reference-genomes", :description "Reference Genome Endpoints"}
                                     {:name "requests", :description "Request Endpoints"}
                                     {:name "subjects", :description "Subject Endpoints"}
                                     {:name "tags", :description "Tag Endpoints"}
                                     {:name "teams", :description "Team Endpoints"}
                                     {:name "tools", :description "Tool Endpoints"}
                                     {:name "tool-requests", :description "Tool Request Endpoints"}
                                     {:name "token", :description "OAuth Tokens"}
                                     {:name "user-info", :description "User Information Endpoints"}
                                     {:name "webhooks", :description "Webhook Endpoints"}]
               :securityDefinitions security-definitions}})
  (middleware
   [[wrap-query-param-remover "ip-address" #{#"^/terrain/secured/bootstrap"
                                             #"^/terrain/secured/logout"}]
    wrap-query-params
    wrap-lcase-params
    wrap-keyword-params
    clean-context]
   (context "/terrain" []
     (terrain-routes))))

(def app-wrapper
  (wrap-context-path-adder app "/terrain"))
