(ns terrain.clients.permissions
  (:require [permissions-client.core :as c]
            [slingshot.slingshot :refer [try+]]
            [terrain.util.config :as config]))

(defn- get-client []
  (c/new-permissions-client (config/permissions-base)))

(defn delete-group-subject
  "Removes the subject associated with a group from the permissions service. This will transitively delete all
   permissions for the group if any permissions have been assigned to that group. If permissions have not been
   assigned to that group then chances are that the group has not been defined as a subject in the permissions
   service, so we ignore 404 status codes."
  [external-id]
  (try+
   (c/delete-subject (get-client) external-id "group")
   (catch [:status 404] _))
  nil)

(defn copy-permissions
  "Copies permissions from one subject to one or more other subjects."
  [source-type source-id dest-subjects]
  (c/copy-permissions (get-client) source-type source-id dest-subjects))
