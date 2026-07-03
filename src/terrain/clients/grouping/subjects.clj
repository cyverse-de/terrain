(ns terrain.clients.grouping.subjects
  "Lightweight facade for subject lookups. Kept separate from terrain.clients.grouping so
   that terrain.auth.user-attributes can depend on it without pulling in the legacy
   iplant-groups client, which transitively requires user-attributes and would create a
   circular dependency. Both backends reached from here (terrain.clients.groups and
   terrain.clients.iplant-groups.subjects) are dependency-cycle safe."
  (:require [terrain.clients.groups :as groups]
            [terrain.clients.iplant-groups.subjects :as ipg-subjects]
            [terrain.util.config :as config]))

(defn- new-backend?
  []
  (= (config/groups-backend) "groups"))

(defn- admin-user
  []
  (if (new-backend?)
    (config/groups-admin-user)
    (config/grouper-user)))

(defn lookup-subject
  ([short-username]
   (lookup-subject (admin-user) short-username))
  ([user short-username]
   (if (new-backend?)
     (groups/lookup-subject user short-username)
     (ipg-subjects/lookup-subject user short-username))))

(defn lookup-subjects
  ([subject-ids]
   (lookup-subjects (admin-user) subject-ids))
  ([user subject-ids]
   (if (new-backend?)
     (groups/lookup-subjects user subject-ids)
     (ipg-subjects/lookup-subjects user subject-ids))))
