(ns terrain.clients.user-info
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [cheshire.core :as json]
            [clojure-commons.error-codes :as ce]
            [slingshot.slingshot :refer [throw+]]
            [terrain.util.config :as config]))

(defn- user-info-url
  [& components]
  (str (apply url (config/user-info-base-url) components)))

;; XXX error handling
(defn list-all-alerts
  []
  (let [resp (http/get (user-info-url "alerts" "all"))]
    (json/parse-string (:body resp) true)))

(defn list-active-alerts
  []
  (let [resp (http/get (user-info-url "alerts" "active"))]
    (json/parse-string (:body resp) true)))

(defn add-alert
  [some-args] ; start/end/alert text I guess, start being optional
  nil)

(defn delete-alert
  [some-args] ; only uses end date & alert
  nil)
