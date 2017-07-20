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

(defn- perms-subject-for [{source-id :source_id subject-id :subject_id}]
  {:subject_type (if (= source-id "g:gsa") "group" "user")
   :subject_id   subject-id})

(defn- copy-collaborator-list-permissions [user group-id {:keys [results]}]
  (when-let [subjects (seq (for [result results :when (:success result)] (perms-subject-for result)))]
    (perms-client/copy-permissions "group" group-id subjects)))

(defn remove-collaborator-list-members
  [{user :shortUsername} name {:keys [members]} {retain-permissions? :retain-permissions}]
  (let [group-id (:id (ipg/get-collaborator-list user name))
        results  (ipg/remove-collaborator-list-members user name members)]
    (when (Boolean/parseBoolean retain-permissions?)
      (copy-collaborator-list-permissions user group-id results))
    results))
