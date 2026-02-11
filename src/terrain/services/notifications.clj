(ns terrain.services.notifications
  "Service layer for notification operations. Wraps the raw notification client
   to decode responses for use with Swagger-documented endpoints."
  (:require
   [cheshire.core :as json]
   [terrain.clients.notifications :as cn]
   [terrain.clients.notifications.raw :as rn]
   [terrain.util.transformers :refer [add-current-user-to-map]]))

(defn- decode-response
  "Decodes the JSON body from a raw HTTP response."
  [response]
  (-> response :body slurp (json/decode true)))

(defn- encode-body
  "Encodes a Clojure map as JSON for request bodies, adding the current user."
  [body]
  (json/encode (add-current-user-to-map body)))

(defn get-messages
  "Lists notifications for the authenticated user."
  [params]
  (decode-response (rn/get-messages params)))

(defn get-unseen-messages
  "Lists unseen notifications for the authenticated user."
  [params]
  (decode-response (rn/get-unseen-messages params)))

(defn last-ten-messages
  "Returns the ten most recent notifications for the authenticated user."
  []
  (cn/last-ten-messages))

(defn count-messages
  "Returns the count of notifications for the authenticated user."
  [params]
  (decode-response (rn/count-messages params)))

(defn delete-notifications
  "Deletes the specified notifications for the authenticated user."
  [body]
  (decode-response (rn/delete-notifications (encode-body body))))

(defn delete-all-notifications
  "Deletes all notifications matching the filter criteria."
  [params]
  (decode-response (rn/delete-all-notifications params)))

(defn mark-notifications-seen
  [body]
  (decode-response (rn/mark-notifications-seen (encode-body body))))

(defn mark-all-notifications-seen
  "Marks all notifications as seen for the authenticated user."
  []
  (decode-response (rn/mark-all-notifications-seen (json/encode (add-current-user-to-map {})))))
