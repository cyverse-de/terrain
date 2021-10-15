(ns terrain.services.permanent-id-requests
  (:use [kameleon.uuids :only [uuidify]]
        [slingshot.slingshot :only [try+ throw+]]
        [terrain.auth.user-attributes :only [current-user]])
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clojure.data.xml :as xml]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [org.cyverse.metadata-files.datacite-4-1 :as datacite]
            [terrain.clients.async-tasks :as async-tasks-client]
            [terrain.clients.data-info :as data-info]
            [terrain.clients.data-info.raw :as data-info-client]
            [terrain.clients.datacite :as datacite-client]
            [terrain.clients.iplant-groups :as groups]
            [terrain.clients.metadata.raw :as metadata]
            [terrain.clients.notifications :as notifications]
            [terrain.util.config :as config]
            [terrain.util.email :as email]))

;; Status Codes.
(def ^:private status-code-completion "Completion")
(def ^:private status-code-failed "Failed")
(def ^:private completion-comment-fmt
"The DOI Permanent ID has been created for
%s

The dataset is available via its landing page at https://doi.org/%s.

Your dataset is now available to CyVerse users and the public.
If you need to make any changes to the dataset, including the metadata, please contact doi@cyverse.org.

If this dataset accompanies a paper, please contact us with the DOI for that paper once it is published.")

(defn- format-staging-path
  [path]
  (ft/path-join (config/permanent-id-staging-dir) (ft/basename path)))

(defn- format-publish-path
  [path]
  (ft/path-join (config/permanent-id-publish-dir) (ft/basename path)))

(defn- format-metadata-target-url
  [path]
  (str (ft/rm-last-slash (config/permanent-id-target-base-url)) (format-publish-path path)))

(defn- find-attr-value
  [avus attr]
  (->> avus
       (filter #(= attr (:attr %)))
       first
       :value))

(defn- validate-datacite-metadata
  [avus]
  (when (empty? avus)
    (throw+ {:type :clojure-commons.exception/bad-request
             :error "No metadata found for Permanent ID Request."}))
  (let [identifier (find-attr-value avus (config/permanent-id-identifier-attr))]
    (when-not (empty? identifier)
      (throw+ {:type :clojure-commons.exception/bad-request
               :error "The metadata already contains a Permanent Identifier attribute with a value."
               :attribute (config/permanent-id-identifier-attr)
               :identifier identifier})))
  avus)

(defn- validate-request-type
  [request-type]
  (when-not (= "DOI" (string/upper-case request-type))
    (throw+ {:type         :clojure-commons.exception/bad-request-field
             :error        (str "Permanent ID Request type '"
                                request-type
                                "' not supported by this service.")
             :request-type request-type})))

(defn- validate-request-for-completion
  [{:keys [folder original_path permanent_id type]}]
  (validate-request-type type)
  (when (empty? folder)
    (throw+ {:type :clojure-commons.exception/not-found
             :error "Folder not found."
             :path original_path}))
  (when-not (empty? permanent_id)
    (throw+ {:type :clojure-commons.exception/bad-request
             :error "This Request appears to be completed, since it already has a Permanent ID."
             :permanent-id permanent_id})))

(defn- validate-request-target-type
  "Validate the target is a folder. Needs the :type key."
  [{target-type :type :as folder}]
  (let [target-type (metadata/resolve-data-type target-type)]
    (when-not (= target-type "folder")
      (throw+ {:type :clojure-commons.exception/bad-request-field
               :error "The given data ID does not belong to a folder."
               :file folder}))
    target-type))

(defn- validate-owner
  "Validate ownership. Needs only id and permission"
  [user {:keys [id permission] :as data-item}]
  (when-not (= (keyword permission) :own)
    (throw+ {:type :clojure-commons.exception/not-owner
             :error "User does not own given folder."
             :user user
             :folder-id id}))
  data-item)

(defn- validate-folder-not-empty
  "Validate non-empty. Needs file-count and dir-count"
  [{:keys [dir-count file-count] :as folder}]
  (when-not (and dir-count file-count (<= 1 (+ dir-count file-count)))
    (throw+ {:type :clojure-commons.exception/bad-request-field
             :error "The given folder appears to be empty."
             :folder folder}))
  folder)

(defn- validate-staging-dest-available
  [{:keys [paths]} staging-dest src-folder]
  (let [path-exists? (get paths (keyword staging-dest))]
    (when (and path-exists? (not= staging-dest src-folder))
    (throw+ {:type :clojure-commons.exception/exists
             :error "A folder with this name has already been submitted for a Permanent ID request."
             :path staging-dest}))))

(defn- validate-publish-dest-available
  [{:keys [paths]} publish-dest src-folder]
  (let [path-exists? (get paths (keyword publish-dest))]
    (when (and path-exists? (not= publish-dest src-folder))
      (throw+ {:type :clojure-commons.exception/exists
               :error "A folder with this name has already been published."
               :path publish-dest}))))

(defn- validate-publish-dest
  [{:keys [path] :as data-item}]
  (let [publish-dest (format-publish-path path)
        paths-exist (data-info/check-existence (config/permanent-id-curators-group)
                                               [publish-dest])]
    (validate-publish-dest-available paths-exist publish-dest path))
  data-item)

(defn- validate-data-item
  "Validate then return a data item. Needs: path"
  [user {:keys [path] :as data-item}]
  (validate-owner user data-item)
  (validate-request-target-type data-item)
  (validate-folder-not-empty data-item)
  (let [staging-dest (format-staging-path path)
        publish-dest (format-publish-path path)
        paths-exist  (data-info/check-existence (config/permanent-id-curators-group)
                                                [staging-dest
                                                 publish-dest])]
    (validate-staging-dest-available paths-exist staging-dest path)
    (validate-publish-dest-available paths-exist publish-dest path))
  data-item)

(defn- submit-permanent-id-request
  "Submits the request to the metadata create-permanent-id-request endpoint."
  [type folder-id target-type path]
  (metadata/create-permanent-id-request
    {:type          type
     :target_id     folder-id
     :target_type   target-type
     :original_path path}))

(defn- create-publish-dir
  "Creates the Permanent ID Requests publish directory, if it doesn't already exist."
  []
  (let [publish-path (config/permanent-id-publish-dir)
        curators     (config/permanent-id-curators-group)]
    (when-not (data-info/path-exists? (config/irods-user) publish-path)
      (log/warn "creating" publish-path "for:" curators)
      (data-info-client/create-dirs (config/irods-user) [publish-path])
      (data-info/share (config/irods-user) [curators] [publish-path] "own"))))

(defn- create-staging-dir
  "Creates the Permanent ID Requests staging directory, if it doesn't already exist."
  []
  (let [staging-path (config/permanent-id-staging-dir)
        curators     (config/permanent-id-curators-group)]
    (when-not (data-info/path-exists? (config/irods-user) staging-path)
      (log/warn "creating" staging-path "for:" curators)
      (data-info-client/create-dirs (config/irods-user) [staging-path])
      (data-info/share (config/irods-user) [curators] [staging-path] "own"))))

(defn- move-folder
  [requesting-user user {:keys [id path]} dest-path]
  (try+
   (-> (data-info-client/move-single user id dest-path)
       :body
       (json/decode true)
       :async-task-id)
   (catch Object e
     (log/error e)
     (email/send-permanent-id-request-data-move-error path dest-path requesting-user (:body e (str e)))
     nil)))


(defn- async-move-poller
  [requesting-user {:keys [id path]} dest-path async-task-id post-move-fn]
  (try+
    (log/info "Waiting for async move to complete for" id path)

    (loop [sleep-seconds 1]
      (Thread/sleep (* sleep-seconds 1000))

      (let [{:keys [statuses end_date]} (async-tasks-client/get-by-id async-task-id)
            last-status (-> statuses last :status)]
        (when (and end_date (not= last-status "completed"))
          (log/warn "Async folder move not completed successfully:" last-status))

        (if end_date
          ;; Async move completed.
          (post-move-fn (:path (data-info/stat-by-uuid (config/irods-user)
                                                       id
                                                       :filter-include "path")))
          ;; Else keep waiting for up to wait-seconds-max.
          ;; Since the sleep seconds are doubled each time,
          ;; then this thread has already been waiting
          ;; ((sleep-seconds * 2) - 1) number of seconds.
          (if (< (dec (* sleep-seconds 2)) (config/permanent-id-async-move-wait-seconds-max))
            (recur (* sleep-seconds 2))
            (throw+ (str "Async move took too long to complete successfully. Last status: "
                         last-status))))))

    (catch Object e
      (log/error e)
      (email/send-permanent-id-request-data-move-error path
                                                       dest-path
                                                       requesting-user
                                                       (:body e (str e))))))

(defn- move-data-item-to-staging
  [{:keys [commonName shortUsername]} {:keys [path] :as folder}]
  (let [curators-group (config/permanent-id-curators-group)
        async-task-id  (move-folder commonName
                                    (config/irods-user)
                                    folder
                                    (config/permanent-id-staging-dir))
        share-fn       (fn [staged-path]
                         (data-info/share (config/irods-user) [curators-group] [staged-path] "own")
                         (data-info/share (config/irods-user) [shortUsername] [staged-path] "write"))
        share-poller   #(async-move-poller commonName
                                           folder
                                           (config/permanent-id-staging-dir)
                                           async-task-id
                                           share-fn)]

    (when async-task-id
      (async-tasks-client/run-async-thread async-task-id
                                           share-poller
                                           "permanent-id-move-staging"))
    (format-staging-path path)))

(defn- stage-data-item
  [requesting-user {:keys [path] :as folder}]
  (let [staged-path (format-staging-path path)]
    (when (not= path staged-path)
      (move-data-item-to-staging requesting-user folder))
    staged-path))

(defn- move-data-item-to-published
  [{:keys [commonName]} {:keys [path] :as folder}]
  (let [curators-group (config/permanent-id-curators-group)
        async-task-id  (move-folder commonName
                                    (config/irods-user)
                                    folder
                                    (config/permanent-id-publish-dir))
        share-fn       (fn [publish-path]
                         (data-info/share (config/irods-user) [curators-group] [publish-path] "own"))
        share-poller   #(async-move-poller commonName
                                           folder
                                           (config/permanent-id-publish-dir)
                                           async-task-id
                                           share-fn)]

    (when async-task-id
      (async-tasks-client/run-async-thread async-task-id
                                           share-poller
                                           "permanent-id-move-publish"))
    (format-publish-path path)))

