(ns terrain.clients.bags
  (:use [clojure-commons.error-codes]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [clojure.tools.logging :as log]
            [terrain.util.config :as config]))

(defn- bags-url
  [& [components]]
  (-> (apply url (config/bags-base-url) components)
      (str)))

(defn has-bags
  [username]
  (:body (http/head (bags-url [username])))
  nil)

(defn get-bags
  [username]
  (:body (http/get (bags-url [username]) {:as :json})))

(defn add-bag
  [username contents]
  (:body (http/put (bags-url [username])
                   {:form-params  contents
                    :content-type :json
                    :as           :json})))

(defn delete-all-bags
  [username]
  (:body (http/delete (bags-url [username])))
  nil)

(defn get-bag
  [username id]
  (:body (http/get (bags-url [username id]) {:as :json})))

(defn update-bag
  [username id contents]
  (:body (http/post (bags-url [username id]) {:form-params  contents
                                              :content-type :json
                                              :as           :json})))

(defn delete-bag
  [username id]
  (:body (http/delete (bags-url [username id])))
  nil)

(defn get-default-bag
  [username]
  [username]
  (:body (http/delete (bags-url [username]) {:as :json})))

(defn update-default-bag
  [username contents]
  (:body (http/post (bags-url [username]) {:form-params  contents
                                           :content-type :json
                                           :as           :json})))

(defn delete-default-bag
  [username]
  (:body (http/delete (bags-url [username])))
  nil)
