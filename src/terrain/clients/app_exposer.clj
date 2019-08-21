(ns terrain.clients.app-exposer
  (:use [kameleon.uuids :only [uuidify]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as client]
            [terrain.util.config :as config]
            [terrain.clients.apps.raw :as apps]
            [ring.util.io :as ring-io]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))


(defn- app-exposer-url
  [& components]
  (str (apply curl/url (config/app-exposer-base-uri) components)))

(defn- list-pods-by-external-id
  [external-id]
  (let [pods-map (:body (client/get (app-exposer-url "vice" external-id "pods") {:as :json}))]
    {:pods (map #(assoc %1 :external_id external-id) (:pods pods-map))}))

(defn- analysis-steps
  "Returns the steps for the analysis"
  [analysis-id]
  (:steps (apps/list-job-steps (uuidify analysis-id))))

(defn- analysis-external-ids
  "Extracts a list of external IDs from the steps associated with the analysis"
  [analysis-id]
  (map :external_id (analysis-steps analysis-id)))

(defn list-analysis-pods
  "Returns a list of pods associated with the analysis"
  [analysis-id]
  (let [external-ids (analysis-external-ids analysis-id)]
    {:pods (apply concat (map :pods (map list-pods-by-external-id external-ids)))}))
