(ns terrain.clients.iplant-groups
  (:use [slingshot.slingshot :only [try+]]
        [medley.core :only [remove-vals]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.exception-util :as cxu]
            [cyverse-groups-client.core :as c]
            [terrain.util.config :as config]))

;; General group name functions.

(defn- get-de-users-folder-name [client]
  (c/build-folder-name client "users"))

(defn- get-user-folder-name [client user]
  (c/build-folder-name client (format "users:%s" user)))

(defn- get-collaborator-list-folder-name [client user]
  (c/build-folder-name client (format "users:%s:collaborator-lists" user)))

(defn- get-team-folder-name [client & [user]]
  (if-not user
    (c/build-folder-name client "teams")
    (c/build-folder-name client (format "teams:%s" user))))

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
  (->> [(get-collaborator-list-folder-name client user) (get-team-folder-name client)]
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

(defn- format-group [folder group]
  (let [regex (re-pattern (str "^\\Q" folder ":"))]
    (update-in (dissoc group :detail) [:name] (fn [s] (string/replace s regex "")))))

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
    (get-groups* folder (partial format-group folder) client user lookup-fn)))

;; This function kind of uses a hack. A search string is required, but if we make it the
;; same as the folder name then that approximates listing all groups in the folder. An
;; update to iplant-groups will be required to eliminate this hack.
(defn get-collaborator-lists
  ([user]
   (let [client (get-client)]
     (get-collaborator-lists* client user (partial c/find-groups client user))))
  ([user search]
   (let [client (get-client)]
     (get-collaborator-lists* client user (partial c/find-groups client user search)))))

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

;; Team Functions

(def ^:private team-group-type "group")

(defn- ensure-team-folder-exists [client user]
  (ensure-folder-exists client (config/grouper-user) (get-team-folder-name client))
  (ensure-folder-exists client user (get-team-folder-name client user)))

(defn- get-teams* [client user search-folder lookup-fn]
  (let [folder (get-team-folder-name client)]
    (get-groups* search-folder (partial format-group folder) client user lookup-fn)))

(defn- filter-teams [search result]
  (update-in result [:groups] (partial filter (comp (partial re-find (re-pattern (str "\\Q" search))) :name))))

(defn- find-groups-with-member [client user member search-folder search]
  (let [result (c/list-subject-groups client user member search-folder)]
    (if search
      (filter-teams search result)
      result)))

;; This function kind of uses a hack. A search string is required, but if we make it the
;; same as the folder name then that approximates listing all groups in the folder. An
;; update to iplant-groups will be required to eliminate this hack.
(defn get-teams [user {:keys [search creator member]}]
  (let [client (get-client)
        folder (get-team-folder-name client creator)]
    (->> (cond member (fn [_] (find-groups-with-member client user member folder search))
               search (partial c/find-groups client user search)
               :else  (partial c/find-groups client user))
         (get-teams* client user folder))))

(defn- grant-initial-team-privileges [client user group public-privileges]
  (c/update-group-privileges client user group
                             {:updates [{:subject_id (config/grouper-user) :privileges ["admin"]}
                                        {:subject_id c/public-user :privileges public-privileges}]}))

(defn add-team [user {:keys [name description public_privileges] :or {public_privileges []}}]
  (let [client (get-client)
        folder (get-team-folder-name client user)]
    (ensure-team-folder-exists client user)
    (let [full-name (str folder ":" name)
          group     (c/add-group client user full-name team-group-type description)]
      (grant-initial-team-privileges client user full-name public_privileges)
      (format-group (get-team-folder-name client) group))))

(defn get-team [user name]
  (let [client (get-client)
        folder (get-team-folder-name client)]
    (->> (c/get-group client user (full-group-name name folder))
         (format-group folder))))

(defn verify-team-exists [user name]
  ;; get-team will return a 404 if the team doesn't exist.
  (get-team user name)
  nil)

