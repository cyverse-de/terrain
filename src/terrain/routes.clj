(ns terrain.routes
  (:require
   [cheshire.core :as cheshire]
   [clojure-commons.exception :as cx]
   [clojure-commons.lcase-params :refer [wrap-lcase-params]]
   [clojure-commons.query-params :refer [wrap-query-params]]
   [clojure.tools.logging :as log]
   [common-swagger-api.schema :as schema]
   [compojure.api.core :refer [route-middleware]]
   [compojure.route :as route]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [service-logging.middleware :refer [clean-context wrap-logging]]
   [service-logging.thread-context :as tc]
   [terrain.auth.user-attributes :as user-attributes]
   [terrain.middleware :refer [wrap-context-path-adder wrap-create-workspace
                               wrap-query-param-remover]]
   [terrain.routes.admin :refer [secured-admin-routes]]
   [terrain.routes.alerts :refer [admin-alerts-routes alerts-routes]]
   [terrain.routes.analyses :refer [analysis-routes quicklaunch-routes]]
   [terrain.routes.apps.admin.apps :refer [admin-apps-routes]]
   [terrain.routes.apps.admin.reference-genomes :refer [admin-reference-genomes-routes]]
   [terrain.routes.apps.admin.tools :refer [admin-tool-request-routes
                                            admin-tool-routes]]
   [terrain.routes.apps.categories :refer [app-category-routes
                                           app-community-routes
                                           app-ontology-routes]]
   [terrain.routes.apps.communities :refer [app-community-tag-routes]]
   [terrain.routes.apps.elements :refer [app-elements-routes]]
   [terrain.routes.apps.metadata :refer [app-avu-routes]]
   [terrain.routes.apps.pipelines :refer [app-pipeline-routes]]
   [terrain.routes.apps.reference-genomes :refer [reference-genomes-routes]]
   [terrain.routes.apps.tools :refer [tool-request-routes tool-routes]]
   [terrain.routes.apps.versions :refer [app-version-routes]]
   [terrain.routes.bags :refer [bag-routes]]
   [terrain.routes.bootstrap :refer [secured-bootstrap-routes]]
   [terrain.routes.callbacks :refer [callback-routes]]
   [terrain.routes.coge :refer [coge-routes]]
   [terrain.routes.collaborator :refer [admin-community-routes
                                        collaborator-list-routes
                                        community-routes subject-routes
                                        team-routes]]
   [terrain.routes.comments :refer [admin-app-comment-routes
                                    admin-comment-routes
                                    admin-data-comment-routes
                                    app-comment-routes data-comment-routes]]
   [terrain.routes.dashboard-aggregator :refer [dashboard-aggregator-routes]]
   [terrain.routes.data :refer [secured-data-routes]]
   [terrain.routes.data-usage-api :refer [data-usage-api-routes]]
   [terrain.routes.email :refer [service-account-email-routes]]
   [terrain.routes.favorites :refer [secured-favorites-routes]]
   [terrain.routes.fileio :refer [secured-fileio-routes]]
   [terrain.routes.filesystem :refer [admin-filesystem-metadata-routes
                                      secured-filesystem-metadata-routes
                                      secured-filesystem-routes]]
   [terrain.routes.filesystem.exists :refer [filesystem-existence-routes]]
   [terrain.routes.filesystem.navigation :refer [filesystem-navigation-routes]]
   [terrain.routes.filesystem.stats :refer [filesystem-stat-routes]]
   [terrain.routes.filesystem.tickets :refer [filesystem-ticket-routes]]
   [terrain.routes.groups :refer [admin-groups-routes]]
   [terrain.routes.instantlaunches :refer [admin-instant-launch-routes
                                           instant-launch-routes]]
   [terrain.routes.metadata :refer [admin-app-avu-routes
                                    admin-app-community-routes
                                    admin-category-routes
                                    admin-integration-data-routes
                                    admin-ontology-routes
                                    admin-workspace-routes apps-routes
                                    misc-metadata-routes]]
   [terrain.routes.misc :refer [unsecured-misc-routes]]
   [terrain.routes.notification :refer [secured-notification-routes]]
   [terrain.routes.oauth :refer [oauth-admin-routes oauth-routes
                                 secured-oauth-routes]]
   [terrain.routes.permanent-id-requests :refer [admin-permanent-id-request-routes
                                                 permanent-id-request-routes]]
   [terrain.routes.pref :refer [secured-pref-routes]]
   [terrain.routes.qms :refer [admin-qms-api-routes qms-api-routes
                               service-account-qms-api-routes]]
   [terrain.routes.requests :refer [admin-request-routes
                                    admin-request-type-routes request-routes]]
   [terrain.routes.resource-usage-api :refer [resource-usage-api-routes]]
   [terrain.routes.search :refer [secured-search-routes]]
   [terrain.routes.session :refer [secured-session-routes]]
   [terrain.routes.settings :refer [admin-setting-routes]]
   [terrain.routes.tags :refer [secured-tag-routes]]
   [terrain.routes.token :refer [admin-token-routes token-routes]]
   [terrain.routes.user-info :refer [admin-user-info-routes
                                     secured-user-info-routes
                                     service-account-user-info-routes]]
   [terrain.routes.vice :refer [admin-vice-routes vice-routes]]
   [terrain.routes.webhooks :refer [webhook-routes]]
   [terrain.util :as util]
   [terrain.util.config :as config]
   [terrain.util.service :as service]
   [terrain.util.transformers :as transform]))

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
   (dashboard-aggregator-routes)
   (filesystem-stat-routes)
   (quicklaunch-routes)
   (instant-launch-routes)
   (secured-data-routes)
   (secured-fileio-routes)
   (secured-filesystem-routes)
   (secured-filesystem-metadata-routes)
   (secured-search-routes)
   (alerts-routes)
   (app-category-routes)
   (app-avu-routes)
   (app-comment-routes)
   (app-ontology-routes)
   (app-community-routes)
   (app-community-tag-routes)
   (app-elements-routes)
   (app-pipeline-routes)
   (apps-routes)
   (app-version-routes)
   (qms-api-routes)
   (misc-metadata-routes)))

