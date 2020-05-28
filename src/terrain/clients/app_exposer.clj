(ns terrain.clients.app-exposer
  (:use [kameleon.uuids :only [uuidify]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as client]
            [terrain.util.config :as config]
            [terrain.clients.apps.raw :as apps]
            [ring.util.io :as ring-io]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [terrain.auth.user-attributes :refer [current-user]]))


(defn- augment-query
  [query {:keys [no-user]}]
  (as-> query q
    (if no-user q (assoc q :user (:shortUsername current-user)))))

(defn- app-exposer-url
  ([components]
    (app-exposer-url components {}))
  ([components query & {:as opts}]
    (-> (apply curl/url (config/app-exposer-base-uri) components)
        (assoc :query (augment-query query opts))
        str)))

(defn get-pod-logs
  "Returns the logs for a pod"
  [analysis-id query]
  (:body (client/get (app-exposer-url ["vice" analysis-id "logs"] query) {:as :json})))

(defn get-time-limit
  "Returns the time limit for a VICE analysis"
  [analysis-id]
  (:body (client/get (app-exposer-url ["vice" analysis-id "time-limit"]) {:as :json})))

(defn set-time-limit
  "Calls the endpoint that adds two days to the time limit for a VICE analysis"
  [analysis-id]
  (:body (client/post (app-exposer-url ["vice" analysis-id "time-limit"]) {:as :json})))

(defn get-resources
  "Calls app-exposer's GET /vice/listing endpoint, with filter as the query filter map."
  [filter]
  (:body (client/get (app-exposer-url ["vice" "listing"] filter :no-user true) {:as :json})))

(defn cancel-analysis
  "Calls app-exposer's POST /vice/{id}/save-and-exit endpoint"
  [analysis-id]
  (:body (client/post (app-exposer-url ["vice", analysis-id, "save-and-exit"]) {:as :json})))

(defn readiness
  "Calls app-exposer's GET /vice/{subdomain}/url-ready endpoint"
  [subdomain]
  (:body (client/get (app-exposer-url ["vice", subdomain, "url-ready"]) {:as :json})))
