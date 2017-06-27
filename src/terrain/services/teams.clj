(ns terrain.services.teams
  (:require [terrain.clients.iplant-groups :as ipg]
            [terrain.clients.permissions :as perms-client]))

(defn get-teams [{user :shortUsername} params]
  (ipg/get-teams user (select-keys params [:search :creator :member])))

(defn add-team [{user :shortUsername} body]
  (ipg/add-team user body))

(defn get-team [{user :shortUsername} name]
  (ipg/get-team user name))

(defn update-team [{user :shortUsername} name body]
  (ipg/update-team user name body))

(defn delete-team [{user :shortUsername} name]
  (ipg/delete-team user name))

(defn get-team-members [{user :shortUsername} name]
  (ipg/get-team-members user name))

(defn add-team-members [{user :shortUsername} name {:keys [members]}]
  (ipg/add-team-members user name members))

(defn remove-team-members [{user :shortUsername} name {:keys [members]}]
  (ipg/remove-team-members user name members))

(defn list-team-privileges [{user :shortUsername} name]
  (ipg/list-team-privileges user name))

(defn update-team-privileges [{user :shortUsername} name updates]
  (ipg/update-team-privileges user name updates))

(defn join [{user :shortUsername} name]
  (ipg/join-team user name))

(defn leave [{user :shortUsername} name]
  (ipg/leave-team user name))
