(ns terrain.clients.dashboard-aggregator
    (:use [clojure-commons.error-codes]
          [slingshot.slingshot :only [try+ throw+]])
    (:require [clj-http.client :as http]
              [cemerick.url :refer [url]]
              [cheshire.core :as json]
              [clojure.tools.logging :as log]
              [terrain.util.config :as config]))

(defn- dashboard-aggregator-url
  [user]
  (str (url (config/dashboard-aggregator-url) "users" user)))

(defn get-dashboard-data
  [username]
  (:body (http/get (dashboard-aggregator-url username) {:as :json})))