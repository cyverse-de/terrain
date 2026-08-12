(ns terrain.services.subjects
  (:require [terrain.clients.grouping :as ipg]))

(defn find-subjects [{user :shortUsername} {:keys [search]}]
  (ipg/find-subjects user search))