(defn- publish-data-item
  [requesting-user {:keys [path] :as folder}]
  (let [publish-path (format-publish-path path)]
    (when (not= path publish-path)
      (move-data-item-to-published requesting-user folder))
    publish-path))

(defn- publish-metadata
  [{:keys [id type]} publish-avus]
  (let [data-type (metadata/resolve-data-type type)]
    (metadata/update-avus data-type id publish-avus)))

(defn- send-notification
  [user email subject contents request-id]
  (log/debug "sending permanent_id_request notification to" user ":" subject)
  (try
    (notifications/send-notification
      {:type "permanent_id_request"
       :user user
       :subject subject
       :email true
       :email_template "blank"
       :payload {:email_address email
                 :contents      contents
                 :uuid          request-id}})
    (catch Exception e
      (log/error e
        "Could not send permanent_id_request (" request-id ") notification to" user ":" subject))))

(defn- send-update-notification
  [{:keys [id type folder history] {:keys [username email]} :requested_by}]
  (let [{:keys [status comments]} (last history)
        folder-name (if folder (ft/basename (:path folder)) "unknown")
        subject (str type " Request for " folder-name " Status Changed to " status)]
    (send-notification username email subject comments id)))

(defn- format-avus
  [{:keys [avus irods-avus]}]
  (concat avus irods-avus [{:attr (config/permanent-id-date-attr) :value (str (time/year (time/now)))}]))

