(ns terrain.clients.oauth
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- oauth-url [& components]
  (str (apply curl/url (config/oauth-base-uri) components)))

(defn get-token [username password]
  (:body (http/post (oauth-url "token")
                    {:form-params {:grant_type "password"
                                   :client_id  (config/oauth-client-id)
                                   :username   username
                                   :password   password}
                     :as          :x-www-form-urlencoded})))
