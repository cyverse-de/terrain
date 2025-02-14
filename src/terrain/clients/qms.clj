(ns terrain.clients.qms
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer  [throw+ try+]]
            [terrain.clients.util :refer [with-trap]]
            [terrain.util.config :as config]))

(defn- extract-qms-error
  [body]
  (let [body (if (string? body) body (slurp body))]
    (try+
     (:error (json/parse-string body true) body)
     (catch Object _
       body))))

(defn- default-error-handler
  [error-code {:keys [body] :as response}]
  (log/error "QMS request failed:" response)
  (let [error (extract-qms-error body)]
    (throw+ {:error_code error-code
             :reason     error})))

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
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "usages" username])
        (http/get {:as :json})
        (:body))))

(defn add-usage
  [usage]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "usages"])
        (http/post {:form-params  usage
                    :as           :json
                    :content-type :json})
        (:body))))

(defn add-subscriptions
  [params body]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "subscriptions"] params)
        (http/post {:form-params  body
                    :as           :json
                    :content-type :json})
        (:body))))

(defn list-subscriptions
  [params]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "subscriptions"] params)
        (http/get {:as :json})
        (:body))))

(defn list-user-subscriptions
  [username params]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "users" username "subscriptions"] params)
        (http/get {:as :json})
        (:body))))

(defn update-subscription-quota
  [username resource-type body]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "users" username "plan" resource-type "quota"])
        (http/post {:form-params  body
                    :as           :json
                    :content-type :json})
        (:body))))

(defn update-subscription
  [username plan-name params]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "users" username plan-name] params)
        (http/put {:as :json})
        (:body))))

;;; Non-admin
(defn subscription
  [username]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "users" username "plan"])
        (http/get {:as :json})
        (:body))))

(defn list-all-plans
  []
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "plans"])
        (http/get {:as :json})
        (:body))))

(defn single-plan
  [plan-id]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "plans" plan-id])
        (http/get {:as :json})
        (:body))))

(defn list-resource-types
  []
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "resource-types"])
        (http/get {:as :json})
        (:body))))
