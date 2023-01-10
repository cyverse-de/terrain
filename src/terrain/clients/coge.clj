(ns terrain.clients.coge
  (:use [clojure-commons.core :only [remove-nil-values]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.clients.util :only [with-trap]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [terrain.util.config :as config]
            [terrain.util.jwt :as jwt]))

(defn- coge-url
  [& components]
  (str (apply curl/url (config/coge-base-url) components)))

(defn- default-error-handler
  [error-code {:keys [body] :as response}]
  (log/warn "CoGe request failed:" response)
  (throw+ {:error_code error-code
           :reason     (if (string? body) body (slurp body))}))

(defn search-genomes
  "Searches for genomes in CoGe."
  [search-term]
  (with-trap [default-error-handler]
    (:body (http/get (coge-url "genomes" "search" search-term)
                     {:headers (jwt/add-auth-header current-user)
                      :as      :json}))))

(def export-fasta-job-type  "export_fasta")
(def export-fasta-dest-type "irods")

(defn- export-fasta-request
  "Builds the request to export the FastA file for a genome into iRODS."
  [user genome-id {:keys [notify overwrite destination]}]
  (cheshire/encode
   {:type       export-fasta-job-type
    :parameters (remove-nil-values
                 {:genome_id genome-id
                  :dest_type export-fasta-dest-type
                  :overwrite (if overwrite 1 0)
                  :email     (when notify (:email user))})}))

(defn export-fasta
  "Submits a job to CoGe to export the FastA file for a genome into iRODS."
  [genome-id opts]
  (with-trap [default-error-handler]
    (:body (http/put (coge-url "jobs")
                     {:headers      (jwt/add-auth-header current-user)
                      :body         (export-fasta-request current-user genome-id opts)
                      :content-type :json
                      :as           :json}))))

(def test-organism-id 38378)

(defn- genome-viewer-url-request
  "Builds the request to import a list of genomes into CoGe."
  [paths]
  (cheshire/encode
   {:organism_id test-organism-id
    :metadata    {:name        "test"
                  :version     "1"
                  :type_id     1
                  :source_name "test"
                  :restricted  true}
    :source_data (map (partial hash-map :type "irods" :path) paths)}))

(defn get-genome-viewer-url
  "Sends a request for a genome viewer URL to the COGE service."
  [paths]
  (with-trap [default-error-handler]
    (:body (http/put (coge-url "genomes")
                     {:body         (genome-viewer-url-request paths)
                      :headers      (jwt/add-auth-header current-user)
                      :content-type :json
                      :as           :json}))))
