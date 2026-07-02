(ns terrain.clients.groups
  "HTTP client for the new Groups service (Keycloak-backed), the intended replacement for
   iplant-groups. Every request forwards the acting user as the `user` query parameter."
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [try+]]
            [terrain.util.config :as config]))

(defn- groups-url
  [& components]
  (str (apply curl/url (config/groups-base) components)))

(defn- admin-user?
  "True if the given subject is the Groups administrative user, which should never be
   surfaced to callers."
  [{id :id}]
  (= id (config/groups-admin-user)))

;; Subject search and lookup functions.

(defn format-like-trellis
  "Reformat a Groups subject response to look like a trellis response."
  [response]
  {:username    (:id response)
   :firstname   (:first_name response)
   :lastname    (:last_name response)
   :name        (:name response)
   :email       (:email response)
   :institution (:institution response)})

(defn- format-subject
  "The Groups service returns user subjects directly, so the display name is simply the
   subject name."
  [subject]
  (assoc subject :display_name (:name subject)))

(defn- format-subjects
  [subjects]
  (mapv format-subject (remove admin-user? subjects)))

(defn find-subjects
  "Searches for subjects matching the given search string."
  [user search]
  (-> (http/get (groups-url "subjects")
                {:query-params {:user user :search search}
                 :as           :json})
      :body
      (update :subjects format-subjects)))

(defn lookup-subject
  "Looks up a single subject by ID, returning nil if the subject is not found."
  [user short-username]
  (try+
   (:body (http/get (groups-url "subjects" short-username)
                    {:query-params {:user user}
                     :as           :json}))
   (catch [:status 404] _
     (log/warn (str "no user info found for username '" short-username "'"))
     nil)
   (catch Object _
     (log/error (:throwable &throw-context) "user lookup for '" short-username "' failed")
     nil)))

(defn lookup-subjects
  "Looks up multiple subjects by ID. Unresolvable IDs are silently omitted by the service."
  [user subject-ids]
  (:body (http/post (groups-url "subjects" "lookup")
                    {:query-params {:user user}
                     :form-params  {:subject_ids subject-ids}
                     :content-type :json
                     :as           :json})))

(defn- empty-user-info
  "Returns an empty user-info record for the given username."
  [username]
  {:id username :name "" :first_name "" :last_name "" :email "" :institution "" :source_id ""})

(defn lookup-subject-add-empty
  "Looks up a single subject by ID, returning an empty user-info block if nothing is found."
  [user short-username]
  (or (lookup-subject user short-username)
      (empty-user-info short-username)))

;; General group formatting.

(defn- format-group
  "Reshapes a Groups service group into terrain's group contract, synthesizing the `type`
   and `id_index` fields that the new service does not provide."
  [group]
  (-> group
      (assoc :type "group")
      (assoc :id_index (or (:id_index group) ""))))

(defn list-groups-for-user
  "Lists the groups to which the given subject belongs."
  [subject-id _details]
  (-> (http/get (groups-url "subjects" subject-id "groups")
                {:query-params {:user (config/groups-admin-user)}
                 :as           :json})
      :body
      (update :groups (partial mapv format-group))))
