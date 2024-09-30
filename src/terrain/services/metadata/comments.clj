(ns terrain.services.metadata.comments
  (:require [clojure-commons.error-codes :as err]
            [clojure-commons.validators :as validators]
            [slingshot.slingshot :refer [try+ throw+]]
            [terrain.auth.user-attributes :as user]
            [terrain.clients.data-info :as data]
            [terrain.clients.apps.raw :as apps]
            [terrain.clients.metadata.raw :as metadata]
            [terrain.util.config :as config]))

(defn- validate-entry-id-accessible
  [user entry-id]
  (try+
   (data/validate-uuid-accessible user entry-id)
   (catch [:error_code err/ERR_DOES_NOT_EXIST] _ (throw+ {:error_code err/ERR_NOT_FOUND}))))

(defn- validate-app-id
  [app-id]
  (apps/get-app-details config/de-system-id app-id))

(defn add-data-comment
  "Adds a comment to a filesystem entry.

   Parameters:
     entry-id - the `entry-id` from the request. This should be the UUID corresponding to the entry
                being commented on
     body - the request body. It should be a JSON document containing the comment"
  [entry-id body]
  (let [user     (:shortUsername user/current-user)
        _        (validate-entry-id-accessible user entry-id)
        tgt-type (data/resolve-data-type entry-id)]
    (metadata/add-data-comment entry-id tgt-type body)))

(defn add-app-comment
  "Adds a comment to an App.

   Parameters:
     app-id - the UUID corresponding to the App being commented on
     body - the request body. It should be a JSON document containing the comment"
  [app-id body]
  (validate-app-id app-id)
  (metadata/add-app-comment app-id body))

(defn list-data-comments
  "Returns a list of comments attached to a given filesystem entry.

   Parameters:
     entry-id - the `entry-id` from the request. This should be the UUID corresponding to the entry
                being inspected"
  [entry-id]
  (validate-entry-id-accessible (:shortUsername user/current-user) entry-id)
  (metadata/list-data-comments entry-id))

(defn list-app-comments
  "Returns a list of comments attached to a given App ID.

   Parameters:
     app-id - the `app-id` from the request. This should be the UUID corresponding to the App being
              inspected"
  [app-id]
  (validate-app-id app-id)
  (metadata/list-app-comments app-id))

(defn update-data-retract-status
  "Changes the retraction status for a given comment.

   Parameters:
     entry-id - the `entry-id` from the request. This should be the UUID corresponding to the entry
                owning the comment being modified
     comment-id - the comment-id from the request. This should be the UUID corresponding to the
                  comment being modified
     retracted - the `retracted` query parameter. This should be either `true` or `false`."
  [entry-id comment-id retracted]
  (let [user        (:shortUsername user/current-user)
        _           (validate-entry-id-accessible user entry-id)
        owns-entry? (= (keyword (:permission (data/stat-by-uuid user entry-id :filter-include "permission"))) :own)]
    (if owns-entry?
      (metadata/admin-update-data-retract-status entry-id comment-id retracted)
      (metadata/update-data-retract-status entry-id comment-id retracted))))

(defn update-app-retract-status
  "Changes the retraction status for a given comment.

   Parameters:
     app-id - the UUID corresponding to the App owning the comment being modified
     comment-id - the comment-id from the request. This should be the UUID corresponding to the
                  comment being modified
     retracted - the `retracted` query parameter. This should be either `true` or `false`."
  [app-id comment-id retracted]
  (let [app        (apps/get-app-details config/de-system-id app-id)
        owns-app?  (validators/user-owns-app? user/current-user app)]
    (if owns-app?
      (metadata/admin-update-app-retract-status app-id comment-id retracted)
      (metadata/update-app-retract-status app-id comment-id retracted))))

(defn delete-data-comment
  [entry-id comment-id]
  (metadata/delete-data-comment entry-id comment-id))

(defn delete-app-comment
  [app-id comment-id]
  (metadata/delete-app-comment app-id comment-id))

(defn list-comments-by-user
  "Lists all of the comments that were entered by the given user.

   Parameters:
     commenter-id: the ID of the user who entered the comments."
  [commenter-id]
  (metadata/list-comments-by-user commenter-id))

(defn delete-comments-by-user
  "Deletes all of the comments that were entered by the given user.

   Parameters:
     commenter-id: the ID of the user who entered the comments."
  [commenter-id]
  (metadata/delete-comments-by-user commenter-id))
