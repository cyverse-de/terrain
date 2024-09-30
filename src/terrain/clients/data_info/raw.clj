(ns terrain.clients.data-info.raw
  (:require [clojure.tools.logging :as log]
            [cemerick.url :as url]
            [me.raynes.fs :as fs]
            [medley.core :refer [remove-vals]]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [terrain.util.config :as cfg])
  (:import [clojure.lang IPersistentMap ISeq Keyword]))

;; HELPER FUNCTIONS

(defn- resolve-http-call
  [method]
  (case method
    :delete   http/delete
    :get      http/get
    :head     http/head
    :options  http/options
    :patch    http/patch
    :post     http/post
    :put      http/put))

(defn data-info-url
  [& url-path]
  (str (apply url/url (cfg/data-info-base-url) url-path)))

(defn- get-options
  ([]
   (get-options {}))
  ([params]
   {:query-params params
    :as           :json})
  ([user params]
   (get-options (assoc params :user user))))

(defn- put-options
  ([user body]
   (put-options user body {}))
  ([user body params]
   {:form-params  body
    :query-params (assoc params :user user)
    :content-type :json
    :as           :json}))

(defn request
  "This function makes an HTTP request to the data-info service. It uses clj-http to make the
   request."
  [^Keyword method ^ISeq url-path ^IPersistentMap req-map]
  (let [url (apply url/url (cfg/data-info-base-url) url-path)]
    ((resolve-http-call method) (str url) req-map)))

(defn- add-user
  [req-map user]
  (assoc-in req-map [:query-params :user] user))

(defn- add-body
  [req-map body]
  (assoc req-map :body body))

(defn- add-extra-params
  [req-map extra-params]
  (assoc req-map
         :query-params (merge (get req-map :query-params {})
                              extra-params)))

(defn- mk-req-map
  ([]
    {:content-type :json})
  ([user]
    (add-user (mk-req-map) user))
  ([user item]
    (-> (if (= (type item) String)
          (add-body (mk-req-map) item)
          (add-extra-params {} item))
        (add-user user)))
  ([user body extra-params]
    (-> (add-user (mk-req-map) user)
        (add-body body)
        (add-extra-params extra-params))))

;; NAVIGATION

(defn base-paths
  "Uses the data-info navigation/base-paths endpoint to list a user's home and trash paths."
  [user]
  (request :get ["navigation" "base-paths"] (mk-req-map user)))

(defn list-roots
  "Uses the data-info navigation/root endpoint to list a user's navigation roots."
  [user]
  (request :get ["navigation" "root"] (mk-req-map user)))

(defn- mk-nav-url
  [path]
  (let [nodes         (fs/split path)
        nodes         (if (= "/" (first nodes)) (next nodes) nodes)
        encoded-nodes (map url/url-encode nodes)]
    (apply url/url (cfg/data-info-base-url) "navigation" "path" encoded-nodes)))

(defn list-directories
  "Uses the data-info navigation/path endpoint to list directories contained under path."
  [user path]
  (http/get (str (mk-nav-url path)) (mk-req-map user)))

;; READ

(defn read-chunk
  "Uses the data-info read-chunk endpoint."
  [user path-uuid position chunk-size]
  (request :get ["data" path-uuid "chunks"]
           (mk-req-map user
                  {:position position
                   :size chunk-size})))

(defn read-tabular-chunk
  "Uses the data-info read-tabular-chunk endpoint."
  [user path-uuid separator page chunk-size]
  (request :get ["data" path-uuid "chunks-tabular"]
           (mk-req-map user
                  {:separator separator
                   :page page
                   :size chunk-size})))

(defn manifest
  "Uses the data-info manifest endpoint."
  [user path-uuid]
  (request :get ["data" path-uuid "manifest"]
           (mk-req-map user)))

;; CREATE

