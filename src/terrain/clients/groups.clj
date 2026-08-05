(ns terrain.clients.groups
  "HTTP client for the Groups service, the intended replacement for iplant-groups.

   Groups are addressed by their structured identity -- type, owner, and short name -- so
   nothing here packs hierarchy into a group name. The one place terrain's external group
   contract is composed is `format-group`, including the `<owner>:<short>` name the DE uses
   for teams. Every request forwards the acting user as the `user` query parameter."
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.exception-util :as cxu]
            [medley.core :refer [remove-vals]]
            [slingshot.slingshot :refer [try+]]
            [terrain.util.config :as config]))

;; The group types the Groups service recognizes.
(def ^:private type-collaborator-list "collaborator_list")
(def ^:private type-team "team")
(def ^:private type-community "community")
(def ^:private type-system "system")

;; The well-known subject representing all DE users. It matches the value the legacy Grouper
;; backend used for its public subject (and Sonora's configurable grouper.allUsers), so no
;; Sonora configuration change is required.
(def ^:private public-subject "GrouperAll")

(defn- groups-url
  [& components]
  (str (apply curl/url (config/groups-base) components)))

(defn- query
  "Builds the query parameters for a request: the acting user plus whichever filters were
   actually given."
  ([user] {:user user})
  ([user params] (assoc (remove-vals nil? params) :user user)))

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
                {:query-params (query user {:search search})
                 :as           :json})
      :body
      (update :subjects format-subjects)))

