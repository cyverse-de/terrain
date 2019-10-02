(ns terrain.clients.iplant-groups
  (:use [slingshot.slingshot :only [try+]]
        [medley.core :only [remove-vals]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.exception-util :as cxu]
            [cyverse-groups-client.core :as c]
            [terrain.clients.apps.raw :as apps-client]
            [terrain.clients.metadata.raw :as metadata-client]
            [terrain.util.config :as config]))

(def ^:private team-group-type "group")
(def group-type-teams "teams")
(def group-type-communities "communities")

;; General group name functions.

(defn- get-de-users-folder-name [client]
  (c/build-folder-name client "users"))

(defn- get-user-folder-name [client user]
  (c/build-folder-name client (format "users:%s" user)))

(defn- get-collaborator-list-folder-name [client user]
  (c/build-folder-name client (format "users:%s:collaborator-lists" user)))

(defn- get-team-folder-name [client team-type & [user]]
  (if-not user
    (c/build-folder-name client team-type)
    (c/build-folder-name client (format "%s:%s" team-type user))))

;; Subject search functions.

(defn grouper-admin-user? [{username :id}]
  (= username (config/grouper-user)))

(defn format-like-trellis
  "Reformat an iplant-groups response to look like a trellis response."
  [response]
  {:username (:id response)
   :firstname (:first_name response)
   :lastname (:last_name response)
   :name (:name response)
   :email (:email response)
   :institution (:institution response)})

(defn- empty-user-info
  "Returns an empty user-info record for the given username."
  [username]
  {:id username
   :name ""
   :first_name ""
   :last_name ""
   :email ""
   :institution ""
   :source_id ""})

(defn- lookup-subject-url
  [short-username]
  (str (curl/url (config/ipg-base) "subjects" short-username)))

(defn lookup-subject
  "Uses iplant-groups's subject lookup by ID endpoint to retrieve user details."
  [user short-username]
  (try
    (if-let [user-info (-> (http/get (lookup-subject-url short-username) {:query-params {:user user} :as :json})
                           (:body))]
      user-info
      (do (log/warn (str "no user info found for username '" short-username "'"))
          nil))
    (catch Exception e
      (log/error e (str "username lookup for '" short-username "' failed"))
      nil)))

(defn lookup-subject-add-empty
  "Uses iplant-groups's subject lookup by ID endpoint to retrieve user details, returning an empty user info block if nothing is found."
  [user short-username]
  (if-let [user-info (lookup-subject user short-username)]
    user-info
    (empty-user-info short-username)))

(defn- get-client []
  (c/new-cyverse-groups-client (config/ipg-base) (config/environment-name)))

(defn- build-group-name-prefix-regex [client user]
  (->> [(get-collaborator-list-folder-name client user)
        (get-team-folder-name client group-type-teams)
        (get-team-folder-name client group-type-communities)]
       (mapv (partial format "\\Q%s\\E"))
       (string/join "|")
       (format "^(?:%s):")
       re-pattern))

(defn- format-subjects [subjects client user]
  (let [regex (build-group-name-prefix-regex client user)]
    (vec (for [subject (remove grouper-admin-user? subjects)]
           (assoc subject
             :display_name (:name subject)
             :name         (string/replace (:name subject) regex ""))))))

(defn find-subjects
  [user search]
  (let [client (get-client)]
    (update (c/find-subjects (get-client) user search) :subjects format-subjects client user)))

(defn- create-folder [client user name]
  (c/add-folder client (config/grouper-user) name "")
  (when-not (= user (config/grouper-user))
    (c/grant-folder-privilege client (config/grouper-user) name user :stem))
  nil)

(defn- folder-exists? [client user name]
  (try+
   (c/get-folder client user name)
   true
   (catch [:status 404] _
     false)))

(defn- ensure-folder-exists [client user name]
  (when-not (folder-exists? client user name)
    (create-folder client user name))
  nil)

;; General group functions.

(defn- full-group-name [name folder]
  (when name
    (format "%s:%s" folder name)))

(defn- format-group-with-detail
  [folder group]
  (let [regex (re-pattern (str "^\\Q" folder ":"))]
    (update-in group [:name] (fn [s] (string/replace s regex "")))))

(defn- format-group
  [folder group]
  (dissoc (format-group-with-detail folder group)))

(defn- get-groups* [folder format-fn client user lookup-fn]
  (if (folder-exists? client user folder)
    {:groups (mapv format-fn (:groups (lookup-fn folder)))}
    {:groups []}))

(defn- group-exists? [client user name]
  (try+
   (c/get-group client user name)
   true
   (catch [:status 404] _
     false)))

(defn- verify-group-exists [client user name]
  ;; get-group will return a 404 if the group doesn't exist.
  (c/get-group client user name)
  nil)

;; Collaborator List Functions

