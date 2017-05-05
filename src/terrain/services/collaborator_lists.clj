(ns terrain.services.collaborator-lists
  (:require [terrain.clients.iplant-groups :as ipg]))

(defn get-collaborator-lists [{user :shortUsername} {:keys [search]}]
  (if-not search
    (ipg/get-collaborator-lists user)
    (ipg/get-collaborator-lists user search)))

(defn add-collaborator-list [{user :shortUsername} body]
  (ipg/add-collaborator-list user body))
