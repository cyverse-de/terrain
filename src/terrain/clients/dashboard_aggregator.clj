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
  (let [resp (http/get (dashboard-aggregator-url username) {:throw-exceptions false})]
    (cond
      (not (<= 200 (:status resp) 299))
      (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg (:body resp)})
      
      :else
      (json/parse-string (:body resp) true))))