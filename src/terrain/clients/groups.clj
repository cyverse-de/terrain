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

(defn- find-group
  "Resolves a group by name, throwing a 404 if it does not exist. The search response
   already contains the full group, so callers that only need the group's data should use
   this rather than following up with a lookup by UUID."
  [user full-name]
  (or (find-group-by-name user full-name)
      (cxu/not-found (str "group not found: " full-name))))

(defn- resolve-group-id
  [user full-name]
  (:id (find-group user full-name)))

;; Membership result formatting. The Groups service returns source_id and subject_name
;; alongside each result, so no extra lookup is needed; we only default a blank source_id
;; (e.g. a non-federated user or a failed operation) so the response satisfies the group
;; membership schema.

(defn- format-member-results
  [results]
  (mapv (fn [{:keys [subject_id] :as result}]
          (-> result
              (update :source_id    (fn [s] (if (string/blank? s) "unknown" s)))
              (update :subject_name (fn [n] (or (not-empty n) subject_id)))))
        results))

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
  "Retrieves a single collaborator list by its short name. The search used to resolve the
   name already returns the full group, so no follow-up lookup by UUID is needed."
  [user name]
  (->> (find-group user (collaborator-list-name user name))
       (format-collaborator-list user)))

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
         format-member-results
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
         format-member-results
         (hash-map :results))))

;; Teams. Stored as groups named `de:teams:<creator>:<short>`; the external team name is
;; `<creator>:<short>`. "Public"/joinable teams are modeled by granting the well-known
;; all-users subject read on the team, surfaced to callers as the `view` privilege.

(def ^:private team-folder "de:teams")

