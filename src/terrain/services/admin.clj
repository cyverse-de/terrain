(ns terrain.services.admin
  (:require [clojure.tools.logging :as log]
            [cemerick.url :as url]
            [clojure-commons.error-codes :as ce]
            [terrain.util.config :as config]
            [clj-http.client :as client]
            [terrain.clients.data-info :as data]))


(defn config
  "Returns JSON containing Terrain's configuration, passwords filtered out."
  []
  (config/masked-config))


(defn- check-irods?
  "Returns true if the iRODS settings should be checked."
  []
  (or (config/data-routes-enabled)
      (config/filesystem-routes-enabled)
      (config/fileio-routes-enabled)))

(defn check-jex?
  "Returns true if the JEX settings should be checked."
  []
  (config/app-routes-enabled))

(defn check-apps?
  "Returns true if the apps settings should be checked."
  []
  (config/app-routes-enabled))

(defn check-notificationagent?
  "Returns true if the notification agent settings should be checked."
  []
  (config/notification-routes-enabled))

(defn scrub-url
  [url-to-scrub]
  (str (url/url url-to-scrub :path "/")))

(defn get-with-timeout
  [url]
  (client/get url {:socket-timeout 10000 :conn-timeout 10000}))

(defn perform-jex-check
  []
  (try
    (let [s (:status (get-with-timeout (config/jex-base-url)))]
      (log/info "HTTP Status from JEX: " s)
      (<= 200 s 299))
    (catch Exception e
      (log/error "Error performing JEX status check:")
      (log/error (ce/format-exception e))
      false)))

(defn perform-apps-check
  []
  (try
    (let [base-url (scrub-url (config/apps-base-url))
          s        (:status (get-with-timeout base-url))]
      (log/info "HTTP Status from Apps: " s)
      (<= 200 s 299))
    (catch Exception e
      (log/error "Error performing Apps status check:")
      (log/error (ce/format-exception e))
      false)))

(defn perform-notificationagent-check
  []
  (try
    (let [base-url (scrub-url (config/notifications-base-url))
          s        (:status (get-with-timeout base-url))]
      (log/info "HTTP Status from NotificationAgent: " s)
      (<= 200 s 299))
    (catch Exception e
      (log/error "Error performing NotificationAgent status check:")
      (log/error (ce/format-exception e))
      false)))

(defn- perform-ezid-check
  []
  (try
    (let [ezid-status-url (str (url/url (config/ezid-base-url) "status"))
          ezid-status     (:body (get-with-timeout ezid-status-url))]
      (log/info "HTTP Status from EZID: " ezid-status)
      ezid-status)
    (catch Exception e
      (log/error "Error performing EZID status check:")
      (log/error (ce/format-exception e))
      false)))


(defn- status-irods
  [overall-status]
  (if (check-irods?)
    (merge overall-status {:iRODS (data/irods-running?)})
    overall-status))

(defn status-jex
  [overall-status]
  (if (check-jex?)
    (merge overall-status {:jex (perform-jex-check)})
    overall-status))

(defn status-apps
  [overall-status]
  (if (check-apps?)
    (merge overall-status {:apps (perform-apps-check)})
    overall-status))

(defn status-notificationagent
  [overall-status]
  (if (check-notificationagent?)
    (merge overall-status {:notificationagent (perform-notificationagent-check)})
    overall-status))

(defn- status-ezid
  [overall-status]
  (merge overall-status {:ezid (perform-ezid-check)}))

(defn status
  "Returns JSON containing the Terrain's status."
  []
  (-> {}
    (status-irods)
    (status-jex)
    (status-apps)
    (status-notificationagent)
    (status-ezid)))
