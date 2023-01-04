(ns terrain.routes.qms
  (:require [terrain.util.config :as config]
            [terrain.clients.qms :as qms]
            [terrain.services.qms :as handlers]
            [terrain.auth.user-attributes :refer [current-user require-authentication require-service-account]]
            [terrain.util :refer [optional-routes]]
            [common-swagger-api.schema :refer [context GET POST DELETE PUT]]
            [terrain.routes.schemas.qms :as schema]
            [ring.util.http-response :refer [ok]]))

(defn qms-api-routes
  []
  (optional-routes
   [config/qms-api-routes-enabled]

   (context "/qms" []
     :tags ["qms"]

     (context "/plans" []
       (GET "/" []
         :middleware [require-authentication]
         :summary schema/GetAllPlansSummary
         :description schema/GetAllPlansDescription
         :return schema/PlanListResponse
         (ok (qms/list-all-plans)))

       (GET "/:plan-id" []
         :middleware [require-authentication]
         :summary schema/GetPlanSummary
         :description schema/GetPlanDescription
         :path-params [plan-id :- schema/PlanID]
         :return schema/PlanResponse
         (ok (qms/single-plan plan-id))))

     ;;; It's /user and not /users since this is for the logged-in user and not
     ;;; an admin-only lookup, which would require the username to be included
     ;;; in the path or query-parameters.
     (context "/user" []
       (GET "/plan" []
         :middleware [require-authentication]
         :summary schema/GetUserPlanSummary
         :description schema/GetUserPlanDescription
         :return schema/UserPlanResponse
         (ok (qms/user-plan (:shortUsername current-user))))

       (GET "/usages" []
         :middleware [require-authentication]
         :summary schema/GetUserUsagesSummary
         :description schema/GetUserUsagesDescription
         :return schema/UsagesResponse
         (ok (qms/get-usages (:shortUsername current-user))))))))

(defn admin-qms-api-routes
  []
  (optional-routes
   [config/qms-api-routes-enabled]

   (context "/qms" []
     :tags ["admin-qms"]

     (POST "/usages" []
       :middleware [require-authentication]
       :summary schema/UpdateUsageSummary
       :description schema/UpdateUsageDescription
       :body [body schema/AddUsage]
       :return schema/SuccessResponse
       (ok (qms/add-usage body)))

     (context "/subscriptions" []
       (GET "/" []
         :middleware [require-authentication]
         :summary schema/ListSubscriptionsSummary
         :description schema/ListSubscriptionsDescription
         :query [params schema/ListSubscriptionsParams]
         :return schema/SubscriptionListingResponse
         (ok (qms/list-subscriptions params)))

       (POST "/" []
         :middleware [require-authentication]
         :summary schema/CreateSubscriptionsSummary
         :description schema/CreateSubscriptionsDescription
         :query [params schema/BulkSubscriptionParams]
         :body [body schema/SubscriptionRequests]
         :return schema/BulkSubscriptionResponse
         (ok (handlers/add-subscriptions params body))))

     (context "/users" []
       (context "/:username" []
         :path-params [username :- schema/Username]

         (GET "/usages" []
           :middleware [require-authentication]
           :summary schema/GetUserUsagesSummary
           :description schema/GetUserUsagesDescription
           :return schema/UsagesResponse
           (ok (qms/get-usages username)))

         (context "/plan" []
           (GET "/" []
             :middleware [require-authentication]
             :summary schema/GetUserPlanSummary
             :description schema/GetUserPlanDescription
             :return schema/UserPlanResponse
             (ok (qms/user-plan username)))

           (POST "/:resource-type/quota" []
             :middleware [require-authentication]
             :summary schema/UpdateUserPlanQuotaSummary
             :description schema/UpdateUserPlanQuotaDescription
             :path-params [resource-type :- schema/ResourceTypeName]
             :body [body schema/QuotaValue]
             :return schema/SubscriptionUpdateResponse
             (ok (handlers/update-user-plan-quota username resource-type body)))

           (PUT "/:plan-name" []
             :middleware [require-authentication]
             :summary schema/UpdateUserPlanSummary
             :description schema/UpdateUserPlanDescription
             :path-params [plan-name :- schema/PlanName]
             :return schema/SuccessResponse
             (ok (qms/update-user-plan username plan-name)))))))))

(defn service-account-qms-api-routes
  []
  (optional-routes
   [config/qms-api-routes-enabled]

   (context "/qms" []
     :tags ["service-account-qms"]

     (context "/users" []
       (context "/:username" []
         :path-params [username :- schema/Username]

         (PUT "/plan/:plan-name" []
           :middleware [[require-service-account ["cyverse-subscription-updater"]]]
           :summary schema/UpdateUserPlanSummary
           :description schema/UpdateUserPlanDescription
           :path-params [plan-name :- schema/PlanName]
           :return schema/SuccessResponse
           (ok (qms/update-user-plan username plan-name))))))))
