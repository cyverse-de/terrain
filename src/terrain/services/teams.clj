(ns terrain.services.teams
  (:require [terrain.clients.iplant-groups :as ipg]
            [terrain.clients.permissions :as perms-client]))

(defn get-teams [{user :shortUsername} {:keys [search]}]
  (if-not search
    (ipg/get-teams user)
    (ipg/get-teams user search)))


(defn add-team [{user :shortUsername} body]
  (ipg/add-team user body))