(defn lookup-subject
  "Looks up a single subject by ID, returning nil if the subject is not found."
  [user short-username]
  (try+
   (:body (http/get (groups-url "subjects" short-username)
                    {:query-params (query user)
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
                    {:query-params (query user)
                     :form-params  {:subject_ids subject-ids}
                     :content-type :json
                     :as           :json})))

(defn- index-subjects
  [subjects]
  (into {} (map (juxt :id identity)) subjects))

(defn- empty-user-info
  "Returns an empty user-info record for the given username."
  [username]
  {:id username :name "" :first_name "" :last_name "" :email "" :institution "" :source_id ""})

(defn lookup-subject-add-empty
  "Looks up a single subject by ID, returning an empty user-info block if nothing is found."
  [user short-username]
  (or (lookup-subject user short-username)
      (empty-user-info short-username)))

;; The external group contract.

(defn- external-name
  "The name terrain exposes for a group. Teams carry their owner as a prefix because two
   users may own teams with the same short name; no other group type is qualified."
  [{:keys [group_type owner name]}]
  (if (= group_type type-team)
    (str owner ":" name)
    name))

(defn- format-group
  "Reshapes a group from the Groups service into terrain's external group contract. This is
   the only place that contract is composed, and the only place that knows a team's external
   name embeds its owner."
  [group]
  {:id                (:id group)
   :name              (external-name group)
   :type              "group"
   :id_index          ""
   :description       (or (:description group) "")
   :display_extension (:name group)
   :display_name      (or (:display_name group) (:name group))})

(defn- epoch-millis
  [timestamp]
  (if timestamp
    (.toEpochMilli (.toInstant (java.time.OffsetDateTime/parse timestamp)))
    0))

(defn- creator-subject
  "The subject block for a group's creator, falling back to the bare owner id when the
   lookup resolved nothing or the subject has no name."
  [subjects-by-id owner]
  (let [subject (get subjects-by-id owner {:id owner :source_id ""})]
    (update subject :name #(or (not-empty %) owner))))

(defn- attach-details
  "Adds the :detail block the DE's listings read -- creation time plus creator id and display
   name -- to each formatted group, resolving all distinct owners' names with one bulk subject
   lookup. A group with no owner (e.g. a community) gets no :detail: there is no creator to
   report."
  [user groups formatted-groups]
  (let [owners (vec (distinct (keep :owner groups)))
        by-id  (if (seq owners) (index-subjects (:subjects (lookup-subjects user owners))) {})]
    (mapv (fn [{:keys [owner created_at]} formatted]
            (cond-> formatted
              owner (assoc :detail {:created_at          (epoch-millis created_at)
                                    :created_by          owner
                                    :created_by_detail   (creator-subject by-id owner)
                                    :has_composite       false
                                    :is_composite_factor false})))
          groups
          formatted-groups)))

(defn- format-groups
  "Formats a listing into the external contract, attaching :detail when it was requested."
  [user details groups]
  (let [formatted (mapv format-group groups)]
    (if details (attach-details user groups formatted) formatted)))

;; Group identity and requests. A `ref` is a group's structured identity: its type, its owner
;; where it has one, and its short name.

(defn- ref-label
  [{:keys [group-type owner name]}]
  (str group-type " " (if owner (str owner ":" name) name)))

(defn- find-group
  "Resolves a group by its structured identity, returning nil if it does not exist."
  [user {:keys [group-type owner name]}]
  (try+
   (:body (http/get (groups-url "groups" "lookup")
                    {:query-params (query user {:group_type group-type :owner owner :name name})
                     :as           :json}))
   (catch [:status 404] _ nil)))

(defn- get-group
  "Resolves a group by its structured identity, throwing a 404 if it does not exist."
  [user ref]
  (or (find-group user ref)
      (cxu/not-found (str "group not found: " (ref-label ref)))))

(defn- group-id
  [user ref]
  (:id (get-group user ref)))

;; The Groups service caps one listing response at this many groups.
(def ^:private list-page-size 1000)

(defn- list-groups
  "Lists groups page by page. The service silently caps a single response at
   `list-page-size` groups, so an unpaged request would truncate large listings."
  [user filters]
  (loop [offset 0 groups []]
    (let [page   (:groups (:body (http/get (groups-url "groups")
                                           {:query-params (query user (assoc filters
                                                                             :limit  list-page-size
                                                                             :offset offset))
                                            :as           :json})))
          groups (into groups page)]
      (if (< (count page) list-page-size)
        groups
        (recur (+ offset (count page)) groups)))))

(defn- create-group
  [user spec]
  (:body (http/post (groups-url "groups")
                    {:query-params (query user)
                     :form-params  (remove-vals nil? spec)
                     :content-type :json
                     :as           :json})))

(defn- update-group
  [user id updates]
  (:body (http/put (groups-url "groups" id)
                   {:query-params (query user)
                    :form-params  (remove-vals nil? updates)
                    :content-type :json
                    :as           :json})))

(defn- delete-group
  "Deletes a group by ID. The service returns no body, so callers report the group they
   resolved beforehand rather than a deletion response."
  [user id]
  (http/delete (groups-url "groups" id) {:query-params (query user)})
  nil)

(defn- delete-group-by-ref
  "Deletes a group by identity, returning it in the external contract shape."
  [user ref]
  (let [group (get-group user ref)]
    (delete-group user (:id group))
    (format-group group)))

;; Membership.

(defn- format-member-results
  "Defaults a blank source_id (e.g. a non-federated user or a failed operation) and subject
   name so the response satisfies the group membership schema."
  [results]
  (mapv (fn [{:keys [subject_id] :as result}]
          (-> result
              (update :source_id    (fn [s] (if (string/blank? s) "unknown" s)))
              (update :subject_name (fn [n] (or (not-empty n) subject_id)))))
        results))

(defn- list-members
  [user group-id]
  {:members (format-subjects (:members (:body (http/get (groups-url "groups" group-id "members")
                                                        {:query-params (query user) :as :json}))))})

(defn- change-members
  [user group-id components members]
  (->> (http/post (apply groups-url "groups" group-id components)
                  {:query-params (query user)
                   :form-params  {:members members}
                   :content-type :json
                   :as           :json})
       :body
       :results
       format-member-results
       (hash-map :results)))

(defn- add-members
  [user group-id members]
  (change-members user group-id ["members"] members))

(defn- remove-members
  [user group-id members]
  (change-members user group-id ["members" "deleter"] members))

;; Group permissions. The Groups service records group-management rights in the permissions
;; service (own/write/admin/read). Terrain translates those to the privilege vocabulary used
;; by the DE UI: `admin` (own/admin), `read` (write/read), and `view` for the public subject.

(defn- grant-permission
  [user group-id subject-type subject-id level]
  (http/put (groups-url "groups" group-id "permissions" subject-type subject-id)
            {:query-params (query user)
             :form-params  {:level level}
             :content-type :json
             :as           :json}))

(defn- revoke-permission
  [user group-id subject-type subject-id]
  (try+
   (http/delete (groups-url "groups" group-id "permissions" subject-type subject-id)
                {:query-params (query user) :as :json})
   (catch [:status 404] _ nil)))

(defn- group-permissions
  [user group-id]
  (:permissions (:body (http/get (groups-url "groups" group-id "permissions")
                                 {:query-params (query user) :as :json}))))

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
      (names "admin")                    "admin"
      (or (names "read") (names "view")) "read"
      :else                              nil)))

(defn- permission-subject
  [subjects-by-id {:keys [subject_id subject_type]}]
  (if (= subject_type "group")
    {:id subject_id :source_id "g:gsa"}
    (get subjects-by-id subject_id {:id subject_id :source_id ""})))

(defn- list-privileges
  [user group-id]
  (let [perms    (group-permissions user group-id)
        user-ids (->> perms (map :subject) (filter (comp #{"user"} :subject_type)) (map :subject_id))
        by-id    (index-subjects (:subjects (lookup-subjects user (vec user-ids))))]
    {:privileges (mapv (fn [{:keys [subject level]}]
                         {:type    "access"
                          :name    (level->privilege-name (:subject_id subject) level)
                          :subject (permission-subject by-id subject)})
                       perms)}))

(defn- admins-of
  "Lists the administrators of a group by id: the user subjects holding own/admin, excluding
   the administrative service user and the public subject."
  [user group-id]
  (let [admin-ids (->> (group-permissions user group-id)
                       (filter (comp #{"user"} :subject_type :subject))
                       (filter (comp #{"own" "admin"} :level))
                       (map (comp :subject_id :subject))
                       (remove #{(config/groups-admin-user)})
                       vec)
        by-id     (index-subjects (:subjects (lookup-subjects user admin-ids)))]
    {:members (mapv #(get by-id % {:id % :source_id ""}) admin-ids)}))

;; Collaborator lists, which are owned by the user who created them.

(defn- collaborator-list-ref
  [user name]
  {:group-type type-collaborator-list :owner user :name name})

(defn get-collaborator-lists
  "Lists (or searches) the calling user's collaborator lists. The search is performed by the
   service, which matches on name and description."
  ([user details]
   (get-collaborator-lists user details nil))
  ([user details search]
   {:groups (format-groups user details (list-groups user {:group_type type-collaborator-list
                                                           :owner      user
                                                           :search     search}))}))

(defn add-collaborator-list
  "Creates a collaborator list owned by the calling user."
  [user {:keys [name description]}]
  (format-group (create-group user {:group_type   type-collaborator-list
                                    :owner        user
                                    :name         name
                                    :display_name name
                                    :description  description})))

(defn get-collaborator-list
  "Retrieves a single collaborator list by its short name."
  [user name]
  (format-group (get-group user (collaborator-list-ref user name))))

(defn update-collaborator-list
  "Updates the name and/or description of a collaborator list."
  [user old-name {:keys [name description]}]
  (let [id (group-id user (collaborator-list-ref user old-name))]
    (format-group (update-group user id {:name         name
                                         :display_name name
                                         :description  description}))))

(defn delete-collaborator-list
  "Deletes a collaborator list, returning the removed group (including its id)."
  [user name]
  (delete-group-by-ref user (collaborator-list-ref user name)))

(defn get-collaborator-list-members
  "Lists the members of a collaborator list."
  [user name]
  (list-members user (group-id user (collaborator-list-ref user name))))

(defn add-collaborator-list-members
  "Adds members to a collaborator list, creating the list if it does not yet exist."
  [user name members]
  (let [id (or (:id (find-group user (collaborator-list-ref user name)))
               (:id (add-collaborator-list user {:name name :description ""})))]
    (add-members user id members)))

(defn remove-collaborator-list-members
  "Removes members from a collaborator list."
  [user name members]
  (remove-members user (group-id user (collaborator-list-ref user name)) members))

;; Teams, whose external name is `<owner>:<short>`.

(defn- team-ref
  "Parses the external team name into a group identity. Only the first colon separates the
   owner from the short name, which may itself contain colons."
  [external-name]
  (let [[owner name] (string/split external-name #":" 2)]
    (when (string/blank? name)
      (cxu/bad-request (str "team names must be qualified by their owner: " external-name)))
    {:group-type type-team :owner owner :name name}))

(defn get-teams
  "Lists (or searches) teams, optionally scoped to a creator or to teams a member belongs to.
   Filtering and searching are performed by the service."
  [user {:keys [search creator member details]}]
  {:groups (format-groups user details (list-groups user {:group_type type-team
                                                          :owner      creator
                                                          :member     member
                                                          :search     search}))})

;; Grouper separated `view` -- the group is discoverable and joinable -- from `read`, which
;; also exposes the member list, and both arrive here in public_privileges. The permissions
;; service has no level weaker than read, so the public marker is one grant either way and
;; the member-list half is carried by the group's own members_public flag.
(defn- members-public?
  [public-privileges]
  (boolean (some #{"read"} public-privileges)))

;; `optin` is what let a user add themselves without approval. The DE sends it for
;; communities and withholds it from public teams, which go through the join-request
;; flow instead -- so this is not derivable from members-public?, even though the two
;; happen to coincide in the privileges the DE currently sends.
(defn- joinable?
  [public-privileges]
  (boolean (some #{"optin"} public-privileges)))

(defn add-team
  "Creates a team owned by the caller, optionally granting all DE users read access when the
   team is public."
  [user {:keys [name description public_privileges] :or {public_privileges []}}]
  (let [group (create-group user {:group_type     type-team
                                  :owner          user
                                  :name           name
                                  :display_name   name
                                  :description    description
                                  :members_public (members-public? public_privileges)
                                  :joinable       (joinable? public_privileges)})]
    (when (seq public_privileges)
      (grant-permission user (:id group) "group" public-subject "read"))
    (format-group group)))

(defn get-team
  "Retrieves a single team by its `<owner>:<short>` name."
  [user name]
  (format-group (get-group user (team-ref name))))

(defn verify-team-exists
  "Throws a 404 if the named team does not exist."
  [user name]
  (get-group user (team-ref name))
  nil)

(defn update-team
  "Updates the name and/or description of a team. The owner is part of the team's identity
   and cannot change, so only the short name is sent."
  [user name {new-name :name description :description}]
  (let [id (group-id user (team-ref name))]
    (format-group (update-group user id {:name         new-name
                                         :display_name new-name
                                         :description  description}))))

(defn delete-team
  "Deletes a team, returning the removed group (including its id)."
  [user name]
  (delete-group-by-ref user (team-ref name)))

(defn get-team-members
  "Lists the members of a team."
  [user name]
  (list-members user (group-id user (team-ref name))))

(defn- reject-admin-user
  "The administrative account may not be a member of anything. It is filtered out of every
   listing, so admitting it creates a member the UI cannot show or remove."
  [members]
  (when (some #{(config/groups-admin-user)} members)
    (cxu/bad-request "the administrative user may not be added to or removed from any group")))

(defn add-team-members
  "Adds members to a team. Membership implies read access, so no privilege grant is needed."
  [user name members]
  (reject-admin-user members)
  (add-members user (group-id user (team-ref name)) members))

(defn remove-team-members
  "Removes members from a team."
  [user name members]
  (reject-admin-user members)
  (remove-members user (group-id user (team-ref name)) members))

(defn join-team
  "Adds the caller to a team that is open to join. The membership change is performed as the
   administrative user because a non-member has no write access to the group.

   Being public is not enough: Grouper granted the all-users subject `optin` on groups that
   could be joined directly and only `view` on public teams, refusing a self-join against
   those. Teams use the join-request flow, where an administrator approves."
  [user name]
  (reject-admin-user [user])
  (let [group (get-group user (team-ref name))]
    (when-not (:joinable group)
      (cxu/forbidden (str "team is not open to join: " name)))
    (add-members (config/groups-admin-user) (:id group) [user])))

(defn- revoke-membership-read
  "Revokes a subject's read permission on a group, and only read.

   The DE granted every member `optout` and `read` together, so leaving revoked both and an
   ex-member kept nothing. The importer turns that `read` into an explicit grant, which
   removing membership does not touch -- so without this an ex-member keeps read access to the
   group and its member list.

   Deliberately narrow: Grouper's leave revoked only the member privileges, so someone holding
   `admin` who left a group kept administering it. Revoking whatever level is present would
   strip owners and admins."
  [group-id subject-id]
  (let [admin (config/groups-admin-user)
        level (->> (group-permissions admin group-id)
                   (filter #(= subject-id (:subject_id (:subject %))))
                   first
                   :level)]
    (when (= "read" level)
      (revoke-permission admin group-id "user" subject-id))))

(defn leave-team
  "Removes the caller from a team, performed as the administrative user, then drops the read
   grant that membership carried. Membership goes first: if the removal fails, the caller is
   still an ordinary member rather than a member whose read grant was already revoked."
  [user name]
  (reject-admin-user [user])
  (let [id      (group-id user (team-ref name))
        results (remove-members (config/groups-admin-user) id [user])]
    (revoke-membership-read id user)
    results))

(defn list-team-privileges
  "Lists the privileges granted on a team, translated to the DE privilege vocabulary."
  [user name]
  (list-privileges user (group-id user (team-ref name))))

(defn update-team-privileges
  "Applies privilege updates to a team, translating DE privilege names to permission levels.
   A subject with no privileges has its permission revoked."
  [user name {:keys [updates]}]
  (let [id (group-id user (team-ref name))]
    (doseq [{:keys [subject_id privileges]} updates]
      (let [subject-type (if (= subject_id public-subject) "group" "user")]
        (if-let [level (privileges->level privileges)]
          (grant-permission user id subject-type subject_id level)
          (revoke-permission user id subject-type subject_id))))
    (list-privileges user id)))

(defn get-team-admins
  "Lists the administrators of a team."
  [user name]
  (admins-of user (group-id user (team-ref name))))

;; Communities, which belong to no user namespace. Public communities grant the all-users
;; subject read (joinable), surfaced as the `view` privilege. Community admins hold the admin
;; privilege and are also members.

(defn- community-ref
  [name]
  {:group-type type-community :name name})

(defn get-communities
  "Lists (or searches) communities, reporting whether the caller is a member of each. The
   details flag is deliberately ignored: communities have no owner, so there is no creator
   to build a :detail block from."
  [user {:keys [search member]}]
  (let [groups    (list-groups user {:group_type type-community :member member :search search})
        ;; When listing a user's own communities every result is a membership; otherwise the
        ;; caller's memberships are fetched once rather than once per listed community.
        member-of (if (= user member)
                    (set (map external-name groups))
                    (set (map external-name (list-groups user {:group_type type-community
                                                               :member     user}))))]
    ;; :privileges is intentionally empty: the DE UI derives admin/follower status from the
    ;; /admins and /members endpoints, and computing per-community privileges here would cost
    ;; one permissions request per listed community.
    {:groups (mapv #(assoc (format-group %)
                           :member     (contains? member-of (external-name %))
                           :privileges [])
                   groups)}))

(defn admin-get-communities
  "Lists (or searches) all communities without per-user membership details."
  [user {:keys [search]}]
  {:groups (mapv format-group (list-groups user {:group_type type-community :search search}))})

(defn add-community
  "Creates a community, granting all DE users read when it is public."
  [user {:keys [name description public_privileges] :or {public_privileges []}}]
  (let [group (create-group user {:group_type     type-community
                                  :name           name
                                  :display_name   name
                                  :description    description
                                  :members_public (members-public? public_privileges)
                                  :joinable       (joinable? public_privileges)})]
    (when (seq public_privileges)
      (grant-permission user (:id group) "group" public-subject "read"))
    (format-group group)))

(defn get-community
  "Retrieves a single community by its short name."
  [user name]
  (format-group (get-group user (community-ref name))))

(defn update-community
  "Updates the name and/or description of a community."
  [user name {new-name :name description :description}]
  (let [id (group-id user (community-ref name))]
    (format-group (update-group user id {:name         new-name
                                         :display_name new-name
                                         :description  description}))))

(defn delete-community
  "Deletes a community, returning the removed group (including its id)."
  [user name]
  (delete-group-by-ref user (community-ref name)))

(defn get-community-members
  "Lists the members of a community."
  [user name]
  (list-members user (group-id user (community-ref name))))

(defn get-community-admins
  "Lists the administrators of a community."
  [user name]
  (admins-of user (group-id user (community-ref name))))

(defn add-community-admins
  "Grants the given subjects the admin privilege on a community and adds them as members."
  [user name members]
  (reject-admin-user members)
  (let [id (group-id user (community-ref name))]
    (doseq [member members]
      (grant-permission user id "user" member "admin"))
    (add-members user id members)))

(defn remove-community-admins
  "Revokes the admin privilege from the given subjects and removes them as members."
  [user name members]
  (reject-admin-user members)
  (let [id (group-id user (community-ref name))]
    (doseq [member members]
      (revoke-permission user id "user" member))
    (remove-members user id members)))

(defn join-community
  "Adds the caller to a community that is open to join, performed as the administrative user.
   Communities carried `optin` in Grouper, so in practice this admits the same set as before.
   See join-team."
  [user name]
  (reject-admin-user [user])
  (let [group (get-group user (community-ref name))]
    (when-not (:joinable group)
      (cxu/forbidden (str "community is not open to join: " name)))
    (add-members (config/groups-admin-user) (:id group) [user])))

(defn leave-community
  "Removes the caller from a community, performed as the administrative user, then drops the
   read grant that membership carried. See leave-team for the ordering."
  [user name]
  (reject-admin-user [user])
  (let [id      (group-id user (community-ref name))
        results (remove-members (config/groups-admin-user) id [user])]
    (revoke-membership-read id user)
    results))

;; DE user group administration.

(defn list-groups-for-user
  "Lists the groups to which the given subject belongs, including through nesting."
  [subject-id details]
  (let [admin  (config/groups-admin-user)
        groups (:groups (:body (http/get (groups-url "subjects" subject-id "groups")
                                         {:query-params (query admin)
                                          :as           :json})))]
    {:groups (format-groups admin details groups)}))

(defn remove-de-user
  "Removes a user from the well-known DE users group."
  [subject-id]
  (let [admin (config/groups-admin-user)
        id    (group-id admin {:group-type type-system :name (config/de-users-group)})]
    (http/delete (groups-url "groups" id "members" subject-id)
                 {:query-params (query admin) :as :json})
    nil))
