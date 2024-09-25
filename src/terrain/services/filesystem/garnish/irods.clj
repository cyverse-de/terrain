(ns terrain.services.filesystem.garnish.irods
  (:require [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :refer [exists?]]
            [clj-jargon.metadata :refer [get-attribute]]
            [clj-jargon.permissions :refer [is-readable?]]
            [clj-jargon.users :refer [user-exists?]]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [slingshot.slingshot :refer [throw+]]
            [terrain.util.config :as cfg]
            [terrain.services.filesystem.icat :as icat]))

;; Declarations to elimiinate lint warnings for irods context map bindings.
(declare cm)

(defn get-types
  "Gets all of the filetypes associated with path."
  ([cm user path]
   (log/info "in get-types")

   (when-not (exists? cm path)
     (throw+ {:error_code ce/ERR_DOES_NOT_EXIST
              :path path}))

   (when-not (user-exists? cm user)
     (throw+ {:error_code ce/ERR_NOT_A_USER
              :user user}))

   (when-not (is-readable? cm user path)
     (throw+ {:error_code ce/ERR_NOT_READABLE
              :user user
              :path path}))
   (let [path-types (get-attribute cm path (cfg/garnish-type-attribute))]
     (log/info "Retrieved types " path-types " from " path " for " user ".")
     (:value (first path-types) "")))

  ([user path]
   (with-jargon (icat/jargon-cfg) [cm]
     (get-types cm user path))))
