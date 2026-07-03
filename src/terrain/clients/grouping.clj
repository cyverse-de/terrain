(ns terrain.clients.grouping
  "Facade over the two group backends. Dispatches each operation to either the legacy
   iplant-groups client or the new Groups service client based on the configured backend.
   This namespace exists to support a config-gated cutover and is intended to be removed
   once the migration to the Groups service is complete, at which point callers can depend
   on terrain.clients.groups directly."
  (:require [terrain.clients.groups :as groups]
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
