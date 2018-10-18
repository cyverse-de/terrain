(ns terrain.services.teams
  (:require [terrain.clients.iplant-groups :as ipg]
            [terrain.clients.permissions :as perms-client]
            [terrain.clients.notifications :as cn]
            [terrain.util.service :as service]))

(defn get-teams [{user :shortUsername} params]
  (ipg/get-teams user (select-keys params [:search :creator :member])))

(defn add-team [{user :shortUsername} body]
  (ipg/add-team user body))

(defn get-team [{user :shortUsername} name]
  (ipg/get-team user name))

(defn update-team [{user :shortUsername} name body]
  (ipg/update-team user name body))

(defn delete-team [{user :shortUsername} name]
  (let [{:keys [id] :as result} (ipg/delete-team user name)]
    (when id (perms-client/delete-group-subject id))
    result))

(defn get-team-members [{user :shortUsername} name]
  (ipg/get-team-members user name))

(defn add-team-members [{user :shortUsername} name {:keys [members]}]
  (let [response (ipg/add-team-members user name members)]
    (doseq [member members]
      (let [member-info (ipg/lookup-subject user member)]
        (when-not (= (:source_id member-info) "g:gsa")
          (cn/send-team-add-notification member-info name))))
    response))

(defn remove-team-members [{user :shortUsername} name {:keys [members]}]
  (ipg/remove-team-members user name members))

(defn list-team-privileges [{user :shortUsername} name]
  (ipg/list-team-privileges user name))

(defn update-team-privileges [{user :shortUsername} name updates]
  (ipg/update-team-privileges user name updates))

(defn join [{user :shortUsername} name]
  (ipg/join-team user name))

(defn join-request [{user :shortUsername user-name :commonName email :email} name message]
  (let [admin (-> (ipg/get-team-admins user name)
                  :members
                  first)]
    (cn/send-team-join-notification user user-name email name admin message)))

(defn deny-join-request [{user :shortUsername} name requester message]
  (ipg/verify-team-exists user name)
  (if-let [email (:email (ipg/lookup-subject user requester))]
    (cn/send-team-join-denial requester email name message)
    (service/not-found "user" requester)))

(defn leave [{user :shortUsername} name]
  (ipg/leave-team user name))
