(ns terrain.services.communities
  (:require [terrain.clients.iplant-groups :as ipg]
            [terrain.clients.permissions :as perms-client]
            [terrain.clients.notifications :as cn]))

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
  (let [response (ipg/add-community-admins user name members)]
    (doseq [member members]
      (let [member-info (ipg/lookup-subject user member)]
        (when-not (= (:source_id member-info) "g:gsa")
          (cn/send-community-admin-add-notification member-info name))))
    response))

(defn remove-community-admins [{user :shortUsername} name {:keys [members]}]
  (ipg/remove-community-admins user name members))
