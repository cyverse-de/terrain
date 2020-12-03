(ns terrain.services.filesystem.stat
  (:use [clojure-commons.validators]
        [clojure.string :as string]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [exists? is-dir? stat]]
        [clj-jargon.item-ops :only [input-stream]]
        [clj-jargon.metadata :only [get-attribute]]
        [clj-jargon.permissions :only [is-writeable? list-user-perms permission-for owns?]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [terrain.services.filesystem.validators :as validators]
            [terrain.services.filesystem.garnish.irods :as filetypes]
            [clj-icat-direct.icat :as icat]
            [terrain.clients.data-info.raw :as data-raw]
            [terrain.util.config :as cfg]
            [terrain.services.filesystem.common-paths :as paths]
            [terrain.services.filesystem.icat :as jargon])
  (:import [clojure.lang IPersistentMap]
           [java.io InputStream]
           [org.apache.tika Tika]))

(defn- count-shares
  [cm user path]
  (let [filter-users (set (conj (cfg/fs-perms-filter) user (cfg/irods-user)))
        full-listing (list-user-perms cm path)]
    (count
     (filterv
      #(not (contains? filter-users (:user %1)))
      full-listing))))

(defn- merge-counts
  [stat-map cm user path]
  (if (is-dir? cm path)
    (merge stat-map {:file-count (icat/number-of-files-in-folder user (cfg/irods-zone) path)
                     :dir-count  (icat/number-of-folders-in-folder user (cfg/irods-zone) path)})
    stat-map))

(defn- merge-shares
  [stat-map cm user path]
  (if (owns? cm user path)
    (merge stat-map {:share-count (count-shares cm user path)})
    stat-map))

(defn- detect-media-type-from-contents
  [^IPersistentMap cm ^String path]
  (with-open [^InputStream istream (input-stream cm path)]
    (.detect (Tika.) istream)))

(defn- detect-content-type
  [^IPersistentMap cm ^String path]
  (let [path-type (.detect (Tika.) (ft/basename path))]
    (if (or (= path-type "application/octet-stream")
            (= path-type "text/plain"))
      (detect-media-type-from-contents cm path)
      path-type)))

(defn- merge-type-info
  [stat-map cm user path]
  (if-not (is-dir? cm path)
    (-> stat-map
        (merge {:infoType (filetypes/get-types cm user path)})
        (merge {:content-type (detect-content-type cm path)}))
    stat-map))

(defn- merge-label
  [stat-map user path]
  (assoc stat-map
         :label (paths/id->label user path)))

(defn get-public-data-user
  "Returns the anonymous user for public data if a user is not provided"
  ([user paths ids]
   (let [paths (if (sequential? paths) paths [paths])
         has-ids-or-private-paths? (or (seq ids) (some #(not (string/starts-with? % (cfg/fs-community-data))) paths))
         request-user (if has-ids-or-private-paths?
                        user
                        (or user "anonymous"))]
     (or request-user (throw+ {:type :clojure-commons.exception/not-authorized
                               :user user}))))
  ([user paths]
   (get-public-data-user user paths nil)))

(defn path-is-dir?
  [path]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/path-exists cm path)
    (is-dir? cm path)))

(defn decorate-stat
  [cm user stat]
  (let [path (:path stat)]
    (-> stat
        (assoc :id         (:value (first (get-attribute cm path "ipc_UUID")))
               :permission (permission-for cm user path))
        (merge-label user path)
        (merge-type-info cm user path)
        (merge-shares cm user path)
        (merge-counts cm user path))))

(defn path-stat
  ([cm user path]
   (let [path (ft/rm-last-slash path)]
     (log/warn "[path-stat] user:" user "path:" path)
     (validators/path-exists cm path)
     (decorate-stat cm user (stat cm path))))

  ([user path]
   (with-jargon (jargon/jargon-cfg) [cm]
     (path-stat cm user path))))

(defn- dir-stack
  "Obtains a stack of parent directories for a directory path."
  [path]
  (take-while (complement nil?) (iterate ft/dirname path)))

(defn- deepest-extant-parent
  "Finds the deepest parent of a path that exists."
  [cm path]
  (first (filter (partial exists? cm) (dir-stack path))))

(defn can-create-dir?
  ([cm user path]
   ((every-pred (partial is-dir? cm) (partial is-writeable? cm user))
    (log/spy :warn (deepest-extant-parent cm path))))
  ([user path]
   (with-jargon (jargon/jargon-cfg) [cm]
     (can-create-dir? cm user path))))

(defn do-stat
  [{user :shortUsername} body]
  (let [paths         (:paths body)
        ids           (:ids body)
        request-user  (get-public-data-user user paths ids)]
    (-> (data-raw/collect-stats request-user body)
        :body
        (json/decode true))))
