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


(defn- app-exposer-url
  ([components]
   (app-exposer-url components {}))
  ([components query]
   (-> (apply curl/url (config/app-exposer-base-uri) components)
       (assoc :query (assoc query :user (:shortUsername current-user)))
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
  (:body (client/get (app-exposer-url ["vice" "listing"] filter))))
