(ns terrain.services.collaborator-lists
  (:require [terrain.clients.iplant-groups :as ipg]))

(defn get-collaborator-lists [{user :shortUsername} {:keys [search]}]
  (if-not search
    (ipg/get-collaborator-lists user)
    (ipg/get-collaborator-lists user search)))

(defn add-collaborator-list [{user :shortUsername} body]
  (ipg/add-collaborator-list user body))

(defn get-collaborator-list [{user :shortUsername} name]
  (ipg/get-collaborator-list user name))

(defn delete-collaborator-list [{user :shortUsername} name]
  (ipg/delete-collaborator-list user name))

(defn get-collaborator-list-members [{user :shortUsername} name]
  (ipg/get-collaborator-list-members user name))

(defn add-collaborator-list-members [{user :shortUsername} name {:keys [members]}]
  (ipg/add-collaborator-list-members user name members))
