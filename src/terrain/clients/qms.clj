(ns terrain.clients.qms
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- qms-api
  ([components]
   (qms-api components {}))
  ([components query]
   (-> (apply curl/url (config/qms-api-uri) components)
       (assoc :query query)
       str)))

;;; Admin
(defn get-usages
  [username]
  (-> (qms-api ["v1" "usages" username])
      (http/get {:as :json})
      (:body)))

(defn add-usage
  [usage]
  (-> (qms-api ["v1" "usages"])
      (http/post {:form-params  usage
                  :as           :json
                  :content-type :json})
      (:body)))

(defn update-user-plan
  [username plan-name]
  (-> (qms-api ["v1" "users" username plan-name])
      (http/put {:as :json})
      (:body)))

;;; Non-admin
(defn user-plan
  [username]
  (-> (qms-api ["v1" "users" username "plan"])
      (http/get {:as :json})
      (:body)))

(defn list-all-plans
  []
  (-> (qms-api ["v1" "plans"])
      (http/get {:as :json})
      (:body)))

(defn single-plan
  [plan-id]
  (-> (qms-api ["v1" "plans" plan-id])
      (http/get {:as :json})
      (:body)))
