(ns terrain.clients.iplant-groups.subjects
  "Functions to perform subject lookups using the iplant-groups service. This namespace was originally created to
   avoid a cirucular dependency in terrain.auth.user-attributes when terrain.clients.iplant-groups was added as a
   dependency to that namespace."
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- iplant-groups-url
  [& components]
  (str (apply curl/url (config/ipg-base) components)))

(defn lookup-subject
  "Uses iplant-groups's subject lookup by ID endpoint to retrieve user details."
  [grouper-user short-username]
  (:body (http/get (iplant-groups-url "subjects" short-username)
                   {:query-params {:user grouper-user}
                    :as           :json})))

(defn lookup-subjects
  "Uses iplant-groups's multiple subject lookup by ID endpoint to retrieve user details."
  ([subject-ids]
   (lookup-subjects (config/grouper-user) subject-ids))
  ([grouper-user subject-ids]
   (:body (http/post (iplant-groups-url "subjects" "lookup")
                     {:query-params {:user grouper-user}
                      :form-params  {:subject_ids subject-ids}
                      :content-type :json
                      :as           :json}))))
