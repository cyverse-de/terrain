(ns terrain.clients.user-info
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [cheshire.core :as json]
            [clojure-commons.core :refer [remove-nil-values]]
            [terrain.util.config :as config]))

(defn- user-info-url
  [& components]
  (str (apply url (config/user-info-base-url) components)))

(defn list-all-alerts
  []
  (-> (user-info-url "alerts" "all")
      (http/get {:as :json})
      :body))

(defn list-active-alerts
  []
  (-> (user-info-url "alerts" "active")
      (http/get {:as :json})
      :body))

(defn add-alert
  ([end-date alert-text]
   (add-alert nil end-date alert-text))
  ([start-date end-date alert-text]
   (-> (user-info-url "alerts")
       (http/post {:content-type :json
                   :body         (remove-nil-values
                                   {:start_date start-date
                                    :end_date   end-date
                                    :alert      alert-text})})
       :body)))

(defn delete-alert
  [end-date alert-text]
  (-> (user-info-url "alerts")
      (http/delete {:content-type :json
                    :body         {:end_date   end-date
                                   :alert      alert-text}})
      :body))
