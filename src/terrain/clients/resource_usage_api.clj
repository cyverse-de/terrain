(ns terrain.clients.resource-usage-api
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- resource-usage-api
  ([components]
   (resource-usage-api components {}))
  ([components query]
   (-> (apply curl/url (config/resource-usage-api-uri) components)
       (assoc :query query)
       str)))

(defn current-cpu-hours-total
  ([]
   (-> (resource-usage-api ["admin" "cpu" "totals"])
       (http/get {:as :json})
       (:body)))
  ([username]
   (-> (resource-usage-api [username "cpu" "total"])
       (http/get {:as :json})
       (:body))))

(defn all-cpu-hours-totals
  ([]
   (-> (resource-usage-api ["admin" "cpu" "totals" "all"])
       (http/get {:as :json})
       (:body)))
  ([username]
   (-> (resource-usage-api [username "cpu" "total" "all"])
       (http/get {:as :json})
       (:body))))

(defn add-cpu-hours
  [username hours]
  (-> (resource-usage-api [username "cpu" "update" "add" hours])
      (http/post  {:as :json})
      (:body)))

(defn subtract-cpu-hours
  [username hours]
  (-> (resource-usage-api [username "cpu" "update" "subtract" hours])
      (http/post  {:as :json})
      (:body)))

(defn reset-cpu-hours
  [username hours]
  (-> (resource-usage-api [username "cpu" "update" "reset" hours])
      (http/post  {:as :json})
      (:body)))

(defn list-workers
  []
  (-> (resource-usage-api ["admin" "workers"])
      (http/get  {:as :json})
      (:body)))

(defn worker
  ([worker-id]
   (-> (resource-usage-api ["admin" "workers" worker-id])
       (http/get  {:as :json})
       (:body)))
  ([worker-id worker-info]
   (-> (resource-usage-api ["admin" "workers" worker-id])
       (http/post {:content-type :json
                   :as :json
                   :form-params worker-info})
       (:body))))

(defn delete-worker
  [worker-id]
  (-> (resource-usage-api ["admin" "workers" worker-id])
      (http/delete)
      (:body)))

(defn list-events
  ([]
   (-> (resource-usage-api ["admin" "cpu" "events"])
       (http/get {:as :json})
       (:body)))
  ([username]
   (-> (resource-usage-api ["admin" "cpu" "events" "user" username])
       (http/get {:as :json})
       (:body))))

(defn event
  ([event-id]
   (-> (resource-usage-api ["admin" "cpu" "events" event-id])
       (http/get {:as :json})
       (:body)))

  ([event-id update-info]
   (-> (resource-usage-api ["admin" "cpu" "events" event-id])
       (http/post {:content-type :json
                   :as :json
                   :form-params update-info})
       (:body))))

(defn delete-event
  [event-id]
  (-> (resource-usage-api ["admin" "cpu" "events" event-id])
      (http/delete)
      (:body)))