(defn update-team [user name updates]
  (let [client  (get-client)
        folder  (get-team-folder-name client)
        creator (first (string/split name #":" 2))
        group   (full-group-name name folder)]
    (verify-group-exists client user group)
    (->> (update (select-keys updates [:name :description]) :name
                 full-group-name (get-team-folder-name client creator))
         (remove-vals nil?)
         (c/update-group client user group)
         (format-group folder))))

(defn delete-team [user name]
  (let [client (get-client)
        folder (get-team-folder-name client)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (->> (c/delete-group client user group)
         (format-group folder))))

(defn get-team-members [user name]
  (let [client (get-client)
        folder (get-team-folder-name client)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (update (c/list-group-members client user group) :members format-subjects client user)))

(defn get-team-admins [user name]
  (let [client (get-client)
        folder (get-team-folder-name client)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (->> (c/list-group-privileges client (config/grouper-user) group {:subject-source-id "ldap" :privilege "admin"})
         :privileges
         (mapv :subject)
         (remove (comp (partial = (config/grouper-user)) :id)))))

(defn- format-privilege-updates [user subject-ids privileges]
  {:updates (vec (for [subject-id subject-ids :when (not= user subject-id)]
                   {:subject_id subject-id :privileges privileges}))})

(defn- format-member-privilege-updates [user subject-ids]
  (format-privilege-updates user subject-ids ["optout" "read"]))

(defn- grant-member-privileges [client user group members]
  (c/update-group-privileges client user group (format-member-privilege-updates user members) {:replace false}))

(defn- revoke-member-privileges [client user group members]
  (c/revoke-group-privileges client user group (format-member-privilege-updates user members)))

(defn add-team-members [user name members]
  (let [client (get-client)
        folder (get-team-folder-name client)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (when (some (partial = (config/grouper-user)) members)
      (cxu/bad-request "the administrative Grouper user may not be added to any teams"))
    (grant-member-privileges client user group members)
    (c/add-group-members client user group members)))

(defn remove-team-members [user name members]
  (let [client (get-client)
        folder (get-team-folder-name client)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (when (some (partial = (config/grouper-user)) members)
      (cxu/bad-request "the administrative Grouper user may not be removed from any teams"))
    (revoke-member-privileges client user group members)
    (c/remove-group-members client user group members)))

(defn join-team [user name]
  (let [client (get-client)
        folder (get-team-folder-name client)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (when (= user (config/grouper-user))
      (cxu/bad-request "the administrative Grouper user may not join any teams"))
    (let [response (c/add-group-members client user group [user])]
      (grant-member-privileges client (config/grouper-user) group [user])
      response)))

(defn leave-team [user name]
  (let [client (get-client)
        folder (get-team-folder-name client)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (when (= user (config/grouper-user))
      (cxu/bad-request "the administrative Grouper user may not leave any teams"))
    (let [response (c/remove-group-members client user group [user])]
      (revoke-member-privileges client (config/grouper-user) group [user])
      response)))

(defn- format-group-privileges [m]
  (let [format-priv  (fn [priv] (dissoc priv :group))
        format-privs (fn [privs] (vec (distinct (map format-priv privs))))]
    (update m :privileges format-privs)))

(defn list-team-privileges [user name]
  (let [client (get-client)
        folder (get-team-folder-name client)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (format-group-privileges (c/list-group-privileges client user group {:inheritance-level "immediate"}))))

(defn update-team-privileges [user name updates]
  (let [client (get-client)
        folder (get-team-folder-name client)
        group  (full-group-name name folder)]
    (verify-group-exists client user group)
    (format-group-privileges (c/update-group-privileges client user group updates))))

;; DE user group administration functions.

(defn remove-de-user [subject-id]
  (let [client (get-client)
        folder (get-de-users-folder-name client)
        group  (full-group-name (config/de-users-group) folder)]
    (c/remove-group-member client (config/grouper-user) group subject-id)))

(defn list-groups-for-user [subject-id]
  (c/list-subject-groups (get-client) (config/grouper-user) subject-id))
