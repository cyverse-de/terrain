(ns terrain.clients.grouping
  "Facade over the two group backends. Dispatches each operation to either the legacy
   iplant-groups client or the new Groups service client based on the configured backend.
   This namespace exists to support a config-gated cutover and is intended to be removed
   once the migration to the Groups service is complete, at which point callers can depend
   on terrain.clients.groups directly."
  (:require [terrain.clients.groups :as groups]
            [terrain.clients.grouping.retag :as retag]
            [terrain.clients.iplant-groups :as ipg]
            [terrain.util.config :as config]))

(defn- new-backend?
  []
  (= (config/groups-backend) "groups"))

;; Subjects.

(defn find-subjects [user search]
  (if (new-backend?)
    (groups/find-subjects user search)
    (ipg/find-subjects user search)))

(defn lookup-subject [user short-username]
  (if (new-backend?)
    (groups/lookup-subject user short-username)
    (ipg/lookup-subject user short-username)))

(defn lookup-subject-add-empty [user short-username]
  (if (new-backend?)
    (groups/lookup-subject-add-empty user short-username)
    (ipg/lookup-subject-add-empty user short-username)))

(defn format-like-trellis [response]
  (if (new-backend?)
    (groups/format-like-trellis response)
    (ipg/format-like-trellis response)))

(defn list-groups-for-user [subject-id details]
  (if (new-backend?)
    (groups/list-groups-for-user subject-id details)
    (ipg/list-groups-for-user subject-id details)))

(defn remove-de-user [subject-id]
  (if (new-backend?)
    (groups/remove-de-user subject-id)
    (ipg/remove-de-user subject-id)))

;; Collaborator lists.

(defn get-collaborator-lists
  ([user details]
   (if (new-backend?)
     (groups/get-collaborator-lists user details)
     (ipg/get-collaborator-lists user details)))
  ([user details search]
   (if (new-backend?)
     (groups/get-collaborator-lists user details search)
     (ipg/get-collaborator-lists user details search))))

(defn add-collaborator-list [user body]
  (if (new-backend?)
    (groups/add-collaborator-list user body)
    (ipg/add-collaborator-list user body)))

(defn get-collaborator-list [user name]
  (if (new-backend?)
    (groups/get-collaborator-list user name)
    (ipg/get-collaborator-list user name)))

(defn update-collaborator-list [user name body]
  (if (new-backend?)
    (groups/update-collaborator-list user name body)
    (ipg/update-collaborator-list user name body)))

(defn delete-collaborator-list [user name]
  (if (new-backend?)
    (groups/delete-collaborator-list user name)
    (ipg/delete-collaborator-list user name)))

(defn get-collaborator-list-members [user name]
  (if (new-backend?)
    (groups/get-collaborator-list-members user name)
    (ipg/get-collaborator-list-members user name)))

(defn add-collaborator-list-members [user name members]
  (if (new-backend?)
    (groups/add-collaborator-list-members user name members)
    (ipg/add-collaborator-list-members user name members)))

(defn remove-collaborator-list-members [user name members]
  (if (new-backend?)
    (groups/remove-collaborator-list-members user name members)
    (ipg/remove-collaborator-list-members user name members)))

;; Teams.

(defn get-teams [user params]
  (if (new-backend?) (groups/get-teams user params) (ipg/get-teams user params)))

(defn add-team [user body]
  (if (new-backend?) (groups/add-team user body) (ipg/add-team user body)))

(defn get-team [user name]
  (if (new-backend?) (groups/get-team user name) (ipg/get-team user name)))

(defn update-team [user name body]
  (if (new-backend?) (groups/update-team user name body) (ipg/update-team user name body)))

(defn delete-team [user name]
  (if (new-backend?) (groups/delete-team user name) (ipg/delete-team user name)))

(defn verify-team-exists [user name]
  (if (new-backend?) (groups/verify-team-exists user name) (ipg/verify-team-exists user name)))

(defn get-team-members [user name]
  (if (new-backend?) (groups/get-team-members user name) (ipg/get-team-members user name)))

(defn add-team-members [user name members]
  (if (new-backend?) (groups/add-team-members user name members) (ipg/add-team-members user name members)))

(defn remove-team-members [user name members]
  (if (new-backend?)
    (groups/remove-team-members user name members)
    (ipg/remove-team-members user name members)))

(defn list-team-privileges [user name]
  (if (new-backend?) (groups/list-team-privileges user name) (ipg/list-team-privileges user name)))

(defn update-team-privileges [user name updates]
  (if (new-backend?)
    (groups/update-team-privileges user name updates)
    (ipg/update-team-privileges user name updates)))

(defn get-team-admins [user name]
  (if (new-backend?) (groups/get-team-admins user name) (ipg/get-team-admins user name)))

(defn join-team [user name]
  (if (new-backend?) (groups/join-team user name) (ipg/join-team user name)))

(defn leave-team [user name]
  (if (new-backend?) (groups/leave-team user name) (ipg/leave-team user name)))

;; Communities.

(defn get-communities [user params]
  (if (new-backend?) (groups/get-communities user params) (ipg/get-communities user params)))

(defn admin-get-communities [user params]
  (if (new-backend?) (groups/admin-get-communities user params) (ipg/admin-get-communities user params)))

(defn add-community [user body]
  (if (new-backend?) (groups/add-community user body) (ipg/add-community user body)))

(defn get-community [user name]
  (if (new-backend?) (groups/get-community user name) (ipg/get-community user name)))

(defn update-community [user name retag-apps? force-rename? body]
  (if (new-backend?)
    (do
      (when-let [new-name (:name body)]
        (when (not= new-name name)
          (retag/check-for-tagged-apps retag-apps? force-rename? name new-name)))
      (groups/update-community user name body))
    (ipg/update-community user name retag-apps? force-rename? body)))

(defn delete-community [user name]
  (if (new-backend?) (groups/delete-community user name) (ipg/delete-community user name)))

(defn get-community-members [user name]
  (if (new-backend?) (groups/get-community-members user name) (ipg/get-community-members user name)))

(defn get-community-admins [user name]
  (if (new-backend?) (groups/get-community-admins user name) (ipg/get-community-admins user name)))

(defn add-community-admins [user name members]
  (if (new-backend?)
    (groups/add-community-admins user name members)
    (ipg/add-community-admins user name members)))

(defn remove-community-admins [user name members]
  (if (new-backend?)
    (groups/remove-community-admins user name members)
    (ipg/remove-community-admins user name members)))

(defn join-community [user name]
  (if (new-backend?) (groups/join-community user name) (ipg/join-community user name)))

(defn leave-community [user name]
  (if (new-backend?) (groups/leave-community user name) (ipg/leave-community user name)))