; Add new secured routes to this function, not to (secured-routes).
; This function allows for secured routes without the /secured content/prefix,
; which is what the -no-context refers to.
(defn secured-routes-no-context
  []
  (util/flagged-routes
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
   (oauth-routes)
   (request-routes)
   (bag-routes)
   (vice-routes)
   (resource-usage-api-routes)
   (data-usage-api-routes)
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
   (admin-alerts-routes)
   (admin-apps-routes)
   (admin-app-avu-routes)
   (admin-app-comment-routes)
   (admin-app-community-routes)
   (admin-comment-routes)
   (admin-community-routes)
   (admin-filesystem-metadata-routes)
   (admin-groups-routes)
   (admin-instant-launch-routes)
   (admin-ontology-routes)
   (admin-reference-genomes-routes)
   (admin-tool-routes)
   (admin-tool-request-routes)
   (admin-permanent-id-request-routes)
   (oauth-admin-routes)
   (admin-integration-data-routes)
   (admin-workspace-routes)
   (admin-user-info-routes)
   (admin-request-type-routes)
   (admin-request-routes)
   (admin-setting-routes)
   (admin-vice-routes)
   (admin-qms-api-routes)
   (route/not-found (service/unrecognized-path-response))))

(defn service-account-routes
  []
  (util/flagged-routes
   (service-account-qms-api-routes)
   (service-account-email-routes)
   (service-account-user-info-routes)
   (route/not-found (service/unrecognized-path-response))))

(defn unsecured-routes
  []
  (util/flagged-routes
   (admin-token-routes)
   (callback-routes)
   (token-routes)
   (unsecured-misc-routes)))

(def admin-handler
  (route-middleware
   [user-attributes/authenticate-current-user
    user-attributes/require-authentication
    wrap-user-info
    user-attributes/validate-current-user
    wrap-logging
    wrap-create-workspace]
   (admin-routes)))

(def service-account-handler
  (route-middleware
    [user-attributes/authenticate-current-user
     wrap-user-info
     wrap-logging]
    (service-account-routes)))

(def secured-routes-handler
  (route-middleware
   [user-attributes/authenticate-current-user
    user-attributes/require-authentication
    wrap-user-info
    wrap-logging
    wrap-create-workspace]
   (secured-routes)))

(def optionally-authenticated-routes-handler
  (route-middleware
   [user-attributes/authenticate-current-user
    wrap-user-info
    wrap-logging
    wrap-create-workspace]
   (optionally-authenticated-routes)))