(defn- append-placeholder-identifier
  "Appends a placeholder `identifier` AVU to the given metadata,
   with a value such as `10.33540/placeholder`,
   to allow the generated DataCite XML to pass validation."
  [request-type avus]
  (conj avus {:attr  (config/permanent-id-identifier-attr)
              :value (str (config/datacite-doi-prefix) "/placeholder")
              :unit  ""
              :avus  [{:attr  (config/permanent-id-identifier-type-attr)
                       :value request-type
                       :unit  ""}]}))

(defn- parse-valid-datacite-metadata
  [request-type avus]
  (validate-datacite-metadata avus)
  (->> avus
       (append-placeholder-identifier request-type)
       datacite/build-datacite
       xml/emit-str))

(defn- get-validated-data-item
  "Gets data-info stat for the given ID and checks if the data item is valid for a Permanent ID request.
  Should filter the stat to what's needed for validation and by callers."
  [user request-type data-id]
  (let [data-item (->> (data-info/stat-by-uuid user
                                               data-id
                                               :filter-include "path,id,type,permission,file-count,dir-count")
                       (validate-data-item user))
        metadata (data-info/get-metadata-json user data-id)]
    (parse-valid-datacite-metadata request-type (format-avus metadata))
    data-item))

(defn- format-publish-avus
  "Formats AVUs containing completed request information for saving with the metadata service."
  [avus identifier identifier-type]
  (let [publish-date (find-attr-value avus (config/permanent-id-date-attr))]
    {:avus
     [{:attr  (config/permanent-id-identifier-attr)
       :value identifier
       :unit  ""
       :avus  [{:attr  (config/permanent-id-identifier-type-attr)
                :value identifier-type
                :unit  ""}]}
      {:attr  (config/permanent-id-date-attr)
       :value publish-date
       :unit  ""}]}))

(defn- format-perm-id-req-response
  [user path-info-for {:keys [target_id] :as response}]
  (-> response
      (dissoc :target_id :target_type)
      (assoc :folder (path-info-for (keyword target_id)))))

