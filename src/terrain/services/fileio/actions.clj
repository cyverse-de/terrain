(ns terrain.services.fileio.actions
  (:require [cemerick.url :as url]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+]]
            [terrain.clients.data-info :as data-info]
            [terrain.services.filesystem.updown :as updown]
            [terrain.services.metadata.internal-jobs :as internal-jobs]))

(defn- url-encoded?
  [string-to-check]
  (re-seq #"\%[A-Fa-f0-9]{2}" string-to-check))

(defn- validate-import-target
  "Checks that a URL import can land where it was asked to: the destination has to be writeable by
   the user, and nothing can already be sitting at the name being imported."
  [user dest-path filename]
  (let [dest-stat (data-info/stat-by-path user dest-path)
        dest-file (ft/path-join dest-path filename)]
    (when-not (contains? #{"write" "own"} (:permission dest-stat))
      (throw+ {:error_code ce/ERR_NOT_WRITEABLE :user user :path dest-path}))
    (when (data-info/path-exists? user dest-file)
      (throw+ {:error_code ce/ERR_EXISTS :path dest-file}))))

(defn urlimport
  "Submits a URL import job for execution.

   Parameters:
     user - string containing the username of the user that requested the import.
     address - string containing the URL of the file to be imported.
     filename - the filename of the file being imported.
     dest-path - irods path indicating the directory the file should go in."
  [user address filename dest-path]
  (let [filename  (if (url-encoded? filename) (url/url-decode filename) filename)
        dest-path (ft/rm-last-slash dest-path)]
    (validate-import-target user dest-path filename)
    (internal-jobs/submit :url-import [address filename dest-path])
    {:msg   "Upload scheduled."
     :url   address
     :label filename
     :dest  dest-path}))

(defn download
  "Returns a response map filled out with info that lets the client download
   a file.

   Forcibly set Content-Type to application/octet-stream to ensure the file
   is downloaded rather than displayed."
  [user file-path]
  (log/debug "In download.")
  (let [resp (updown/download-file-as-stream user file-path true)]
    (assoc-in resp [:headers "Content-Type"] "application/octet-stream")))
