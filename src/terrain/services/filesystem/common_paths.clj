(ns terrain.services.filesystem.common-paths
  (:require [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [clj-jargon.item-info :as item]
            [terrain.util.config :as cfg]
            [terrain.util.validators :as valid]))


(defn trace-log
  [trace-type func-name namespace params]
  (let [log-ns (str "trace." namespace)
        desc   (str "[" trace-type "][" func-name "]")
        msg    (apply print-str desc params)]
    (log/log log-ns :trace nil msg)))

(defmacro log-call
  [func-name & params]
  `(trace-log "call" ~func-name ~*ns* [~@params]))

(defn log-func*
  [func-name namespace]
  (fn [result]
    (trace-log "result" func-name namespace result)))

(defmacro log-func
  [func-name]
  `(log-func* ~func-name ~*ns*))

(defmacro log-result
  [func-name & result]
  `(trace-log "result" ~func-name ~*ns* [~@result]))

(defn super-user?
  [^String username]
  (.equals username (cfg/irods-user)))

(defn user-home-dir
  [user]
  (ft/path-join "/" (cfg/irods-zone) "home" user))


(defn valid-path? [path-to-check] (valid/good-string? path-to-check))

(defn user-trash-path
  [user]
  (item/trash-base-dir (cfg/irods-zone) user))

(defn in-trash?
  [user ^String fpath]
  (.startsWith fpath (user-trash-path user)))

(defn- dir-equal?
  [path comparison]
  (apply = (map ft/rm-last-slash [path comparison])))

(defn- user-trash-dir?
  [user abs]
  (dir-equal? abs (user-trash-path user)))
(defn- sharing? [abs] (dir-equal? abs (cfg/irods-home)))
(defn- community? [abs] (dir-equal? abs (cfg/fs-community-data)))

(defn id->label
  "Generates a label given a listing ID (read as absolute path)."
  [user id]
  (cond
    (community? id)           "Community Data"
    (nil? user)               (ft/basename id)
    (user-trash-dir? user id) "Trash"
    (sharing? id)             "Shared With Me"
    :else                     (ft/basename id)))
