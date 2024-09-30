(ns terrain.services.coge
  (:require [clojure-commons.file-utils :as ft]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.clients.coge :as coge]
            [terrain.clients.data-info :as data]
            [terrain.util.config :as config]))

(defn- share-paths
  "Shares the given paths with the CoGe user so the genome viewer service can access them."
  [sharer paths]
  (let [share-withs [(config/coge-user)]
        perms       "write"]
    (data/share sharer share-withs paths perms)))

(defn- coge-data-folder-path
  "Determines the path to the user's CoGe data folder."
  [username]
  (ft/path-join (data/user-home-folder username) (config/coge-data-folder-name)))

(defn- create-coge-data-folder
  "Creates the CoGe data folder in the user's home directory if it doesn't exist already."
  [username data-folder-path]
  (data/ensure-dir-created username data-folder-path)
  (share-paths username [data-folder-path]))

(defn export-fasta
  "Exports the FastA file corresponding to a Genome into the user's CoGe data folder."
  [genome-id {:keys [notify overwrite] :or {notify false overwrite false}}]
  (let [data-folder-path (coge-data-folder-path (:shortUsername current-user))]
    (create-coge-data-folder (:shortUsername current-user) data-folder-path)
    (coge/export-fasta genome-id
                       {:notify      notify
                        :overwrite   overwrite
                        :destination data-folder-path})))

(defn get-genome-viewer-url
  "Retrieves a genome viewer URL by sharing the given paths and sending a request to the CoGe
   service."
  [{:keys [paths]}]
  (share-paths (:shortUsername current-user) paths)
  {:coge_genome_url (:site_url (coge/get-genome-viewer-url paths))})

(defn search-genomes
  "Searches for genomes in CoGe."
  [{:keys [search]}]
  (coge/search-genomes search))
