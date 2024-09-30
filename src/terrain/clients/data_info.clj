(ns terrain.clients.data-info
  (:require [clj-http.client :as http]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]
            [cemerick.url :as url]
            [cheshire.core :as json]
            [me.raynes.fs :as fs]
            [clj-icat-direct.icat :as db]
            [clojure-commons.core :refer [remove-nil-values]]
            [clojure-commons.error-codes :as error]
            [clojure-commons.file-utils :as ft]
            [clojure-commons.assertions :as assertions]
            [slingshot.slingshot :refer [throw+ try+]]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.clients.data-info.raw :as raw]
            [terrain.services.filesystem.common-paths :as cp]
            [terrain.services.filesystem.create :as cr]
            [terrain.services.filesystem.icat :as icat]
            [terrain.services.filesystem.sharing :as sharing]
            [terrain.services.filesystem.stat :as st]
            [terrain.services.filesystem.uuids :as uuids]
            [terrain.services.filesystem.validators :as validators]
            [terrain.util.config :as cfg])
  (:import [clojure.lang IPersistentMap ISeq Keyword]
           [java.util UUID]))

(defn irods-running?
  "Determines whether or not iRODS is running."
  ^Boolean []
  (-> (raw/request :get [] {:content-type :json})
      :body
      json/decode
      (get "iRODS")))


(defn user-home-folder
  "Determines the home folder for the given user.

   Parameters:
     user - the user of the home folder.

   Returns:
     It returns the absolute path to the home folder."
  ^String [^String user]
  (cp/user-home-dir user))


(defn user-base-paths
  "Fetches the home and trash paths for the given user.

   Parameters:
     user - the user of the base paths.

   Returns:
     A map of the absolute paths to the home and trash folders."
  ^String [^String user]
  (-> (raw/base-paths user)
      :body
      (json/decode true)))


(defn uuid-for-path
  [^String user ^String path]
  (-> (raw/uuid-for-path user path)
      :body
      (json/decode true)
      :id))

(defn ensure-dir-created
  "If a folder doesn't exist, it creates the folder and makes the given user an owner of it.

   Parameters:
     user - the username of the user to become an owner of the new folder
     dir  - the absolute path to the folder"
  [^String user ^String dir]
  (cr/ensure-created user dir))

(defn read-chunk
  "Uses the data-info read-chunk endpoint."
  [params body]
  (let [path      (:path body)
        user      (st/get-public-data-user (:user params) path)
        path-uuid (uuid-for-path user path)]
    (raw/read-chunk user path-uuid (:position body) (:chunk-size body))))

(defn read-tabular-chunk
  "Uses the data-info read-tabular-chunk endpoint."
  [params body]
  (let [path      (:path body)
        user      (st/get-public-data-user (:user params) path)
        path-uuid (uuid-for-path user path)]
    (raw/read-tabular-chunk user path-uuid (:separator body) (:page body) (:chunk-size body))))

(defn manifest
  "Uses the data-info manifest endpoint."
  [params]
  (let [path      (:path params)
        user      (st/get-public-data-user (:user params) path)
        path-uuid (uuid-for-path user path)]
    (raw/manifest user path-uuid)))

(defn create-dirs
  [{:keys [user]} {:keys [paths]}]
  (validators/not-superuser user)
  (raw/create-dirs user paths))

(defn create-dir
  [params {:keys [path]}]
  (let [paths-request {:paths [path]}]
    (create-dirs params paths-request)
    (-> (st/do-stat params paths-request)
        (get-in [:paths (keyword path)]))))

(defn list-tickets
  [{:keys [user]} {:keys [paths]}]
  (-> (raw/list-tickets user paths)
      :body
      (json/decode true)))

(defn add-tickets
  [{:keys [user]} paths params]
  (-> (raw/add-tickets user paths params)
      :body
      (json/decode true)))

(defn delete-tickets
  [{:keys [user]} {:keys [tickets]}]
  (-> (raw/delete-tickets user tickets)
      :body
      (json/decode true)))

(defn check-existence
  "Uses the data-info existence-marker endpoint to query existence for a set of files/folders."
  [user paths]
  (-> (raw/check-existence user paths)
      :body
      (json/decode true)))

