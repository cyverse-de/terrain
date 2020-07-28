(ns terrain.clients.iplant-groups.subjects
  "Functions to perform subject lookups using the iplant-groups service. This namespace was originally created to
   avoid a cirucular dependency in terrain.auth.user-attributes when terrain.clients.iplant-groups was added as a
   dependency to that namespace."
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- lookup-subject-url
  [short-username]
  (str (curl/url (config/ipg-base) "subjects" short-username)))

(defn lookup-subject
  "Uses iplant-groups's subject lookup by ID endpoint to retrieve user details."
  [user short-username]
  (:body (http/get (lookup-subject-url short-username)
                   {:query-params {:user user}
                    :as           :json})))
