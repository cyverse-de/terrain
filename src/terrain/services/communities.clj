(ns terrain.services.communities
  (:require [terrain.clients.iplant-groups :as ipg]
            [terrain.clients.permissions :as perms-client]
            [terrain.clients.notifications :as cn]
            [terrain.util.config :as config]))

(defn get-communities [{user :shortUsername} params]
  (ipg/get-communities user (select-keys params [:search :creator :member])))

(defn add-community [{user :shortUsername} body]
  (ipg/add-community user (assoc body :public_privileges ["read"])))

(defn get-community [{user :shortUsername} name]
  (ipg/get-community user name))

(defn update-community [{user :shortUsername} name body]
  (ipg/update-community user name body))

(defn delete-community [{user :shortUsername} name]
  (let [{:keys [id] :as result} (ipg/delete-community user name)]
    (when id (perms-client/delete-group-subject id))
    result))

(defn get-community-admins [{user :shortUsername} name]
  (ipg/get-community-admins user name))

(defn add-community-admins [{user :shortUsername} name {:keys [members]}]
  (let [{:keys [results] :as response} (ipg/add-community-admins user name members)]
    (doseq [{:keys [success subject_id source_id]} results]
      (when (and success (not= source_id "g:gsa"))
        (cn/send-community-admin-add-notification (ipg/lookup-subject user subject_id) name)))
    response))

(defn remove-community-admins [{user :shortUsername} name {:keys [members]}]
  (ipg/remove-community-admins user name members))

(defn admin-get-communities [params]
  (get-communities {:shortUsername (config/grouper-user)} params))

(defn admin-get-community [name]
  (get-community {:shortUsername (config/grouper-user)} name))

(defn admin-update-community [name body]
  (update-community {:shortUsername (config/grouper-user)} name body))

(defn admin-delete-community [name]
  (delete-community {:shortUsername (config/grouper-user)} name))

(defn admin-get-community-admins [name]
  (get-community-admins {:shortUsername (config/grouper-user)} name))

(defn admin-add-community-admins [name params]
  (add-community-admins {:shortUsername (config/grouper-user)} name params))

(defn admin-remove-community-admins [name params]
  (remove-community-admins {:shortUsername (config/grouper-user)} name params))
