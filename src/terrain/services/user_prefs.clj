(ns terrain.services.user-prefs
  (:use [slingshot.slingshot :only [throw+]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.routes.schemas.user-prefs :only [default-output-dir-key]]
        [terrain.util.service :only [success-response]])
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [terrain.clients.user-prefs :as cup]
            [terrain.services.user-prefs.output-dir :as output-dir]))

(defn- process-outgoing-prefs
  [user prefs]
  (output-dir/process-outgoing user prefs))

(defn- process-incoming-prefs
  [user prefs]
  (output-dir/process-incoming user prefs))

(defn- get-user-prefs
  [user prefs]
  (log/spy prefs)
  (let [processed-prefs (log/spy (process-outgoing-prefs user prefs))]
    (when-not (= prefs processed-prefs)
      (log/debug "updating the preferences for" user)
      (cup/set-prefs user processed-prefs))
    processed-prefs))

(defn- set-user-prefs
  [user prefs]
  (log/spy prefs)
  (cup/set-prefs user (log/spy (process-incoming-prefs user prefs))))

(defn validate-user-prefs
  [prefs]
  (if-not (contains? prefs default-output-dir-key)
    (throw+ {:error_code ce/ERR_BAD_REQUEST
             :msg        (str "Missing " (name default-output-dir-key))})))

(defn user-prefs
  "Retrieves or saves the user's preferences."
  ([user]
     (let [prefs (cup/get-prefs user)]
       (get-user-prefs user prefs)))
  ([user req-prefs-string]
     (let [prefs (if-not (map? req-prefs-string)
                   (cheshire/decode req-prefs-string true)
                   req-prefs-string)]
       (validate-user-prefs prefs)
       (set-user-prefs user prefs))))

(defn remove-prefs
  "Deletes the preferences for the current user."
  []
  (cup/delete-prefs (:username current-user))
  (success-response))

(defn do-get-prefs
  []
  (success-response (user-prefs (:username current-user))))

(defn do-post-prefs
  [body]
  (success-response (user-prefs (:username current-user) body)))
