(ns terrain.services.collaborator-lists
  (:require [terrain.clients.iplant-groups :as ipg]
            [terrain.clients.permissions :as perms-client]))

(defn get-collaborator-lists [{user :shortUsername} {:keys [search]}]
  (if-not search
    (ipg/get-collaborator-lists user)
    (ipg/get-collaborator-lists user search)))

(defn add-collaborator-list [{user :shortUsername} body]
  (ipg/add-collaborator-list user body))

(defn get-collaborator-list [{user :shortUsername} name]
  (ipg/get-collaborator-list user name))

(defn update-collaborator-list [{user :shortUsername} name body]
  (ipg/update-collaborator-list user name body))

(defn delete-collaborator-list [{user :shortUsername} name]
  (let [{:keys [id] :as list} (ipg/delete-collaborator-list user name)]
    (when id (perms-client/delete-group-subject id))
    list))

(defn get-collaborator-list-members [{user :shortUsername} name]
  (ipg/get-collaborator-list-members user name))

(defn add-collaborator-list-members [{user :shortUsername} name {:keys [members]}]
  (ipg/add-collaborator-list-members user name members))

(defn remove-collaborator-list-members [{user :shortUsername} name {:keys [members]}]
  (ipg/remove-collaborator-list-members user name members))
