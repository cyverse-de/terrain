(ns terrain.services.teams
  (:require [terrain.clients.iplant-groups :as ipg]
            [terrain.clients.permissions :as perms-client]))

(defn get-teams [{user :shortUsername} params]
  (ipg/get-teams user (select-keys params [:search :creator :member])))

(defn add-team [{user :shortUsername} body]
  (ipg/add-team user body))
