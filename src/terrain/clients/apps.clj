(ns terrain.clients.apps
  (:require [terrain.clients.apps.raw :as raw]
            [terrain.util.service :as service]))

(defn admin-list-tool-requests
  [params]
  (->> (raw/admin-list-tool-requests params)
       (:body)
       (service/decode-json)))

(defn get-authenticated-user
  []
  (-> (raw/get-authenticated-user)
      (:body)
      (service/decode-json)))