(defn- url-encoded?
  [string-to-check]
  (re-seq #"\%[A-Fa-f0-9]{2}" string-to-check))

(defn- url-decode
  [string-to-decode]
  (if (url-encoded? string-to-decode)
    (url/url-decode string-to-decode)
    string-to-decode))

(defn path-exists?
  [user path]
  (let [path (url-decode (ft/rm-last-slash path))]
    (-> (check-existence user [path])
        (get-in [:paths (keyword path)]))))

(defn get-or-create-dir
  "Returns the path argument if the path exists and refers to a directory.  If
   the path exists and refers to a regular file then nil is returned.
   Otherwise, a new directory is created and the path is returned."
  [path]
  (log/debug "getting or creating dir: path =" path)
  (let [user (:shortUsername current-user)]
    (cond
      (not (path-exists? user path))
      (create-dir {:user user} {:path path})

      (and (path-exists? user path) (st/path-is-dir? path))
      path

      (and (path-exists? user path) (not (st/path-is-dir? path)))
      nil

      :else
      nil)))

(defn can-create-dir?
  "Determines if a directory exists or can be created."
  [user path]
  (log/warn "checking to see if" path "can be created")
  (st/can-create-dir? user path))

(defn overwrite-file
  "Overwrite a file with a new file by path."
  [user dest istream]
  (let [path-uuid (uuid-for-path user dest)]
    (raw/overwrite-file user path-uuid istream)))

(defn rename
  "Uses the data-info set-name endpoint to rename a file within the same directory."
  [{:keys [user]} {:keys [source dest]}]
  (validators/not-superuser user)
  (assertions/assert-valid (= (ft/dirname dest) (ft/dirname source))
                           "The directory names of the source and destination must match for this endpoint.")
  (let [path-uuid (uuid-for-path user source)]
    (raw/rename user path-uuid (ft/basename dest))))

(defn move
  "Uses the data-info bulk mover endpoint to move items into a new directory."
  [{:keys [user]} {:keys [sources dest]}]
  (validators/not-superuser user)
  (raw/move-multi user sources dest))

(defn move-contents
  "Uses the data-info set-children-directory-name endpoint to move the contents of one directory
   into another directory."
  [{:keys [user]} {:keys [source dest]}]
  (validators/not-superuser user)
  (let [path-uuid (uuid-for-path user source)]
    (raw/move-contents user path-uuid dest)))

(defn delete-paths
  "Uses the data-info deleter endpoint to delete many paths."
  [{:keys [user]} {:keys [paths]}]
  (validators/not-superuser user)
  (raw/delete-paths user paths))

(defn delete-contents
  "Uses the data-info delete-children endpoint to delete the contents of a directory."
  [{:keys [user]} {:keys [path]}]
  (validators/not-superuser user)
  (let [path-uuid (uuid-for-path user path)]
    (raw/delete-contents user path-uuid)))

(defn delete-trash
  "Uses the data-info trash endpoint to empty the trash of a user."
  [params]
  (raw/delete-trash (:user params)))

(defn restore-files
  "Uses the data-info restorer endpoint to restore many or all paths."
  ([params]
   (raw/restore-files (:user params)))
  ([params body]
   (raw/restore-files (:user params) (:paths body))))

(defn collect-permissions
  "Uses the data-info permissions-gatherer endpoint to query user permissions for a set of files/folders."
  [params body]
  (raw/collect-permissions (:user params) (:paths body)))

(defn path-list-creator
  "Uses the data-info path-list-creator endpoint to create an HT Path List files for a set of file/folder paths."
  [params body]
  (raw/path-list-creator (:user params) (:paths body) params))

(def get-type-list raw/get-type-list)

(defn set-file-type
  "Uses the data-info set-type endpoint to change the type of a file."
  [params body]
  (let [path-uuid (uuid-for-path (:user params) (:path body))]
    (raw/set-file-type (:user params) path-uuid (:type body))))

(defn share-with-anonymous
  "Uses the data-info anonymizer endpoint to share paths with the anonymous user."
  [params body]
  (raw/share-with-anonymous (:user params) (:paths body)))

(defn gen-output-dir
  "Either obtains or creates a default output directory using a specified base name."
  [base]
  (first
   (remove #(nil? (get-or-create-dir %))
           (cons base (map #(str base "-" %) (iterate inc 1))))))

(defn build-path
  "Builds a path from a base path and a list of components."
  [path & components]
  (string/join
   "/"
   (cons (string/replace path #"/+$" "")
         (map #(string/replace % #"^/+|/+$" "") components))))


(defn get-metadata-json
  [user data-id]
  (:body (raw/get-avus user data-id :as :json)))

(defn stat-by-uuid
  "Resolves a stat info for the entity with a given UUID.

   Params:
     user - the user requesting the info
     uuid - the UUID

   Returns:
     It returns a path-stat map containing an additional UUID field."
  ^IPersistentMap [^String user ^UUID uuid & {:keys [filter-include filter-exclude]}]
  (-> (raw/collect-path-info user :ids [uuid] :filter-include filter-include :filter-exclude filter-exclude)
      :body
      json/decode
      (get-in ["ids" (str uuid)])
      walk/keywordize-keys))

(defn stats-by-uuids
  "Resolves the stat info for the entities with the given UUIDs. The results are not paged.

   Params:
     user   - the user requesting the info.
     uuids  - the UUIDs for the items.
     params - additional query parameters.

  Query Parameters:
     :ignore-missing      - Ignore non-existent UUIDs.
     :ignore-inaccessible - Ignore UUIDs referring to items the user lacks permission to view.
     :filter-include      - Fields to explicitly include in the response.
     :filter-exclude      - Fields to exclude from the response.

   Returns:
     It returns a path-stat map containing an additional UUID field."
  ^ISeq [user uuids params]
  (let [params (select-keys params [:ignore-missing :ignore-inaccessible :filter-include :filter-exclude])]
    (-> (http/post (raw/data-info-url "/path-info")
                   {:query-params (assoc params :user user)
                    :form-params  {:ids uuids}
                    :content-type :json
                    :as           :json})
        :body
        :ids)))

(defn stats-by-uuids-paged
  "Resolves the stat info for the entities with the given UUIDs. The results are paged.

   Params:
     user       - the user requesting the info
     sort-field - the stat field to sort on
     sort-order - the direction of the sort (asc|desc)
     limit      - the maximum number of results to return
     offset     - the number of results to skip before returning some
     uuids      - the UUIDS of interest
     info-types - This is info types to of the files to return. It may be nil, meaning return all
                  info types, a string containing a single info type, or a sequence containing a set
                  of info types.

   Returns:
     It returns a page of stat info maps."
  ^ISeq
  [^String  user
   ^String  sort-field
   ^String  sort-order
   ^Integer limit



   ^Integer offset
   ^ISeq    uuids
   info-types]
  (let [info-types (if (string? info-types) [info-types] info-types)
        page       (uuids/paths-for-uuids-paged user
                                                sort-field
                                                sort-order
                                                limit
                                                offset
                                                uuids
                                                info-types)]
    {:files   (filter #(= (:type %) :file) page)
     :folders (filter #(= (:type %) :dir) page)
     :total   (db/number-of-uuids-in-folder user (cfg/irods-zone) uuids info-types)}))


(defn uuid-accessible?
  "Indicates if a data item is readable by a given user.

   Parameters:
     user     - the authenticated name of the user
     data-id  - the UUID of the data item

   Returns:
     It returns true if the user can access the data item, otherwise false"
  ^Boolean [^String user ^UUID data-id]
  (uuids/uuid-accessible? user data-id))


(defn validate-uuid-accessible
  "Throws an exception if the given data item is not accessible to the given user.

   Parameters:
     user     - the authenticated name of the user
     data-id  - the UUID of the data item"
  [^String user ^UUID data-id]
  (when-not (uuid-accessible? user data-id)
    (throw+ {:error_code error/ERR_NOT_FOUND :uuid data-id})))


(defn resolve-data-type
  "Given filesystem id, it returns the type of data item it is, file or folder.

   Parameters:
     data-id - The UUID of the data item to inspect

   Returns:
     The type of the data item, `file` or `folder`"
  ^String [^UUID data-id]
  (icat/resolve-data-type data-id))


(defn share
  "grants access to a list of data entities for a list of users by a user

   Params:
     user        - the username of the sharing user
     share-withs - the list of usernames receiving access
     fpaths      - the list of absolute paths to the data entities being shared
     perm        - the permission being granted to the user users (read|write|own)

   Returns:
     It returns a map with the following fields:

       :user    - the list of users who actually received access
       :path    - the list of paths actually shared
       :skipped - the list of records for the things skipped, each record has the following fields:
                    :user   - the user who didn't get access
                    :path   - the path the user didn't get access to
                    :reason - the reason access wasn't granted
       :perm    - the permission that was granted"
  ^IPersistentMap [^String user ^ISeq share-withs ^ISeq fpaths ^String perm]
  (sharing/share user share-withs fpaths perm))


(defn unshare
  "Params:
     user          - the username of the user removing access
     unshare-withs - the list of usernames having access removed
     fpaths        - the list of absolute paths ot the data entities losing accessibility

   Returns:
     It returns a map with the following fields:

       :user    - the list of users who lost access
       :path    - the list of paths that lost accessibility
       :skipped - a list of records for the things skipped, each record has the following fields:
                    :user   - the user who kept access
                    :path   - the path the user still can access
                    :reason - the reason access wasn't removed"
  [^String user ^ISeq unshare-withs ^ISeq fpaths]
  (sharing/unshare user unshare-withs fpaths))


(defn- fmt-method
  [method]
  (string/upper-case (name method)))


(defn- handle-service-error
  [method url msg]
  (let [full-msg (str method " " url " had a service error: " msg)]
    (log/error full-msg)
    (assertions/request-failure full-msg)))


(defn- handle-client-error
  [method url err msg]
  (let [full-msg (str "interal error related to usage of " method " " url ": " msg)]
    (log/error err full-msg)
    (assertions/request-failure full-msg)))


(defn mk-data-path-url-path
  "This function constructs the url path to the resource backing a given data item.

   Parameters:
     path - the absolute iRODS path to the data item

   Returns:
     It returns the data-info URL path to the corresponding resource"
  ^String [^String path]
  (let [nodes (fs/split path)
        nodes (if (= "/" (first nodes)) (next nodes) nodes)]
    (str "data/path/" (string/join "/" (map url/url-encode nodes)))))


(defn respond-with-default-error
  "This function generates the default responses for errors returned from a data-info request.

   Parameters:
     status - the data-info HTTP response code
     method - the HTTP method called
     url    - the URL called
     err    - the clj-http stone wrapping the error

   Throws:
     It always throws a slingshot stone with the following fields.

       :error_code - ERR_REQUEST_FAILED
       :message    - a message describing the error"
  [^Integer status method url ^IPersistentMap err]
  (let [method (fmt-method method)
        url    (str url)]
    (case status
      400 (handle-client-error method url err "bad request")
      403 (handle-client-error method url err "user not allowed")
      404 (handle-client-error method url err "URL not found")
      405 (handle-client-error method url err "method not supported")
      406 (handle-client-error method url err "doesn't support requested content type")
      409 (handle-client-error method url err "request would conflict")
      410 (handle-client-error method url err "no longer exists")
      412 (handle-client-error method url err "provided precondition failed")
      413 (handle-client-error method url err "request body too large")
      414 (handle-client-error method url err "URL too long")
      415 (handle-client-error method url err "doesn't support request body's content type")
      422 (handle-client-error method url err "the request was not processable")
      500 (handle-service-error method url "internal error")
      501 (handle-service-error method url "not implemented")
      503 (handle-service-error method url "temporarily unavailable")
      (handle-client-error method url err "unexpected response code"))))


(defn- handle-error
  [method url err handlers]
  (let [status (:status err)]
    (if-let [handler ((keyword (str status)) handlers)]
      (handler method url err)
      (respond-with-default-error status method url err))))


(defn trapped-request
  "This function makes an HTTP request to the data-info service. It uses clj-http to make the
   request. It traps any errors and provides a response to it. A custom error handler may be
   provided for each type of error.

   The handler needs to be a function with the following signature.

     (fn [^Keyword method ^String url ^IPersistentMap err])

     method - is the unaltered method parameter passed in the the request function.
     url    - is the URL of the data-info resource.
     err    - is the clj-http stone wrapping the error response.

   Parameters:
     method         - The HTTP method (:delete|:get|:head|:options|:patch|:post|:put)
     url-path       - The path to the data info resource being accessed
     req-map        - The ring request
     error-handlers - (OPTIONAL) zero or more handlers for different error responses. They are
                      provided as named parameters where the name is a keyword based on the error
                      code. For example, :404 handle-404 would define the function handle-404 for
                      handling 404 error responses."
  [^Keyword method ^String url-path ^IPersistentMap req-map & {:as error-handlers}]
  (let [url (url/url (cfg/data-info-base-url) url-path)]
    (try+
     (raw/request method [url-path] req-map)
     (catch #(not (nil? (:status %))) err
       (handle-error method url err error-handlers)))))


(defn- data-path-url
  "Returns the URL for the path to a data item in the data store."
  [path]
  (->> (ft/rm-last-slash path)
       fs/split
       (remove (partial = "/"))
       (map url/url-encode)
       (apply url/url (cfg/data-info-base-url) "data" "path")
       str))


(defn list-folder-contents
  "Obtains a directory listing for a folder path."
  [path params]
  (:body (http/get (data-path-url path)
                   {:query-params (remove-nil-values params)
                    :accept       :json
                    :as           :json})))
