(ns terrain.routes.notification
  (:require
   [common-swagger-api.schema :refer [context DELETE GET POST]]
   [ring.util.http-response :refer [ok]]
   [terrain.routes.schemas.notifications :as schema]
   [terrain.services.notifications :as notifications]
   [terrain.util :refer [optional-routes]]
   [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare params body uuid)

(defn secured-notification-routes
  []
  (optional-routes
   [config/notification-routes-enabled]

   (context "/notifications" []
     :tags ["notifications"]

     (GET "/messages" []
       :query [params schema/NotificationPagingParams]
       :summary schema/GetMessagesSummary
       :description schema/GetMessagesDescription
       :return schema/NotificationListing
       (ok (notifications/get-messages params)))

     (GET "/unseen-messages" []
       :query [params schema/NotificationPagingParams]
       :summary schema/GetUnseenMessagesSummary
       :description schema/GetUnseenMessagesDescription
       :return schema/NotificationListing
       (ok (notifications/get-unseen-messages params)))

     (GET "/last-ten-messages" []
       :summary schema/GetLastTenMessagesSummary
       :description schema/GetLastTenMessagesDescription
       :return schema/NotificationListing
       (ok (notifications/last-ten-messages)))

     (GET "/count-messages" []
       :query [params schema/MessageCountParams]
       :summary schema/CountMessagesSummary
       :description schema/CountMessagesDescription
       :return schema/MessageCountResponse
       (ok (notifications/count-messages params)))

     (POST "/delete" []
       :body [body schema/NotificationUUIDList]
       :summary schema/DeleteNotificationsSummary
       :description schema/DeleteNotificationsDescription
       (ok (notifications/delete-notifications body)))

     (DELETE "/delete-all" []
       :query [params schema/DeleteMatchingMessagesParams]
       :summary schema/DeleteAllNotificationsSummary
       :description schema/DeleteAllNotificationsDescription
       (ok (notifications/delete-all-notifications params)))

     (POST "/seen" []
       :body [body schema/NotificationUUIDList]
       :summary schema/MarkSeenSummary
       :description schema/MarkSeenDescription
       (ok (notifications/mark-notifications-seen body)))

     (POST "/mark-all-seen" []
       :summary schema/MarkAllSeenSummary
       :description schema/MarkAllSeenDescription
       (ok (notifications/mark-all-notifications-seen))))))
