(ns terrain.clients.groups
  "HTTP client for the new Groups service (Keycloak-backed), the intended replacement for
   iplant-groups. Every request forwards the acting user as the `user` query parameter."
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.exception-util :as cxu]
            [medley.core :refer [remove-vals]]
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

;; Name <-> UUID resolution. The Groups service is flat and addresses groups by UUID, so
;; terrain encodes its hierarchy in the group name and resolves that name to a UUID.

(defn- search-groups
  [user search]
  (:groups (:body (http/get (groups-url "groups")
                            {:query-params {:user user :search search}
                             :as           :json}))))

(defn- find-group-by-name
  [user full-name]
  (first (filter (comp #{full-name} :name) (search-groups user full-name))))

(defn- resolve-group-id
  [user full-name]
  (or (:id (find-group-by-name user full-name))
      (cxu/not-found (str "group not found: " full-name))))

;; Membership result enrichment. The Groups service returns only {subject_id, success,
;; error} per member; the terrain contract also requires source_id (and optionally
;; subject_name), so we backfill those from a single bulk subject lookup.

(defn- enrich-member-results
  [user results]
  (let [by-id (into {} (map (juxt :id identity))
                    (:subjects (lookup-subjects user (mapv :subject_id results))))]
    (mapv (fn [{:keys [subject_id] :as result}]
            (let [subject (get by-id subject_id)]
              (assoc result
                     :source_id    (or (:source_id subject) "unknown")
                     :subject_name (or (:name subject) subject_id))))
          results)))

;; Collaborator lists. Stored as groups named `de:users:<user>:collaborator-lists:<short>`,
;; with the short name also kept in display_extension.

(defn- collaborator-list-folder
  [user]
  (format "de:users:%s:collaborator-lists" user))

(defn- collaborator-list-name
  [user short-name]
  (str (collaborator-list-folder user) ":" short-name))

(defn- strip-folder
  [folder group-name]
  (string/replace group-name (re-pattern (str "^\\Q" folder ":\\E")) ""))

(defn- format-collaborator-list
  [user group]
  (->> (update group :name (partial strip-folder (collaborator-list-folder user)))
       format-group))

(defn get-collaborator-lists
  "Lists (or searches) the calling user's collaborator lists."
  ([user _details]
   (get-collaborator-lists user _details nil))
  ([user _details search]
   (let [folder (collaborator-list-folder user)
         groups (->> (search-groups user folder)
                     (filter #(string/starts-with? (:name %) (str folder ":")))
                     (mapv (partial format-collaborator-list user)))]
     {:groups (if search
                (filterv #(string/includes? (:name %) search) groups)
                groups)})))

(defn add-collaborator-list
  "Creates a collaborator list owned by the calling user."
  [user {:keys [name description]}]
  (->> (http/post (groups-url "groups")
                  {:query-params {:user user}
                   :form-params  {:name              (collaborator-list-name user name)
                                  :description       description
                                  :display_extension name}
                   :content-type :json
                   :as           :json})
       :body
       (format-collaborator-list user)))

(defn get-collaborator-list
  "Retrieves a single collaborator list by its short name."
  [user name]
  (let [id (resolve-group-id user (collaborator-list-name user name))]
    (->> (http/get (groups-url "groups" id)
                   {:query-params {:user user} :as :json})
         :body
         (format-collaborator-list user))))

(defn update-collaborator-list
  "Updates the name and/or description of a collaborator list."
  [user old-name {:keys [name description]}]
  (let [id   (resolve-group-id user (collaborator-list-name user old-name))
        body (remove-vals nil? {:name              (when name (collaborator-list-name user name))
                                :description       description
                                :display_extension name})]
    (->> (http/put (groups-url "groups" id)
                   {:query-params {:user user}
                    :form-params  body
                    :content-type :json
                    :as           :json})
         :body
         (format-collaborator-list user))))

(defn delete-collaborator-list
  "Deletes a collaborator list, returning the removed group (including its id)."
  [user name]
  (let [id (resolve-group-id user (collaborator-list-name user name))]
    (->> (http/delete (groups-url "groups" id)
                      {:query-params {:user user} :as :json})
         :body
         (format-collaborator-list user))))

(defn get-collaborator-list-members
  "Lists the members of a collaborator list."
  [user name]
  (let [id (resolve-group-id user (collaborator-list-name user name))]
    (-> (http/get (groups-url "groups" id "members")
                  {:query-params {:user user} :as :json})
        :body
        (update :members format-subjects))))

(defn add-collaborator-list-members
  "Adds members to a collaborator list, creating the list if it does not yet exist."
  [user name members]
  (let [id (or (:id (find-group-by-name user (collaborator-list-name user name)))
               (:id (add-collaborator-list user {:name name :description ""})))]
    (->> (http/post (groups-url "groups" id "members")
                    {:query-params {:user user}
                     :form-params  {:members members}
                     :content-type :json
                     :as           :json})
         :body
         :results
         (enrich-member-results user)
         (hash-map :results))))

(defn remove-collaborator-list-members
  "Removes members from a collaborator list."
  [user name members]
  (let [id (resolve-group-id user (collaborator-list-name user name))]
    (->> (http/post (groups-url "groups" id "members" "deleter")
                    {:query-params {:user user}
                     :form-params  {:members members}
                     :content-type :json
                     :as           :json})
         :body
         :results
         (enrich-member-results user)
         (hash-map :results))))

;; DE user group administration.

(defn remove-de-user
  "Removes a user from the well-known DE users group."
  [subject-id]
  (let [admin (config/groups-admin-user)
        id    (resolve-group-id admin (str "de:users:" (config/de-users-group)))]
    (http/delete (groups-url "groups" id "members" subject-id)
                 {:query-params {:user admin} :as :json})
    nil))
