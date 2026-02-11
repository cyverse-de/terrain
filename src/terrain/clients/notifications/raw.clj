(ns terrain.clients.notifications.raw
  (:require
   [cemerick.url :as curl]
   [clj-http.client :as client]
   [terrain.util.config :as config]
   [terrain.util.transformers :refer [secured-params]]))

(def na-sort-params [:limit :offset :sortfield :sortdir])
(def na-filter-params [:seen :filter])
(def delete-matching-message-params [:filter])
(def na-message-params (concat na-sort-params na-filter-params))

(defn- na-url
  [& components]
  (str (apply curl/url (config/notifications-base-url) components)))

(defn get-messages
  [params]
  (client/get (na-url "messages")
              {:query-params (secured-params params na-message-params)
               :as           :stream}))

(defn get-unseen-messages
  [params]
  (client/get (na-url "unseen-messages")
              {:query-params (secured-params params na-message-params)
               :as           :stream}))

(defn count-messages
  [params]
  (client/get (na-url "count-messages")
              {:query-params (secured-params params na-filter-params)
               :as           :stream}))

(defn delete-notifications
  [body]
  (client/post (na-url "delete")
               {:query-params (secured-params)
                :as           :stream
                :content-type :json
                :body         body}))

(defn delete-all-notifications
  [params]
  (client/delete (na-url "delete-all")
                 {:query-params (secured-params params delete-matching-message-params)
                  :as           :stream}))

(defn mark-notifications-seen
  [body]
  (client/post (na-url "seen")
               {:query-params (secured-params)
                :as           :stream
                :content-type :json
                :body         body}))

(defn mark-all-notifications-seen
  [body]
  (client/post (na-url "mark-all-seen")
               {:query-params (secured-params)
                :as           :stream
                :content-type :json
                :body         body}))