;; The well-known subject representing all DE users. It matches the value the old Grouper
;; backend used for its public subject (and Sonora's configurable grouper.allUsers), so no
;; Sonora configuration change is required.
(def ^:private public-subject "GrouperAll")

(defn- team-full-name
  [external-name]
  (str team-folder ":" external-name))

(defn- format-team
  [group]
  (->> (update group :name (partial strip-folder team-folder))
       format-group))

(defn- grant-permission
  [user group-id subject-type subject-id level]
  (http/put (groups-url "groups" group-id "permissions" subject-type subject-id)
            {:query-params {:user user}
             :form-params  {:level level}
             :content-type :json
             :as           :json}))

(defn- group-permissions
  [user group-id]
  (:permissions (:body (http/get (groups-url "groups" group-id "permissions")
                                 {:query-params {:user user} :as :json}))))

(defn- team-public?
  [user group-id]
  (boolean (some (comp #{public-subject} :subject_id :subject) (group-permissions user group-id))))

(defn get-teams
  "Lists (or searches) teams, optionally scoped to a creator or to teams a member belongs to."
  [user {:keys [search creator member]}]
  (let [raw       (if member
                    (:groups (:body (http/get (groups-url "subjects" member "groups")
                                              {:query-params {:user user} :as :json})))
                    (search-groups user team-folder))
        formatted (->> raw
                       (filter #(string/starts-with? (:name %) (str team-folder ":")))
                       (mapv format-team))
        by-creator (if creator
                     (filterv #(string/starts-with? (:name %) (str creator ":")) formatted)
                     formatted)]
    {:groups (if search
               (filterv #(string/includes? (:name %) search) by-creator)
               by-creator)}))

(defn add-team
  "Creates a team owned by the caller, optionally granting all DE users read access when the
   team is public."
  [user {:keys [name description public_privileges] :or {public_privileges []}}]
  (let [full  (str team-folder ":" user ":" name)
        group (:body (http/post (groups-url "groups")
                                {:query-params {:user user}
                                 :form-params  {:name full :description description :display_extension name}
                                 :content-type :json
                                 :as           :json}))]
    (when (seq public_privileges)
      (grant-permission user (:id group) "group" public-subject "read"))
    (format-team group)))

(defn get-team
  "Retrieves a single team by its `<creator>:<short>` name in a single round trip."
  [user name]
  (->> (find-group user (team-full-name name))
       format-team))

(defn verify-team-exists
  "Throws a 404 if the named team does not exist."
  [user name]
  (find-group user (team-full-name name))
  nil)

(defn update-team
  "Updates the name and/or description of a team, preserving its creator prefix."
  [user name {new-name :name description :description}]
  (let [creator (first (string/split name #":" 2))
        id      (resolve-group-id user (team-full-name name))
        body    (remove-vals nil? {:name              (when new-name (str team-folder ":" creator ":" new-name))
                                   :description       description
                                   :display_extension new-name})]
    (->> (http/put (groups-url "groups" id)
                   {:query-params {:user user} :form-params body :content-type :json :as :json})
         :body
         format-team)))

(defn delete-team
  "Deletes a team, returning the removed group (including its id)."
  [user name]
  (let [id (resolve-group-id user (team-full-name name))]
    (->> (http/delete (groups-url "groups" id)
                      {:query-params {:user user} :as :json})
         :body
         format-team)))

(defn get-team-members
  "Lists the members of a team."
  [user name]
  (let [id (resolve-group-id user (team-full-name name))]
    (-> (http/get (groups-url "groups" id "members")
                  {:query-params {:user user} :as :json})
        :body
        (update :members format-subjects))))

(defn add-team-members
  "Adds members to a team. Membership implies read access, so no privilege grant is needed."
  [user name members]
  (let [id (resolve-group-id user (team-full-name name))]
    (->> (http/post (groups-url "groups" id "members")
                    {:query-params {:user user} :form-params {:members members} :content-type :json :as :json})
         :body
         :results
         format-member-results
         (hash-map :results))))

(defn remove-team-members
  "Removes members from a team."
  [user name members]
  (let [id (resolve-group-id user (team-full-name name))]
    (->> (http/post (groups-url "groups" id "members" "deleter")
                    {:query-params {:user user} :form-params {:members members} :content-type :json :as :json})
         :body
         :results
         format-member-results
         (hash-map :results))))

(defn join-team
  "Adds the caller to a public team. The membership change is performed as the administrative
   user because a non-member has no write access to the group."
  [user name]
  (let [id (resolve-group-id user (team-full-name name))]
    (when-not (team-public? user id)
      (cxu/forbidden (str "team is not open to join: " name)))
    (->> (http/post (groups-url "groups" id "members")
                    {:query-params {:user (config/groups-admin-user)}
                     :form-params  {:members [user]}
                     :content-type :json
                     :as           :json})
         :body
         :results
         format-member-results
         (hash-map :results))))

(defn leave-team
  "Removes the caller from a team, performed as the administrative user."
  [user name]
  (let [id (resolve-group-id user (team-full-name name))]
    (->> (http/post (groups-url "groups" id "members" "deleter")
                    {:query-params {:user (config/groups-admin-user)}
                     :form-params  {:members [user]}
                     :content-type :json
                     :as           :json})
         :body
         :results
         format-member-results
         (hash-map :results))))

;; Team privileges. The Groups service records group-management rights in the permissions
;; service (own/write/admin/read). Terrain translates those to the privilege vocabulary
;; used by the DE UI: `admin` (own/admin), `read` (write/read), and `view` for the public
;; subject.

(defn- revoke-permission
  [user group-id subject-type subject-id]
  (try+
   (http/delete (groups-url "groups" group-id "permissions" subject-type subject-id)
                {:query-params {:user user} :as :json})
   (catch [:status 404] _ nil)))

(defn- index-subjects
  [subjects]
  (into {} (map (juxt :id identity)) subjects))

(defn- level->privilege-name
  [subject-id level]
  (cond
    (= subject-id public-subject) "view"
    (#{"own" "admin"} level)      "admin"
    :else                         "read"))

(defn- privileges->level
  "Collapses the UI privilege names assigned to a subject into a single permission level,
   or nil when the subject's privileges should be revoked."
  [names]
  (let [names (set names)]
    (cond
      (names "admin")                     "admin"
      (or (names "read") (names "view"))  "read"
      :else                               nil)))

(defn- permission-subject
  [subjects-by-id {:keys [subject_id subject_type]}]
  (if (= subject_type "group")
    {:id subject_id :source_id "g:gsa"}
    (get subjects-by-id subject_id {:id subject_id :source_id ""})))

(defn- list-team-privileges*
  [user group-id]
  (let [perms    (group-permissions user group-id)
        user-ids (->> perms (map :subject) (filter (comp #{"user"} :subject_type)) (map :subject_id))
        by-id    (index-subjects (:subjects (lookup-subjects user (vec user-ids))))]
    {:privileges (mapv (fn [{:keys [subject level]}]
                         {:type    "access"
                          :name    (level->privilege-name (:subject_id subject) level)
                          :subject (permission-subject by-id subject)})
                       perms)}))

(defn list-team-privileges
  "Lists the privileges granted on a team, translated to the DE privilege vocabulary."
  [user name]
  (list-team-privileges* user (resolve-group-id user (team-full-name name))))

(defn update-team-privileges
  "Applies privilege updates to a team, translating DE privilege names to permission levels.
   A subject with no privileges has its permission revoked."
  [user name {:keys [updates]}]
  (let [id (resolve-group-id user (team-full-name name))]
    (doseq [{:keys [subject_id privileges]} updates]
      (let [subject-type (if (= subject_id public-subject) "group" "user")]
        (if-let [level (privileges->level privileges)]
          (grant-permission user id subject-type subject_id level)
          (revoke-permission user id subject-type subject_id))))
    (list-team-privileges* user id)))

(defn get-team-admins
  "Lists the administrators of a team: the user subjects holding own/admin, excluding the
   administrative service user and the public subject."
  [user name]
  (let [id        (resolve-group-id user (team-full-name name))
        admin-ids (->> (group-permissions user id)
                       (filter (comp #{"user"} :subject_type :subject))
                       (filter (comp #{"own" "admin"} :level))
                       (map (comp :subject_id :subject))
                       (remove #{(config/groups-admin-user)})
                       vec)
        by-id     (index-subjects (:subjects (lookup-subjects user admin-ids)))]
    {:members (mapv #(get by-id % {:id % :source_id ""}) admin-ids)}))

;; DE user group administration.

(defn remove-de-user
  "Removes a user from the well-known DE users group."
  [subject-id]
  (let [admin (config/groups-admin-user)
        id    (resolve-group-id admin (str "de:users:" (config/de-users-group)))]
    (http/delete (groups-url "groups" id "members" subject-id)
                 {:query-params {:user admin} :as :json})
    nil))
