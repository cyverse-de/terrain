(ns terrain.services.fileio.actions
  (:require [cemerick.url :as url]
            [clj-jargon.init :refer [with-jargon]]
            [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [terrain.services.filesystem.icat :as icat]
            [terrain.services.filesystem.validators :as validators]
            [terrain.services.filesystem.updown :as updown]
            [terrain.services.metadata.internal-jobs :as internal-jobs]))

;; Declarations to eliminate lint warnings for the iRODS context map binding.
(declare cm)

(defn- url-encoded?
  [string-to-check]
  (re-seq #"\%[A-Fa-f0-9]{2}" string-to-check))

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
    (with-jargon (icat/jargon-cfg) [cm]
      (validators/user-exists cm user)
      (validators/path-writeable cm user dest-path)
      (validators/path-not-exists cm (ft/path-join dest-path filename)))
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
