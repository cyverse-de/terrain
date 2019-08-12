(ns terrain.clients.app-exposer
  (:use [kameleon.uuids :only [uuidify]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as client]
            [terrain.util.config :as config]
            [terrain.clients.apps.raw :as apps]
            [clojure.tools.logging :as log]))


(defn- app-exposer-url
  [& components]
  (str (apply curl/url (config/app-exposer-base-uri) components)))

(defn- list-pods-by-external-id
  [external-id]
  (:body (client/get (app-exposer-url "vice" external-id "pods") {:as :json})))

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

(defn- external-id->podmap
  "Given an external-id, return a map associating the pod names with the external-id.
   Format {'pod-name' external-id}"
  [external-id]
  (let [pods (list-pods-by-external-id external-id)]
    (into {} (map (fn [p] (assoc {} (:name p) external-id)) (:pods pods)))))

(defn analysis-pod-logs
  [analysis-id pod-name params]
  (let [pods-index  (into {} (map external-id->podmap (analysis-external-ids analysis-id)))]
    (log/warn pods-index)
    (when-let [extid (get pods-index pod-name)]
      (log/warn analysis-id extid pod-name)
      (:body
       (client/get (app-exposer-url "vice" extid "pods" pod-name "logs")
                   {:query-params params})))))
