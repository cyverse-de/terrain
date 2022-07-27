(ns terrain.clients.data-usage-api
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- data-usage-api
  ([components]
   (data-usage-api components {}))
  ([components query]
   (-> (apply curl/url (config/data-usage-api-uri) components)
       (assoc :query query)
       str)))

(defn user-current-usage
  [username]
  (-> (data-usage-api [username "data" "current"])
      (http/get {:as :json})
      (:body)))

(defn user-data-overage?
  [username]
  (-> (data-usage-api [username "data" "overage"])
      (http/get {:as :json})
      (:body)
      (:has_data_overage)))