(def ^:private collaborator-list-group-type "group")

(defn- ensure-collaborator-list-folder-exists [client user]
  (ensure-folder-exists client user (get-user-folder-name client user))
  (ensure-folder-exists client user (get-collaborator-list-folder-name client user)))

(defn- get-collaborator-lists* [client user lookup-fn]
  (let [folder (get-collaborator-list-folder-name client user)]
    (get-groups* folder (partial format-group-with-detail folder) client user lookup-fn)))

;; This function kind of uses a hack. A search string is required, but if we make it the
;; same as the folder name then that approximates listing all groups in the folder. An
;; update to iplant-groups will be required to eliminate this hack.
(defn get-collaborator-lists
  ([user details]
   (let [client (get-client)]
     (get-collaborator-lists* client user (partial c/find-groups client user details))))
  ([user details search]
   (let [client (get-client)]
     (get-collaborator-lists* client user (partial c/find-groups client user details search)))))

(defn add-collaborator-list [user {:keys [name description]}]
  (let [client (get-client)
        folder (get-collaborator-list-folder-name client user)]
    (ensure-collaborator-list-folder-exists client user)
    (->> (c/add-group client user (str folder ":" name) collaborator-list-group-type description)
         (format-group folder))))

(defn get-collaborator-list [user name]
  (let [client (get-client)
        folder (get-collaborator-list-folder-name client user)]
    (->> (c/get-group client user (full-group-name name folder))
         (format-group folder))))

(defn update-collaborator-list [user old-name {:keys [name description]}]
  (let [client    (get-client)
        folder    (get-collaborator-list-folder-name client user)
        old-group (full-group-name old-name folder)
        new-group (when name (full-group-name name folder))]
    (->> (remove-vals nil? {:name new-group :description description})
         (c/update-group client user old-group)
         (format-group folder))))

