(ns terrain.clients.dashboard-aggregator
    (:use [clojure-commons.error-codes]
          [slingshot.slingshot :only [try+ throw+]])
    (:require [clj-http.client :as http]
              [cemerick.url :refer [url]]
              [cheshire.core :as json]
              [clojure.tools.logging :as log]
              [terrain.util.config :as config]))

(defn- dashboard-aggregator-url
  [& parts]
  (str (apply url (config/dashboard-aggregator-url) parts)))

(defn get-dashboard-data
  ([username]
    (:body (http/get (dashboard-aggregator-url "users" username) {:as :json})))
  ([]
    (:body (http/get (dashboard-aggregator-url) {:as :json}))))