(defn upload-file
  [user dest-path filename content-type istream]
  (:body (http/post (str (url/url (cfg/data-info-base-url) "data"))
                    {:query-params {:user user
                                    :dest dest-path}
                     :accept       :json
                     :as           :json
                     :multipart    [{:part-name "file"
                                     :name      filename
                                     :mime-type content-type
                                     :content   istream}]})))
(defn create-dirs
  "Uses the data-info directories endpoint to create several directories."
  [user paths]
  (request :post ["data" "directories"]
           (mk-req-map user (json/encode {:paths paths}))))

;; MODIFY

(defn overwrite-file
  [user path-uuid istream]
  (:body (http/put (str (url/url (cfg/data-info-base-url) "data" path-uuid))
                   {:query-params {:user user}
                    :accept       :json
                    :as           :json
                    :multipart    [{:name    "file"
                                    :content istream}]})))

;; MOVE AND RENAME

(defn rename
  "Uses the data-info set-name endpoint to rename a file within the same directory."
  [user path-uuid new-name]
  (request :put ["data" path-uuid "name"]
           (mk-req-map user (json/encode {:filename new-name}))))

(defn move-single
  "Uses the data-info single-item directory change endpoint to move an item to a different directory."
  [user path-uuid dest]
  (log/info (str "using move-single to move data item " path-uuid " to " dest))
  (request :put ["data" path-uuid "dir"]
           (mk-req-map user (json/encode {:dirname dest}))))

(defn move-multi
  "Uses the data-info bulk mover endpoint to move a number of items to a different directory."
  [user sources dest]
  (log/info (str "using move-multi to move several data items to " dest))
  (request :post ["mover"]
           (mk-req-map user (json/encode {:sources sources :dest dest}))))

(defn move-contents
  "Uses the data-info set-children-directory-name endpoint to move the contents of one directory
   into another directory."
  [user path-uuid dest]
  (request :put ["data" path-uuid "children" "dir"]
           (mk-req-map user (json/encode {:dirname dest}))))

;; DELETION

(defn delete-paths
    "Uses the data-info deleter endpoint to delete many paths."
    [user paths]
    (request :post ["deleter"]
             (mk-req-map user (json/encode {:paths paths}))))

(defn delete-data-item
  "Uses the data-info delete by data ID endpoint to delete a single data item."
  [user data-id]
  (request :delete ["data" data-id]
           (mk-req-map user)))

(defn delete-contents
    "Uses the data-info delete-children endpoint to delete the contents of a directory."
    [user path-uuid]
    (request :delete ["data" path-uuid "children"]
             (mk-req-map user)))

(defn delete-trash
    "Uses the data-info trash endpoint to empty the trash of a user."
    [user]
    (request :delete ["trash"] (mk-req-map user)))

(defn restore-files
    "Uses the data-info restorer endpoint to restore many or all paths."
    ([user]
     (restore-files user []))
    ([user paths]
     (request :post ["restorer"]
              (mk-req-map user (json/encode {:paths paths})))))

;; METADATA

(defn get-avus
  "Get the set of iRODS AVUs for a data item."
  [user path-uuid & {:keys [as] :or {as :stream}}]
  (request :get ["data" path-uuid "metadata"]
           (assoc (mk-req-map user)
                  :as as)))

(defn admin-get-avus
  "Get the set of iRODS AVUs, including administrative AVUs, for a data-item."
  [user path-uuid]
  (request :get ["admin" "data" path-uuid "metadata"]
           (assoc (mk-req-map user)
                  :as :json)))

(defn set-avus
  "Set the iRODs AVUs to a specific set."
  [user path-uuid avu-map]
  (request :put ["data" path-uuid "metadata"]
           (mk-req-map user (json/encode avu-map))))

(defn add-avus
  "Add AVUs to a data item."
  [user path-uuid avu-map]
  (request :patch ["data" path-uuid "metadata"]
           (mk-req-map user (json/encode avu-map))))

(defn metadata-copy
  [user path-uuid copy-request]
  (request :post ["data" path-uuid "metadata" "copy"]
           (mk-req-map user (json/encode copy-request))))

