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
