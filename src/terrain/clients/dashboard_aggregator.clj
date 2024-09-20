(ns terrain.clients.dashboard-aggregator
  (:require [clj-http.client :as http]
              [cemerick.url :refer [url]]
              [terrain.util.config :as config]))

(defn- dashboard-aggregator-url
  ([limit]
    (dashboard-aggregator-url [] limit))

  ([components limit]
    (-> (apply url (config/dashboard-aggregator-url) components)
        (assoc :query {:limit limit})
        (str))))

(defn get-dashboard-data
  ([username {:keys [limit] :or {limit 8}}]
    (:body (http/get (dashboard-aggregator-url  ["users" username] limit) {:as :json})))

  ([{:keys [limit] :or {limit 8}}]
    (:body (http/get (dashboard-aggregator-url limit) {:as :json}))))
