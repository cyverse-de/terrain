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

(defn- external-id->podmap
  "Given an external-id, return a map associating the pod names with the external-id.
   Format {'pod-name' external-id}"
  [external-id]
  (let [pods (list-pods-by-external-id external-id)]
    (into {} (map (fn [p] (assoc {} (:name p) external-id)) (:pods pods)))))

(defn- copy [in out]
  (let [buffer (byte-array 4096)]
    (loop []
      (let [size (.read in buffer 0 4096)]
        (when-not (neg? size)
          (.write out buffer 0 size)
          (.flush out)
          (recur))))))

(defn analysis-pod-logs
  [analysis-id pod-name params]
  (let [pods-index (into {} (map external-id->podmap (analysis-external-ids analysis-id)))]
    (when-let [extid (get pods-index pod-name)]
      (let [response-stream (:body (client/get (app-exposer-url "vice" extid "pods" pod-name "logs")
                                               {:query-params params
                                                :as           :stream}))]
        {:headers {"Content-Type" "text/plain"
                   "Transfer-Encoding" "chunked"}
         :body
          (ring-io/piped-input-stream #(copy response-stream %))}))))
