(ns terrain.services.qms
  (:require [terrain.clients.qms :as qms]))

(defn add-subscriptions
  "Validates usernames in the request body before forwarding the requests to QMS to create the subscriptions. Only
  requests with valid usernames will be forwarded to QMS to create the subscriptions."
  [params body]
  (qms/add-subscriptions params body))