(defn delete-collaborator-list [user name]
  (let [client (get-client)
        folder (get-collaborator-list-folder-name client user)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (->> (c/delete-group client user group)
         (format-group folder))))

(defn get-collaborator-list-members [user name]
  (let [client (get-client)
        folder (get-collaborator-list-folder-name client user)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (update (c/list-group-members client user group) :members format-subjects client user)))

(defn add-collaborator-list-members [user name members]
  (let [client (get-client)
        folder (get-collaborator-list-folder-name client user)
        group  (full-group-name name folder)]
    (when-not (group-exists? client user group)
      (add-collaborator-list user {:name name :description ""}))
    (c/add-group-members client user group members)))

(defn remove-collaborator-list-members [user name members]
  (let [client (get-client)
        folder (get-collaborator-list-folder-name client user)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (c/remove-group-members client user group members)))

;; Team & Community Functions

(defn- ensure-team-folder-exists [client user]
  (ensure-folder-exists client (config/grouper-user) (get-team-folder-name client group-type-teams))
  (ensure-folder-exists client user (get-team-folder-name client group-type-teams user)))

(defn- find-teams* [client team-type user search-folder lookup-fn]
  (let [folder (get-team-folder-name client team-type)]
    (get-groups* search-folder (partial format-group-with-detail folder) client user lookup-fn)))

(defn- filter-teams [search result]
  (update-in result [:groups] (partial filter (comp (partial re-find (re-pattern (str "\\Q" search))) :name))))

(defn- find-groups-with-member [client user member details search-folder search]
  (let [result (c/list-subject-groups client user member details search-folder)]
    (if search
      (filter-teams search result)
      result)))

;; This function kind of uses a hack. A search string is required, but if we make it the
;; same as the folder name then that approximates listing all groups in the folder. An
;; update to iplant-groups will be required to eliminate this hack.
(defn- get-teams* [team-type user {:keys [search creator details member]}]
  (let [client (get-client)
        folder (get-team-folder-name client team-type creator)]
    (->> (cond member (fn [_] (find-groups-with-member client user member details folder search))
               search (partial c/find-groups client user details search)
               :else (partial c/find-groups client user details))
         (find-teams* client team-type user folder))))

(defn get-teams [user params]
  (get-teams* group-type-teams user params))

(defn- get-user-community-privileges [user]
  (let [client (get-client)
        folder (get-team-folder-name client group-type-communities)
        regex  (re-pattern (str "^\\Q" folder ":"))]
    (->> (c/list-subject-privileges client (config/grouper-user) user {:entity-type "group" :folder folder})
         :privileges
         (map (fn [{privilege :name {group :name} :group}] {(string/replace group regex "") #{privilege}}))
         (apply merge-with into))))

(defn- format-community [memberships privileges-for team]
  (assoc team
    :member     (contains? memberships (:name team))
    :privileges (vec (get privileges-for (:name team) []))))

(defn get-communities [user {:keys [member] :as params}]
  (let [team-listing   (get-teams* group-type-communities user params)
        teams-for-user (if (= user member) team-listing (get-teams* group-type-communities user {:member user}))
        memberships    (->> teams-for-user :groups (map :name) set)
        privileges-for (get-user-community-privileges user)]
    {:groups (mapv (partial format-community memberships privileges-for) (:groups team-listing))}))

(defn admin-get-communities [user params]
  (get-teams* group-type-communities user params))

(defn- grant-initial-team-privileges [client user group initial-admin public-privileges]
  (c/update-group-privileges client user group
                             {:updates [{:subject_id initial-admin :privileges ["admin"]}
                                        {:subject_id c/public-user :privileges public-privileges}]}))

(defn add-team [user {:keys [name description public_privileges] :or {public_privileges []}}]
  (let [client (get-client)
        folder (get-team-folder-name client group-type-teams user)]
    (ensure-team-folder-exists client user)
    (let [full-name (full-group-name name folder)
          group     (c/add-group client user full-name team-group-type description)]
      (grant-initial-team-privileges client user full-name (config/grouper-user) public_privileges)
      (format-group (get-team-folder-name client group-type-teams) group))))

(defn add-community [user {:keys [name description public_privileges] :or {public_privileges []}}]
  (let [client (get-client)
        folder (get-team-folder-name client group-type-communities)]
    (ensure-folder-exists client (config/grouper-user) folder)
    (let [full-name (full-group-name name folder)
          group     (c/add-group client (config/grouper-user) full-name team-group-type description)]
      (grant-initial-team-privileges client (config/grouper-user) full-name user public_privileges)
      (format-group folder group))))

(defn- get-team* [team-type user name]
  (let [client (get-client)
        folder (get-team-folder-name client team-type)]
    (->> (c/get-group client user (full-group-name name folder))
         (format-group folder))))

(defn get-team [user name]
  (get-team* group-type-teams user name))

(defn get-community [user name]
  (get-team* group-type-communities user name))

(defn verify-team-exists [user name]
  ;; get-team will return a 404 if the team doesn't exist.
  (get-team user name)
  nil)

(defn update-team [user name updates]
  (let [client  (get-client)
        folder  (get-team-folder-name client group-type-teams)
        creator (first (string/split name #":" 2))
        group   (full-group-name name folder)]
    (verify-group-exists client user group)
    (->> (update (select-keys updates [:name :description]) :name
                 full-group-name (get-team-folder-name client group-type-teams creator))
         (remove-vals nil?)
         (c/update-group client user group)
         (format-group folder))))

(defn- retag-avu [new-group-name avu]
  (-> avu
      (select-keys [:id :attr :unit :avus])
      (assoc :value new-group-name)))

(defn- retag-apps [new-group-name app-tag-avus]
  (let [app-id->avus (group-by :target_id app-tag-avus)]
    (doseq [app-id (keys app-id->avus)]
      (->> (get app-id->avus app-id)
           (map #(retag-avu new-group-name %))
           (hash-map :avus)
           (metadata-client/update-avus metadata-client/target-type-app app-id)))))

(defn- check-for-tagged-apps [retag-apps? force-rename? group new-group-name]
  (when (or retag-apps? (not force-rename?))
    (when-let [app-tag-avus (->> (metadata-client/find-avus metadata-client/target-type-app
                                                            (config/communities-metadata-attr)
                                                            group)
                                 :avus
                                 seq)]
      (if retag-apps?
        (retag-apps new-group-name app-tag-avus)
        (cxu/exists "Some apps have been tagged with the old community name"
                    :name group
                    :apps (:body (apps-client/admin-get-apps-in-community group :as :json)))))))

(defn update-community [user name retag-apps? force-rename? {new-name :name :as updates}]
  (let [client  (get-client)
        folder  (get-team-folder-name client group-type-communities)
        group   (full-group-name name folder)]
    (verify-group-exists client user group)
    (when (and new-name (not= name new-name))
      (check-for-tagged-apps retag-apps? force-rename? group (full-group-name new-name folder)))
    (->> (update (select-keys updates [:name :description])
                 :name
                 full-group-name
                 folder)
         (remove-vals nil?)
         (c/update-group client user group)
         (format-group folder))))

(defn- delete-team* [team-type user name]
  (let [client (get-client)
        folder (get-team-folder-name client team-type)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (->> (c/delete-group client user group)
         (format-group folder))))

(defn delete-team [user name]
  (delete-team* group-type-teams user name))

(defn delete-community [user name]
  (delete-team* group-type-communities user name))

(defn- get-team-members* [team-type user name]
  (let [client (get-client)
        folder (get-team-folder-name client team-type)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (update (c/list-group-members client user group) :members format-subjects client user)))

(defn get-team-members [user name]
  (get-team-members* group-type-teams user name))

(defn get-community-members [user name]
  (get-team-members* group-type-communities user name))

(defn- get-team-admins* [team-type user name]
  (let [client (get-client)
        folder (get-team-folder-name client team-type)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (->> (c/list-group-privileges client (config/grouper-user) group {:subject-source-id "ldap" :privilege "admin"})
         :privileges
         (mapv :subject)
         (remove (comp (partial = (config/grouper-user)) :id))
         (hash-map :members))))

(defn get-team-admins [user name]
  (get-team-admins* group-type-teams user name))

(defn get-community-admins [user name]
  (get-team-admins* group-type-communities user name))

(defn- format-privilege-updates [user subject-ids privileges]
  {:updates (vec (for [subject-id subject-ids :when (not= user subject-id)]
                   {:subject_id subject-id :privileges privileges}))})

(defn- format-member-privilege-updates [user subject-ids]
  (format-privilege-updates user subject-ids ["optout" "read"]))

(defn- grant-member-privileges [client user group members]
  (c/update-group-privileges client user group (format-member-privilege-updates user members) {:replace false}))

(defn- revoke-member-privileges [client user group members]
  (c/revoke-group-privileges client user group (format-member-privilege-updates user members)))

(defn- format-admin-privilege-updates [user subject-ids]
  (format-privilege-updates user subject-ids ["admin"]))

(defn- grant-admin-privileges [client user group members]
  (c/update-group-privileges client user group (format-admin-privilege-updates user members) {:replace false}))

(defn- revoke-admin-privileges [client user group members]
  (c/revoke-group-privileges client user group (format-admin-privilege-updates user members)))

(defn- add-team-members* [team-type grant-privileges-fn user name members]
  (let [client (get-client)
        folder (get-team-folder-name client team-type)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (when (some (partial = (config/grouper-user)) members)
      (cxu/bad-request "the administrative Grouper user may not be added to any teams or communities"))
    (grant-privileges-fn client user group members)
    (c/add-group-members client user group members)))

(defn add-team-members [user name members]
  (add-team-members* group-type-teams grant-member-privileges user name members))

(defn add-community-admins [user name members]
  (add-team-members* group-type-communities grant-admin-privileges user name members))

(defn- remove-team-members* [team-type revoke-privileges-fn user name members]
  (let [client (get-client)
        folder (get-team-folder-name client team-type)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (when (some (partial = (config/grouper-user)) members)
      (cxu/bad-request "the administrative Grouper user may not be removed from any teams or communities"))
    (revoke-privileges-fn client user group members)
    (c/remove-group-members client user group members)))

(defn remove-team-members [user name members]
  (remove-team-members* group-type-teams revoke-member-privileges user name members))

(defn remove-community-admins [user name members]
  (remove-team-members* group-type-communities revoke-admin-privileges user name members))

(defn- join-team* [team-type user name]
  (let [client (get-client)
        folder (get-team-folder-name client team-type)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (when (= user (config/grouper-user))
      (cxu/bad-request "the administrative Grouper user may not join any teams or communities"))
    (let [response (c/add-group-members client user group [user])]
      (grant-member-privileges client (config/grouper-user) group [user])
      response)))

(defn join-team [user name]
  (join-team* group-type-teams user name))

(defn join-community [user name]
  (join-team* group-type-communities user name))

(defn- leave-team* [team-type user name]
  (let [client (get-client)
        folder (get-team-folder-name client team-type)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (when (= user (config/grouper-user))
      (cxu/bad-request "the administrative Grouper user may not leave any teams or communities"))
    (let [response (c/remove-group-members client user group [user])]
      (revoke-member-privileges client (config/grouper-user) group [user])
      response)))

(defn leave-team [user name]
  (leave-team* group-type-teams user name))

(defn leave-community [user name]
  (leave-team* group-type-communities user name))

(defn- format-group-privileges [m]
  (let [format-priv  (fn [priv] (dissoc priv :group))
        format-privs (fn [privs] (vec (distinct (map format-priv privs))))]
    (update m :privileges format-privs)))

(defn list-team-privileges [user name]
  (let [client (get-client)
        folder (get-team-folder-name client group-type-teams)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (format-group-privileges (c/list-group-privileges client user group {:inheritance-level "immediate"}))))

(defn update-team-privileges [user name updates]
  (let [client (get-client)
        folder (get-team-folder-name client group-type-teams)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (format-group-privileges (c/update-group-privileges client user group updates))))

;; DE user group administration functions.

(defn remove-de-user [subject-id]
  (let [client (get-client)
        folder (get-de-users-folder-name client)
        group  (full-group-name (config/de-users-group) folder)]
    (c/remove-group-member client (config/grouper-user) group subject-id)))

(defn list-groups-for-user [subject-id details]
  (c/list-subject-groups (get-client) (config/grouper-user) subject-id details))
