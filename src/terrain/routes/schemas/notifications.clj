(ns terrain.routes.schemas.notifications
  (:require
   [common-swagger-api.schema :refer [describe PagingParams
                                      SortFieldOptionalKey]]
   [schema.core :as s])
  (:import
   [java.util UUID]))

;; Common schemas
(def NotificationUUID (describe UUID "The notification UUID"))

;; Query parameter schemas
(s/defschema NotificationPagingParams
  (merge PagingParams
         {SortFieldOptionalKey
          (describe String "The field to sort results by. Defaults to timestamp.")

          (s/optional-key :sortdir)
          (describe (s/enum "asc" "desc") "The sort direction. Defaults to desc.")

          (s/optional-key :filter)
          (describe String "Filter notifications by type or message content")

          (s/optional-key :seen)
          (describe Boolean "Filter by seen status")}))

(s/defschema DeleteMatchingMessagesParams
  {(s/optional-key :filter)
   (describe String "The type of message to delete or \"new\" to mark all new messages as deleted")})

(s/defschema MessageCountParams
  {(s/optional-key :filter)
   (describe String "The type of message to display or \"new\" to display only messages that haven't been seen")

   (s/optional-key :seen)
   (describe Boolean "Filter by seen status")})

;; Request body schemas
(s/defschema NotificationUUIDList
  {:uuids (describe [NotificationUUID] "A list of notification UUIDs")})

;; Response schemas
(s/defschema NotificationMessage
  {(s/optional-key :deleted)
   (describe Boolean "Whether the notification has been deleted")

   (s/optional-key :message)
   (describe {s/Any s/Any} "The notification message details")

   (s/optional-key :seen)
   (describe Boolean "Whether the notification has been seen")

   (s/optional-key :type)
   (describe String "The notification type")

   (s/optional-key :user)
   (describe String "The user who received the notification")

   s/Any s/Any})

(s/defschema NotificationListing
  {:total
   (describe String "The total number of notifications matching the query")

   :messages
   (describe [NotificationMessage] "The list of notifications")

   s/Any s/Any})

(s/defschema MessageCountResponse
  {:user-total
   (describe Long "The total number of messages for the user")

   s/Any s/Any})

;; Summary and description strings
(def GetMessagesSummary "List notifications")
(def GetMessagesDescription
  "Lists notifications for the authenticated user. Results can be filtered and paginated.")

(def GetUnseenMessagesSummary "List unseen notifications")
(def GetUnseenMessagesDescription
  "Lists notifications that have not been marked as seen for the authenticated user.")

(def GetLastTenMessagesSummary "Get last ten notifications")
(def GetLastTenMessagesDescription
  "Returns the ten most recent notifications for the authenticated user, sorted by timestamp.")

(def CountMessagesSummary "Count notifications")
(def CountMessagesDescription
  "Returns the count of notifications for the authenticated user, optionally filtered.")

(def DeleteNotificationsSummary "Delete notifications")
(def DeleteNotificationsDescription
  "Deletes the specified notifications for the authenticated user.")

(def DeleteAllNotificationsSummary "Delete all notifications")
(def DeleteAllNotificationsDescription
  "Deletes all notifications matching the filter criteria for the authenticated user.")

(def MarkSeenSummary "Mark notifications as seen")
(def MarkSeenDescription
  "Marks the specified notifications as seen for the authenticated user.")

(def MarkAllSeenSummary "Mark all notifications as seen")
(def MarkAllSeenDescription
  "Marks all notifications as seen for the authenticated user.")
