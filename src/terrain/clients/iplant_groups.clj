(ns terrain.clients.iplant-groups
  (:use [slingshot.slingshot :only [try+]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [cyverse-groups-client.core :as c]
            [terrain.util.config :as config]))

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
  {:email     ""
   :firstname ""
   :id        username
   :lastname  ""})

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

(defn search-subjects
  "Uses iplant-groups's subject search endpoint to retrieve user details."
  [user search]
  (let [res (http/get (str (curl/url (config/ipg-base) "subjects"))
                      ;; Adding wildcards matches previous (trellis) search behavior
                      {:query-params {:user user :search (str "*" search "*")}
                       :as           :json})
        status (:status res)]
    (when-not (#{200 404} status)
      (throw (Exception. (str "iplant-groups service returned status " status))))
    {:subjects (:subjects (:body res))}))

(defn- get-client []
  (c/new-cyverse-groups-client (config/ipg-base) (config/environment-name)))

(defn- get-optional-folder [client user name]
  (try+
   (c/get-folder client user name)
   (catch [:status 404] _ nil)))

(defn- create-user-folder [client user name]
  (let [folder (c/add-folder client (config/grouper-user) name "")]
    (c/grant-folder-privilege client (config/grouper-user) name user :stem)
    folder))

(defn- get-or-add-folder [client user name]
  (or (get-optional-folder client user name)
      (create-user-folder client user name)))

(defn- get-collaborator-list-folder [user client]
  (let [name (c/build-folder-name client (format "users:%s:collaborator-lists" user))]
    (get-or-add-folder client user (string/replace name  #":[^:]+$" ""))
    (get-or-add-folder client user name)))

(defn- format-collaborator-list [folder collaborator-list]
  (let [regex (re-pattern (str "^\\Q" folder ":"))]
    (update-in collaborator-list [:name] (fn [s] (string/replace s regex "")))))

(def ^:private collaborator-list-group-type "group")

;; This function kind of uses a hack. A search string is required, but if we make it the
;; same as the folder name then that approximates listing all groups in the folder. An
;; update to iplant-groups will be required to eliminate this hack.
(defn get-collaborator-lists
  ([user]
   (let [client (get-client)
         folder (:name (get-collaborator-list-folder user client))]
     {:groups (mapv (partial format-collaborator-list folder) (:groups (c/find-groups client user folder)))}))
  ([user search]
   (let [client (get-client)
         folder (:name (get-collaborator-list-folder user client))]
     {:groups (mapv format-collaborator-list (c/find-groups client user search folder))})))

(defn add-collaborator-list [user {:keys [name description]}]
  (let [client (get-client)
        folder (:name (get-collaborator-list-folder user client))]
    (c/add-group client user (str folder ":" name) collaborator-list-group-type description)))
