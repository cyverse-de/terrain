(ns terrain.services.sharing
  (:require [clojure.tools.logging :as log]
            [clojure.string :refer [join]]
            [clojure.walk :refer [walk]]
            [clojure-commons.file-utils :refer [basename]]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.clients.data-info :as data]
            [terrain.clients.notifications :as dn]))

(def file-list-threshold 10)

(defn- share-list->path-list
  "Converts a list of maps with path key-values to a list of path strings."
  [paths]
  (reduce #(conj %1 (:path %2)) [] paths))

(defn- path-list->file-list
  "Returns a string that joins the given path list by commas."
  [path-list]
  (join ", " (map basename path-list)))

(defn- send-sharing-notification
  "Sends an (un)sharing notification."
  [user subject message action path-list error-message]
  (log/debug "sending sharing notification to" user ":" subject)
  (try
    (dn/send-notification {:type "data"
                           :user user
                           :subject subject
                           :message message
                           :payload {:action action
                                     :paths path-list}})
    (catch Exception e
      (log/warn e error-message))))

(defn- send-share-notifications
  "Sends share notifications to both the current user and shared user."
  [sharee shares]
  (let [sharer (:shortUsername current-user)
        path-list (share-list->path-list shares)
        share-count (count path-list)
        file-list (path-list->file-list path-list)
        sharer-summary (str share-count
                            " file(s)/folder(s) have been shared with "
                            sharee)
        sharer-notification (if (< share-count file-list-threshold)
                              (str "The following file(s)/folder(s) have been shared with "
                                   sharee ": "
                                   file-list)
                              sharer-summary)
        sharee-summary (str sharer
                            " has shared "
                            share-count
                            " file(s)/folder(s) with you.")
        sharee-notification (if (< share-count file-list-threshold)
                              (str sharer
                                   " has shared the following file(s)/folder(s) with you: "
                                   file-list)
                              sharee-summary)]
    (send-sharing-notification
     sharer
     sharer-summary
     sharer-notification
     "share"
     path-list
     (str "unable to send share notification to " sharer " for " sharee))
    (send-sharing-notification
     sharee
     sharee-summary
     sharee-notification
     "share"
     path-list
     (str "unable to send share notification from " sharer " to " sharee))))

(defn- send-share-err-notification
  "Sends a share error notification to the current user."
  [sharee shares]
  (let [path-list (share-list->path-list shares)
        share-count (count path-list)
        file-list (path-list->file-list path-list)
        subject (str share-count
                     " file(s)/folder(s) could not be shared with "
                     sharee)
        notification (if (< share-count file-list-threshold)
                       (str "The following file(s)/folder(s) could not be shared with "
                            sharee ": "
                            file-list)
                       subject)]
    (send-sharing-notification
     (:shortUsername current-user)
     subject
     notification
     "share"
     path-list
     (str "unable to send share error notification for " sharee))))

(defn- send-unshare-notifications
  "Sends an unshare notification to only the current user."
  [unsharee unshares]
  (let [path-list (share-list->path-list unshares)
        share-count (count path-list)
        file-list (path-list->file-list path-list)
        subject (str share-count
                     " file(s)/folder(s) have been unshared with "
                     unsharee)
        notification (if (< share-count file-list-threshold)
                       (str " The following file(s)/folder(s) have been unshared with "
                            unsharee ": "
                            file-list)
                       subject)]
    (send-sharing-notification
     (:shortUsername current-user)
     subject
     notification
     "unshare"
     path-list
     (str "unable to send unshare notification for " unsharee))))

(defn- send-unshare-err-notification
  "Sends an unshare error notification to the current user."
  [unsharee unshares]
  (let [path-list (share-list->path-list unshares)
        share-count (count path-list)
        file-list (path-list->file-list path-list)
        subject (str share-count
                     " file(s)/folder(s) could not be unshared with "
                     unsharee)
        notification (if (< share-count file-list-threshold)
                       (str "The following file(s)/folder(s) could not be unshared with "
                            unsharee ": "
                            file-list)
                       subject)]
    (send-sharing-notification
     (:shortUsername current-user)
     subject
     notification
     "unshare"
     path-list
     (str "unable to send unshare error notification for " unsharee))))

(defn- get-user-from-subject
  [subject]
  (condp = (:source_id subject)
    "ldap"  (:id subject)
    "g:gsa" (str "@grouper-" (:id subject))
    nil))

(defn- translate-user-for-irods
  [share-unshare]
  (let [provided-user (:user share-unshare)
        provided-subject (:subject share-unshare)]
    (or provided-user (get-user-from-subject provided-subject))))

(defn- share-with-user
  "Forwards a user's share requests to data-info in a single call, sending any success notifications
   to the users involved, and any error notifications to the current user. data-info reports the
   outcome of every path, so one path that can't be shared doesn't affect the others."
  [share]
  (let [user                (translate-user-for-irods share)
        sharer              (:shortUsername current-user)
        paths               (:paths share)
        _                   (log/warn "share" (map :path paths) "with" user "by" sharer)
        user_share_results  (mapv #(dissoc % :user :reason)
                                  (data/share-paths sharer [{:user user :paths paths}]))
        successful_shares   (filter :success user_share_results)
        unsuccessful_shares (remove :success user_share_results)]
    (when (seq unsuccessful_shares)
      (log/error "data-info could not share" (map :path unsuccessful_shares) "with" user))
    (when (seq successful_shares)
      (send-share-notifications user successful_shares))
    (when (seq unsuccessful_shares)
      (send-share-err-notification user unsuccessful_shares))
    {:user user :sharing user_share_results}))

(defn- unshare-with-user
  "Forwards a user's unshare requests to data-info in a single call, sending any success
   notifications to the users involved, and any error notifications to the current user."
  [unshare]
  (let [user                  (translate-user-for-irods unshare)
        unsharer              (:shortUsername current-user)
        paths                 (:paths unshare)
        _                     (log/warn "unshare" paths "from" user "by" unsharer)
        unshare_results       (mapv #(dissoc % :user :reason)
                                    (data/unshare-paths unsharer [{:user user :paths paths}]))
        successful_unshares   (filter :success unshare_results)
        unsuccessful_unshares (remove :success unshare_results)]
    (when (seq unsuccessful_unshares)
      (log/error "data-info could not unshare" (map :path unsuccessful_unshares) "from" user))
    (when (seq successful_unshares)
      (send-unshare-notifications user successful_unshares))
    (when (seq unsuccessful_unshares)
      (send-unshare-err-notification user unsuccessful_unshares))
    {:user user :unshare unshare_results}))

(defn share
  "Parses a batch share request, forwarding each user-share request to data-info."
  [{:keys [sharing]}]
  (walk share-with-user (partial hash-map :sharing) sharing))

(defn unshare
  "Parses a batch unshare request, forwarding each user-unshare request to data-info."
  [{:keys [unshare]}]
  (walk unshare-with-user (partial hash-map :unshare) unshare))
