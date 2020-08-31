(ns terrain.clients.bags
  (:use [clojure-commons.error-codes]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [clojure.tools.logging :as log]
            [terrain.util.config :as config]))

(defn- bags-url
  ([]
   (bags-url []))
  ([components]
   (-> (apply url (config/bags-base-url) components)
       (str))))

(defn has-bags
  [username]
  (<= 200 (:status (http/head (bags-url [username]))) 299))

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
  (<= 200 (:status (http/delete (bags-url [username]))) 299))

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
  (<= 200 (:status (http/delete (bags-url [username id]))) 299))
