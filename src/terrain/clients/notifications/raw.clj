(ns terrain.clients.notifications.raw
  (:require
   [cemerick.url :as curl]
   [clj-http.client :as client]
   [clojure.string :as string]
   [medley.core :as medley]
   [terrain.util.config :as config]
   [terrain.util.transformers :refer [secured-params]]))

(def na-sort-params [:limit :offset :sort-field :sort-dir])
(def na-filter-params [:seen :filter])
(def delete-matching-message-params [:filter])
(def na-message-params (concat na-sort-params na-filter-params))

(defn- fix-params
  "The notifications service uses underscores rather than hyphens in query parameter names."
  [params]
  (medley/map-keys (fn [k] (-> k name (string/replace "-" "_") keyword)) params))

(defn- na-url
  [& components]
  (str (apply curl/url (config/notifications-base-url) components)))

(defn get-messages
  [params]
  (client/get (na-url "messages")
              {:query-params (fix-params (secured-params params na-message-params))
               :as           :json}))

(defn get-unseen-messages
  [params]
  (client/get (na-url "unseen-messages")
              {:query-params (fix-params (secured-params params na-message-params))
               :as           :json}))

(defn count-messages
  [params]
  (client/get (na-url "count-messages")
              {:query-params (fix-params (secured-params params na-filter-params))
               :as           :json}))

(defn delete-notifications
  [body]
  (client/post (na-url "delete")
               {:query-params (fix-params (secured-params))
                :as           :json
                :content-type :json
                :body         body}))

(defn delete-all-notifications
  [params]
  (client/delete (na-url "delete-all")
                 {:query-params (fix-params (secured-params params delete-matching-message-params))
                  :as           :json}))

(defn mark-notifications-seen
  [body]
  (client/post (na-url "seen")
               {:query-params (fix-params (secured-params))
                :as           :json
                :content-type :json
                :body         body}))

(defn mark-all-notifications-seen
  [body]
  (client/post (na-url "mark-all-seen")
               {:query-params (fix-params (secured-params))
                :as           :json
                :content-type :json
                :body         body}))
