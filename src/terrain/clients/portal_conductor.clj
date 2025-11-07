(ns terrain.clients.portal-conductor
  (:require
   [cemerick.url :as curl]
   [clj-http.client :as client]
   [terrain.util.config :as config]))

(defn- portal-conductor-url
  "Formats a URL that can be used to contact portal-conductor."
  [& components]
  (str (apply curl/url (config/portal-conductor-base-url) components)))

(defn- portal-conductor-opts
  ([]
   (portal-conductor-opts {}))
  ([existing-opts]
   (assoc existing-opts
          :basic-auth [(config/portal-conductor-username) (config/portal-conductor-password)]
          :insecure?  true)))

(defn get-user-details
  "Obtains details for the user with the given username."
  [username]
  (:body
   (client/get (portal-conductor-url "ldap" "users" username)
               (portal-conductor-opts {:as :json}))))
