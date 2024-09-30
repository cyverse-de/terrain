(ns terrain.services.filesystem.icat
  (:require [clojure.tools.logging :as log]
            [clj-icat-direct.icat :refer [icat-db-spec setup-icat]]
            [clj-jargon.init :as init]
            [clj-jargon.metadata :as meta]
            [terrain.util.config :as cfg])
  (:import [clojure.lang IPersistentMap]
           [java.util UUID]))

;; Declarations to eliminate lint warnings for bindings in non-standard macros.
(declare fs)

(def jargon-cfg
  (memoize #(init/init (cfg/irods-host)
                       (cfg/irods-port)
                       (cfg/irods-user)
                       (cfg/irods-pass)
                       (cfg/irods-home)
                       (cfg/irods-zone)
                       (cfg/irods-resc)
               :max-retries (cfg/irods-max-retries)
               :retry-sleep (cfg/irods-retry-sleep)
               :use-trash   (cfg/irods-use-trash))))


(defn- spec
  []
  (icat-db-spec
    (cfg/icat-host)
    (cfg/icat-user)
    (cfg/icat-password)
    :port (cfg/icat-port)
    :db   (cfg/icat-db)))

(defn configure-icat
  "Configures the connection pool to the ICAT database."
  []
  (log/warn "[ICAT] set up ICAT connection.")
  (setup-icat (spec)))


(defn resolve-data-type
  "Given filesystem id, it returns the type of data item it is, file or folder.

   Parameters:
     fs       - (optional) An open jargon context
     data-id  - The UUID of the data item to inspect

   Returns:
     The type of the data item, `file` or `folder`"
  (^String [^IPersistentMap fs ^UUID data-id]
   (if (empty? (meta/list-collections-with-attr-value fs "ipc_UUID" data-id)) "file" "folder"))

  (^String [^UUID data-id]
   (init/with-jargon (jargon-cfg) [fs]
     (resolve-data-type fs data-id))))
