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

(defn- create-folder [client user name]
  (c/add-folder client (config/grouper-user) name "")
  (c/grant-folder-privilege client (config/grouper-user) name user :stem)
  nil)

(defn- ensure-folder-exists [client user name]
  (try+
   (c/get-folder client user name)
   (catch [:status 404] _
     (create-folder client user name)))
  nil)

(def ^:private collaborator-list-group-type "group")

(defn- get-user-folder-name [client user]
  (c/build-folder-name client (format "users:%s" user)))

(defn- get-collaborator-list-folder-name [client user]
  (c/build-folder-name client (format "users:%s:collaborator-lists" user)))

(defn- ensure-collaborator-list-folder-exists [client user]
  (ensure-folder-exists client user (get-user-folder-name client user))
  (ensure-folder-exists client user (get-collaborator-list-folder-name client user)))

(defn- format-collaborator-list [folder collaborator-list]
  (let [regex (re-pattern (str "^\\Q" folder ":"))]
    (update-in (dissoc collaborator-list :detail) [:name] (fn [s] (string/replace s regex "")))))

(defn- get-collaborator-lists* [client user lookup-fn]
  (ensure-collaborator-list-folder-exists client user)
  (let [folder (get-collaborator-list-folder-name client user)]
    {:groups (mapv (partial format-collaborator-list folder) (:groups (lookup-fn folder)))}))

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
         (format-collaborator-list folder))))
