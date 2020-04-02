(ns terrain.services.requests
  (:require [terrain.clients.requests :as rc]))

;; Request type constants
(def vice-request-type "vice")

(defn list-vice-requests
  [{username :shortUsername}]
  (rc/list-requests {:request-type    vice-request-type
                     :requesting-user username}))

(defn- add-user-to-request-details
  [{name :commonName :keys [email]} details]
  (assoc details
         :name name
         :email email))

(defn submit-vice-request
  [{username :shortUsername :as user} details]
  (rc/submit-request vice-request-type username (add-user-to-request-details user details)))