(def secured-routes-no-context-handler
  (route-middleware
   [user-attributes/authenticate-current-user
    user-attributes/require-authentication
    wrap-user-info
    wrap-logging
    wrap-create-workspace]
   (secured-routes-no-context)))

(def unsecured-routes-handler
  (route-middleware
   [wrap-logging]
   (unsecured-routes)))

(defn- terrain-routes
  []
  (util/flagged-routes
   unsecured-routes-handler
   (schema/context "/admin" [] admin-handler)
   (schema/context "/service" [] service-account-handler)
   (schema/context "/secured" [] secured-routes-handler)
   optionally-authenticated-routes-handler
   secured-routes-no-context-handler))

(def ^:private security-definitions
  {:basic  {:type "basic"}
   :bearer {:type "apiKey"
            :name "Authorization"
            :in   "header"}})

(schema/defapi app
  {:exceptions cx/exception-handlers}
  (schema/swagger-routes
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
                                     {:name "admin-instant-launches", :description "Instant Launch Administration Endpoints"}
                                     {:name "admin-permanent-id-requests", :description "Admin Permanent ID Request Endpoints"}
                                     {:name "admin-qms", :description "Admin Quota Management Service Endpoints"}
                                     {:name "admin-reference-genomes", :description "Admin Reference Genome Endpoints"}
                                     {:name "admin-request-types", :description "Admin Request Type Endpoints"}
                                     {:name "admin-requests", :description "Admin Request Endpoints"}
                                     {:name "admin-resource-usage", :description "Resource Usage Administration Endpoints"}
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
                                     {:name "app-versions", :description, "App Version Endpoints"}
                                     {:name "bags", :description "Item Bag Endpoints"}
                                     {:name "bootstrap", :description "Bootstrap Endpoints"}
                                     {:name "callbacks", :description "Callback Endpoints"}
                                     {:name "coge", :description "CoGe Endpoints"}
                                     {:name "collaborator-lists", :description "Collaborator List Endpoints"}
                                     {:name "communities", :description "Community Endpoints"}
                                     {:name "dashboard", :description "Dashboard Aggregator Endpoints"}
                                     {:name "data", :description "Data Endpoints"}
                                     {:name "favorites", :description "Favorites Endpoints"}
                                     {:name "fileio", :description "File Input/Output Endpoints"}
                                     {:name "filesystem", :description "Filesystem Endpoints"}
                                     {:name "instant-launches", :description "Instant Launch Endpoints"}
                                     {:name "notifications", :description "Notifications and related Endpoints"}
                                     {:name "permanent-id-requests", :description "Permanent ID Request Endpoints"}
                                     {:name "qms", :description "Quota Management Service Endpoints"}
                                     {:name "reference-genomes", :description "Reference Genome Endpoints"}
                                     {:name "requests", :description "Request Endpoints"}
                                     {:name "resource-usage" :description "Resource Usage Endpoints"}
                                     {:name "subjects", :description "Subject Endpoints"}
                                     {:name "support", :description "Support Endpoints"}
                                     {:name "tags", :description "Tag Endpoints"}
                                     {:name "teams", :description "Team Endpoints"}
                                     {:name "tools", :description "Tool Endpoints"}
                                     {:name "tool-requests", :description "Tool Request Endpoints"}
                                     {:name "token", :description "OAuth Tokens"}
                                     {:name "user-info", :description "User Information Endpoints"}
                                     {:name "webhooks", :description "Webhook Endpoints"}
                                     {:name "vice", :description "VICE Endpoints"}
                                     {:name "service-account-email" :description "Service Account Email Endpoints"}
                                     {:name "service-account-qms", :description "Service Account QMS Endpoints"}
                                     {:name "service-account-user-info", :description "Service Account User Info Endpoints"}]
               :securityDefinitions security-definitions}})
  (route-middleware
   [[wrap-query-param-remover "ip-address" #{#"^/terrain/secured/bootstrap"
                                             #"^/terrain/secured/logout"}]
    wrap-query-params
    wrap-lcase-params
    wrap-keyword-params
    clean-context]
   (schema/context "/terrain" []
     (terrain-routes))))

(def app-wrapper
  (wrap-context-path-adder app "/terrain"))
