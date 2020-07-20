(ns terrain.services.filesystem.directory
  (:use [clojure-commons.core :only [remove-nil-values]]
        [clojure-commons.validators]
        [kameleon.uuids :only [uuidify]]
        [ring.util.http-response :only [ok]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [cheshire.core :as json]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [me.raynes.fs :as fs]
            [clojure-commons.error-codes :as error]
            [terrain.clients.data-info :as data]
            [terrain.clients.data-info.raw :as data-raw]
            [terrain.services.metadata.favorites :as favorites]
            [terrain.util.config :as cfg]
            [terrain.util.validators :as duv]
            [terrain.services.filesystem.common-paths :as paths]
            [ring.util.http-response :as response]))

(defn- is-favorite?
  [favorite-ids id]
  (contains? favorite-ids (uuidify id)))

(defn- lookup-favorite-ids
  "Filters the list of given data IDs, returning those marked as favorites by the user according to
  the metadata filter-favorites service. If the filtered list of favorite IDs cannot be retrieved,
  an empty list is returned instead."
  [user data-ids]
  (if user
    (try+
     (favorites/filter-favorites data-ids)
     (catch Object e
       (log/error e "Could not lookup favorites in directory listing")
       []))
    []))

(defn- bad-paths
  "Returns a seq of full paths that should not be included in paged listing."
  [user]
  (remove nil? [(cfg/fs-community-data)
                (when user (ft/path-join (cfg/irods-home) user))
                (ft/path-join (cfg/irods-home) "public")]))

(defn- is-bad?
  "Returns true if the map is okay to include in a directory listing."
  [user path-to-check]
  (let [fpaths (set (concat (cfg/fs-bad-names) (bad-paths user)))]
    (or  (contains? fpaths path-to-check)
         (not (paths/valid-path? path-to-check)))))


(defn- fmt-folder
  [user favorite-ids {:keys [id
                             path
                             label
                             permission
                             date-created
                             date-modified]}]
  {:id            id
   :path          path
   :label         label
   :permission    permission
   :date-created  date-created
   :date-modified date-modified
   :isFavorite    (is-favorite? favorite-ids id)
   :badName       (or (is-bad? user path)
                      (is-bad? user (fs/base-name path)))
   :hasSubDirs    true})


(defn- fmt-dir-resp
  [{:keys [id folders] :as data-resp} user]
  (let [favorite-ids (->> folders
                          (map :id)
                          (concat [id])
                          (lookup-favorite-ids user))]
    (assoc (fmt-folder user favorite-ids data-resp)
      :folders (map (partial fmt-folder user favorite-ids) folders))))


(defn- list-directories
  "Lists the directories contained under path."
  [user path]
    (-> (data-raw/list-directories user path)
      :body
      (json/decode true)
      :folder
      (fmt-dir-resp user)))


(defn- top-level-listing
  [{user :user}]
  (let [comm-f     (future (list-directories user (cfg/fs-community-data)))
        share-f    (future (list-directories user (cfg/irods-home)))
        home-f     (future (list-directories user (paths/user-home-dir user)))]
    {:roots [@home-f @comm-f @share-f]}))

(defn- shared-with-me-listing?
  [path]
  (= (ft/add-trailing-slash path) (ft/add-trailing-slash (cfg/irods-home))))


(defn do-directory
  [{:keys [user path] :or {path nil} :as params}]
  (cond
    (nil? path)
    (top-level-listing params)

    (shared-with-me-listing? path)
    (list-directories user (cfg/irods-home))

    :else
    (list-directories user path)))

(with-pre-hook! #'do-directory
  (fn [params]
    (paths/log-call "do-directory" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-directory (paths/log-func "do-directory"))


(defn- format-data-item
  [user favorite-ids data-item]
  (let [id   (:id data-item)
        path (:path data-item)]
    {:id            id
     :path          path
     :label         (paths/id->label user path)
     :infoType      (:infoType data-item)
     :isFavorite    (is-favorite? favorite-ids id)
     :badName       (:badName data-item)
     :permission    (:permission data-item)
     :date-created  (:dateCreated data-item)
     :date-modified (:dateModified data-item)
     :file-size     (:size data-item)}))


(defn- format-page
  [user {:keys [id files folders total totalBad] :as page}]
  (let [file-ids (map :id files)
        folder-ids (map :id folders)
        favorite-ids (lookup-favorite-ids user (concat file-ids folder-ids [id]))]
    (assoc (format-data-item user favorite-ids page)
      :hasSubDirs true
      :files      (map (partial format-data-item user favorite-ids) files)
      :folders    (map (partial format-data-item user favorite-ids) folders)
      :total      total
      :totalBad   totalBad)))


(defn- fix-paged-listing-params
  [user params]
  (remove-nil-values
   (merge (dissoc params :sort-col)
          {:user       (or user "anonymous")
           :bad-chars  (cfg/fs-bad-chars)
           :bad-name   (cfg/fs-bad-names)
           :bad-path   (bad-paths user)
           :sort-field ((some-fn :sort-field :sort-col) params)})))

(defn do-paged-listing
  "Entrypoint for the API that calls (paged-dir-listing)."
  [{user :shortUsername} {:keys [path] :as params}]
  (if-let [listing-user (if (= path (cfg/fs-community-data))
                          (or user "anonymous")
                          user)]
    (let [params (dissoc params :path)]
      (->> (fix-paged-listing-params listing-user params)
           (data/list-folder-contents path)
           (format-page user)
           (ok)))
    (response/unauthorized "No authentication information found in the request")))
