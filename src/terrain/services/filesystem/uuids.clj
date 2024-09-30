(ns terrain.services.filesystem.uuids
  (:require [clojure.tools.logging :as log]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.init :as init]
            [clj-jargon.metadata :refer [list-everything-with-attr-value]]
            [clj-jargon.permissions :refer [is-readable?]]
            [clojure-commons.error-codes :as error]
            [slingshot.slingshot :refer [throw+]]
            [terrain.services.filesystem.icat :as jargon]
            [terrain.services.filesystem.stat :as stat]
            [terrain.services.filesystem.validators :refer [user-exists]]
            [terrain.util.config :as cfg])
  (:import [java.util UUID]
           [clojure.lang IPersistentMap ISeq]))

;; Declarations to eliminate lint warnings for bindings in non-standard macros.
(declare cm)

(def uuid-attr "ipc_UUID")


(defn- path-for-uuid
  "Resolves a stat info for the entity with a given UUID.

   Params:
     user - the user requesting the info
     uuid - the UUID

   Returns:
     It returns a path."
  (^IPersistentMap [^IPersistentMap cm ^String _user ^UUID uuid]
   (let [results (list-everything-with-attr-value cm uuid-attr uuid)]
     (when (empty? results)
       (throw+ {:error_code error/ERR_DOES_NOT_EXIST :uuid uuid}))
     (when (> (count results) 1)
       (log/warn "Too many results for" uuid ":" (count results))
       (log/debug "Results for" uuid ":" results)
       (throw+ {:error_code error/ERR_TOO_MANY_RESULTS
                :count      (count results)
                :uuid       uuid}))
     (when (pos? (count results))
       (first results))))
  (^IPersistentMap [^String user ^UUID uuid]
   (init/with-jargon (jargon/jargon-cfg) [cm]
     (path-for-uuid cm user uuid))))

(defn- fmt-stat
  [cm user data-item]
  (let [path (:full_path data-item)]
    (->> {:date-created  (* 1000 (Long/valueOf (:create_ts data-item)))
          :date-modified (* 1000 (Long/valueOf (:modify_ts data-item)))
          :file-size     (:data_size data-item)
          :id            (:uuid data-item)
          :path          path
          :type          (case (:type data-item)
                           "collection" :dir
                           "dataobject" :file)}
      (stat/decorate-stat cm user))))

(defn paths-for-uuids-paged
  "Resolves the stat info for the entities with the given UUIDs. The results are paged.

   Params:
     user       - the user requesting the info
     sort-field - the stat field to sort on
     sort-order - the direction of the sort (asc|desc)
     limit      - the maximum number of results to return
     offset     - the number of results to skip before returning some
     uuids      - the UUIDS of interest
     info-types - This is info types to of the files to return. It may be nil, meaning return all
                  info types, a string containing a single info type, or a sequence containing a set
                  of info types.

   Returns:
     It returns a page of stat info maps."
  [^String  user
   ^String  sort-col
   ^String  sort-order
   ^Integer limit
   ^Integer offset
   ^ISeq    uuids
   ^ISeq    info-types]
  (let [zone (cfg/irods-zone)]
    (init/with-jargon (jargon/jargon-cfg) [cm]
      (user-exists cm user)
      (map (partial fmt-stat cm user)
           (icat/paged-uuid-listing user zone sort-col sort-order limit offset uuids info-types)))))


(defn uuid-accessible?
  "Indicates if a data item is readable by a given user.

   Parameters:
     user     - the authenticated name of the user
     data-id  - the UUID of the data item

   Returns:
     It returns true if the user can access the data item, otherwise false"
  ^Boolean [^String user ^UUID data-id]
  (init/with-jargon (jargon/jargon-cfg) [cm]
    (let [data-path (path-for-uuid cm user (str data-id))]
      (and data-path (is-readable? cm user data-path)))))