(defn metadata-csv-parser
  [user path-uuid params]
  (request :post ["data" path-uuid "metadata" "csv-parser"]
           (mk-req-map user
                       (remove-vals nil? (select-keys params [:src :separator])))))

(defn admin-add-avus
  "Add AVUs, allowing administrative AVUs to be included, for a data item."
  [user path-uuid avu-map]
  (request :patch ["admin" "data" path-uuid "metadata"]
           (mk-req-map user (json/encode avu-map))))

(defn save-metadata
  "Request that metadata be saved to a file."
  [user path-uuid dest recursive]
  (request :post ["data" path-uuid "metadata" "save"]
           (mk-req-map user (json/encode {:dest dest :recursive recursive}))))

(defn save-ore
  "Request that an OAI-ORE file be saved for a directory."
  [user path-uuid]
  (request :post ["data" path-uuid "ore" "save"]
           (mk-req-map user)))

;; SHARING

(defn share-with-anonymous
  "Share a list of paths with the anonymous user."
  [user paths]
  (request :post ["anonymizer"]
           (mk-req-map user (json/encode {:paths paths}))))

;; TICKETS

(defn list-tickets
  "List tickets for a list of paths."
  [user paths]
  (request :post ["ticket-lister"]
           (mk-req-map user (json/encode {:paths paths}))))

(defn add-tickets
  "Create potentially-public tickets for a list of paths."
  [user paths params]
  (request :post ["tickets"]
           (mk-req-map user (json/encode {:paths paths}) params)))

(defn delete-tickets
  "Delete tickets for a list of ticket IDs"
  [user ticket-ids]
  (request :post ["ticket-deleter"]
           (mk-req-map user (json/encode {:tickets ticket-ids}))))

;; MISC

(defn uuid-for-path
  "Uses the data-info uuid-for-path endpoint to resolve a path to a UUID"
  [user path]
  (request :get ["data" "uuid"]
           (mk-req-map user {:path path})))

(defn collect-permissions
  "Uses the data-info permissions-gatherer endpoint to query user permissions for a set of files/folders."
  [user paths]
  (request :post ["permissions-gatherer"]
           (mk-req-map user (json/encode {:paths paths}))))

(defn collect-stats
  "Uses the data-info stat-gatherer endpoint to gather stat information for a set of files/folders."
  [user {:keys [paths ids]}]
  (request :post ["stat-gatherer"]
           (mk-req-map user
                       (json/encode (remove-vals nil? {:paths paths :ids ids})))))

(defn collect-path-info
  "Uses the data-info path-info endpoint to gather stat information for a set of files/folders."
  [user & {:keys [paths ids filter-include filter-exclude]}]
  (request :post ["path-info"]
           (mk-req-map user
                       (json/encode (remove-vals nil? {:paths paths :ids ids}))
                       (remove-vals nil? {:filter-include filter-include :filter-exclude filter-exclude}))))

(defn check-existence
  "Uses the data-info existence-marker endpoint to gather existence information for a set of files/folders."
  [user paths]
  (request :post ["existence-marker"]
           (mk-req-map user (json/encode {:paths paths}))))

(defn get-type-list
  "Uses the data-info file-types endpoint to produce a list of acceptable types."
  []
  (:body (http/get (data-info-url "file-types") (get-options))))

(defn set-file-type
  "Uses the data-info set-type endpoint to change the type of a file."
  [user path-uuid type]
  (:body (http/put (data-info-url "data" path-uuid "type")
                   (put-options user {:type type}))))

(defn path-list-creator
  "Uses the data-info path-list-creator endpoint to create an HT Path List files for a set of file/folder paths."
  [user paths params]
  (request :post ["path-list-creator"]
           (mk-req-map user
                       (json/encode {:paths paths})
                       (select-keys params [:dest
                                            :path-list-info-type
                                            :name-pattern
                                            :info-type
                                            :folders-only
                                            :recursive]))))
