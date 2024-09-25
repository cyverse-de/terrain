(ns terrain.services.filesystem.create
  (:require [clojure.tools.logging :as log]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.permissions :refer [set-owner]]
            [terrain.services.filesystem.validators :as validators]
            [terrain.services.filesystem.icat :as cfg]))

;; Declarations to eliminate lint warnings for the iRODS context map binding.
(declare cm)

(defn ensure-created
  "If a folder doesn't exist, it creates the folder and makes the given user an owner of it.

   Parameters:
     user - the username of the user to become an owner of the new folder
     dir  - the absolute path to the folder"
  [^String user ^String dir]
  (with-jargon (cfg/jargon-cfg) [cm]
    (when-not (item/exists? cm dir)
      (validators/user-exists cm user)
      (log/info "creating" dir)
      (ops/mkdirs cm dir)
      (set-owner cm dir user))))