(defn- format-requested-by
  [user {:keys [requested_by target_id] :as permanent-id-request}]
  (if-let [user-info (groups/lookup-subject user requested_by)]
    (assoc permanent-id-request :requested_by (groups/format-like-trellis user-info))
    permanent-id-request))

(defn- format-permanent-id-request-details
  [user permanent-id-request]
  (let [uuid          (:target_id permanent-id-request)
        path-info-for (data-info/stats-by-uuids user [uuid] {:ignore-missing true :ignore-inaccessible true})]
    (->> permanent-id-request
         (format-perm-id-req-response user path-info-for)
         (format-requested-by user))))

(defn- format-perm-id-req-list
  [requests]
  (let [user          (:shortUsername current-user)
        uuids         (map :target_id requests)
        path-info-for (data-info/stats-by-uuids user uuids {:ignore-missing true :ignore-inaccessible true})]
    (map
     (partial format-perm-id-req-response user path-info-for)
     requests)))

(defn list-permanent-id-requests
  [params]
  (-> (metadata/list-permanent-id-requests params)
      (update :requests format-perm-id-req-list)))

(defn create-permanent-id-request
  [{type :type folder-id :folder}]
  (create-staging-dir)
  (let [user                           (:shortUsername current-user)
        {:keys [path] :as folder}      (get-validated-data-item user type folder-id)
        target-type                    (validate-request-target-type folder)
        {request-id :id :as response}  (submit-permanent-id-request type folder-id target-type path)
        staged-path                    (stage-data-item current-user folder)]
    (send-notification
      user
      (:email current-user)
      (str type " Request Submitted for " (ft/basename path))
      nil
      request-id)
    (email/send-permanent-id-request-new type staged-path current-user)
    (email/send-permanent-id-request-submitted type staged-path current-user)
    (format-permanent-id-request-details user response)))

(defn list-permanent-id-request-status-codes
  []
  (metadata/list-permanent-id-request-status-codes))

(defn list-permanent-id-request-types
  []
  (metadata/list-permanent-id-request-types))

(defn get-permanent-id-request
  [request-id]
  (->> (metadata/get-permanent-id-request request-id)
       (format-permanent-id-request-details (:shortUsername current-user))))

(defn admin-list-permanent-id-requests
  [params]
  (-> (metadata/admin-list-permanent-id-requests params)
      (update :requests format-perm-id-req-list)))

(defn admin-get-permanent-id-request
  [request-id]
  (->> (metadata/admin-get-permanent-id-request request-id)
       (format-permanent-id-request-details (:shortUsername current-user))))

(defn update-permanent-id-request
  [request-id body]
  (let [response (->> (metadata/update-permanent-id-request request-id body)
                      (format-permanent-id-request-details (:shortUsername current-user)))]
    (send-update-notification response)
    response))

(defn- complete-permanent-id-request
  [user {request-id :id :keys [folder type] :as request}]
  (validate-request-for-completion request)
  (try+
    (let [folder          (validate-publish-dest folder)
          folder-id       (uuidify (:id folder))
          metadata        (data-info/get-metadata-json user folder-id)
          avus            (format-avus metadata)
          target-url      (format-metadata-target-url (:path folder))
          datacite-xml    (parse-valid-datacite-metadata type avus)
          doi-response    (datacite-client/create-doi datacite-xml target-url)
          identifier      (get-in doi-response [:data :id])
          publish-avus    (format-publish-avus avus identifier type)
          publish-path    (publish-data-item current-user folder)]
      (email/send-permanent-id-request-complete type
                                                publish-path
                                                (json/encode doi-response {:pretty true})
                                                identifier)
      (publish-metadata folder publish-avus)
      [identifier publish-path])
    (catch Object e
      (log/error e)
      (update-permanent-id-request request-id {:status status-code-failed})
      (throw+ e))))

(defn create-permanent-id
  [request-id]
  (create-publish-dir)
  (let [{request-type :type :as request} (admin-get-permanent-id-request request-id)
        [identifier publish-path]        (complete-permanent-id-request (:shortUsername current-user) request)
        comments                         (if (= "DOI" request-type)
                                           (format completion-comment-fmt publish-path identifier)
                                           identifier)]
    (update-permanent-id-request request-id {:status       status-code-completion
                                             :comments     comments
                                             :permanent_id identifier})))

(defn preview-datacite-xml
  [request-id]
  (let [user      (:shortUsername current-user)
        {:keys [folder type]} (admin-get-permanent-id-request request-id)
        folder-id (uuidify (:id folder))
        metadata  (data-info/get-metadata-json user folder-id)
        avus      (format-avus metadata)]
    (parse-valid-datacite-metadata type avus)))
