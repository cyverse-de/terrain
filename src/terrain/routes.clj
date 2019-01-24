(ns terrain.routes
  (:use [cheshire.core :as cheshire]
        [clojure-commons.lcase-params :only [wrap-lcase-params]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [common-swagger-api.schema]
        [ring.middleware.keyword-params :only [wrap-keyword-params]]
        [service-logging.middleware :only [wrap-logging clean-context]]
        [terrain.auth.user-attributes]
        [terrain.middleware :only [wrap-context-path-remover]]
        [terrain.routes.admin]
        [terrain.routes.data]
        [terrain.routes.permanent-id-requests]
        [terrain.routes.fileio]
        [terrain.routes.groups]
        [terrain.routes.metadata]
        [terrain.routes.misc]
        [terrain.routes.notification]
        [terrain.routes.pref]
        [terrain.routes.session]
        [terrain.routes.user-info]
        [terrain.routes.collaborator]
        [terrain.routes.filesystem]
        [terrain.routes.search]
        [terrain.routes.coge]
        [terrain.routes.oauth]
        [terrain.routes.favorites]
        [terrain.routes.tags]
        [terrain.routes.token]
        [terrain.routes.webhooks]
        [terrain.routes.comments]
        [terrain.util :as util]
        [terrain.util.transformers :as transform])
  (:require [cemerick.url :as curl]
            [clojure.tools.logging :as log]
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

(defn secured-routes-no-context
  []
  (util/flagged-routes
   (app-category-routes)
   (app-avu-routes)
   (app-comment-routes)
   (app-ontology-routes)
   (app-community-routes)
   (apps-routes)
   (analysis-routes)
   (coge-routes)
   (collaborator-list-routes)
   (community-routes)
   (team-routes)
   (subject-routes)
   (reference-genomes-routes)
   (tool-routes)
   (permanent-id-request-routes)
   (webhook-routes)
   (misc-metadata-routes)
   (oauth-routes)
   (route/not-found (service/unrecognized-path-response))))

(defn secured-routes
  []
  (util/flagged-routes
    (secured-notification-routes)
    (secured-metadata-routes)
    (secured-pref-routes)
    (secured-collaborator-routes)
    (secured-user-info-routes)
    (secured-data-routes)
    (secured-session-routes)
    (secured-fileio-routes)
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
    (admin-permanent-id-request-routes)
    (oauth-admin-routes)
    (admin-integration-data-routes)
    (admin-workspace-routes)
    (admin-user-info-routes)
    (route/not-found (service/unrecognized-path-response))))

(defn unsecured-routes
  []
  (util/flagged-routes
    (token-routes)
    (unsecured-misc-routes)
    (unsecured-notification-routes)))

(def admin-handler
  (middleware
   [authenticate-current-user
    wrap-user-info
    validate-current-user
    wrap-logging]
   (admin-routes)))

(def secured-routes-handler
  (middleware
   [authenticate-current-user
    wrap-user-info
    wrap-logging]
   (secured-routes)))

(def secured-routes-no-context-handler
  (middleware
   [authenticate-current-user
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
    secured-routes-no-context-handler))

(defn- site-handler
  [routes-fn]
  (-> (routes-fn)
      (wrap-context-path-remover "/terrain")
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
   {:ui       config/docs-uri
    :data     {:info                {:title       "Discovery Environment API"
                                     :description "Documentation for the Discovery Environment REST API"
                                     :version     "0.1.0"}
               :tags                [{:name "admin", :description "General Admin Endpoints"}
                                     {:name "coge", :description "CoGe Endpoints"}
                                     {:name "token", :description "OAuth Tokens"}]
               :securityDefinitions security-definitions}})
  (middleware
   [[wrap-context-path-remover "/terrain"]
    wrap-keyword-params
    wrap-lcase-params
    wrap-query-params
    clean-context]
   (terrain-routes)))
