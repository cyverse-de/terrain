(ns terrain.services.requests
  (:use [kameleon.uuids :only [uuid]]))

(defn list-vice-requests
  [current-user]
  {:requests []})

(defn submit-vice-request
  [current-user details]
  {:id              (uuid)
   :request_type    "vice"
   :requesting_user (:shortUsername current-user)
   :details         details})
