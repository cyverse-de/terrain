(ns terrain.services.subjects
  (:require [terrain.clients.iplant-groups :as ipg]))

(defn find-subjects [{user :shortUsername} {:keys [search]}]
  (ipg/find-subjects user search))
