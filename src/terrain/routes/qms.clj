(ns terrain.routes.qms
  (:require [terrain.util.config :as config]
            [terrain.clients.qms :as qms]
            [terrain.clients.qms-nats :as n]
            [terrain.services.qms :as handlers]
            [terrain.auth.user-attributes :refer [current-user require-authentication require-service-account]]
            [terrain.util :refer [optional-routes]]
            [common-swagger-api.schema :refer [context GET POST DELETE PUT]]
            [terrain.routes.schemas.qms :as schema]
            [ring.util.http-response :refer [ok]]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare plan-id body params subscription-uuid uuid username resource-type plan-name)

(defn qms-api-routes
  []
  (optional-routes
   [config/qms-api-routes-enabled]

   (context "/qms" []
     :tags ["qms"]

     (context "/plans" []
       (GET "/" []
         :summary schema/GetAllPlansSummary
         :description schema/GetAllPlansDescription
         :return schema/PlanListResponse
         (ok (qms/list-all-plans)))

       (GET "/:plan-id" []
         :summary schema/GetPlanSummary
         :description schema/GetPlanDescription
         :path-params [plan-id :- schema/PlanID]
         :return schema/PlanResponse
         (ok (qms/single-plan plan-id))))

     (context "/resource-types" []
       (GET "/" []
         :summary schema/GetResourceTypesSummary
         :description schema/GetResourceTypesDescription
         :return schema/ResourceTypesResponse
         (ok (qms/list-resource-types))))

     ;;; It's /user and not /users since this is for the logged-in user and not
     ;;; an admin-only lookup, which would require the username to be included
     ;;; in the path or query-parameters.
     (context "/user" []
       (GET "/plan" []
         :middleware [require-authentication]
         :summary schema/GetSubscriptionSummary
         :description schema/GetSubscriptionDescription
         :return schema/SubscriptionPlanResponse
         (ok (qms/subscription (:shortUsername current-user))))

       (GET "/subscriptions" []
         :middleware [require-authentication]
         :summary schema/ListUserSubscriptionsSummary
         :description schema/ListUserSubscriptionsDescription
         :query [params schema/ListUserSubscriptionsParams]
         :return schema/SubscriptionListingResponse
         (ok (qms/list-user-subscriptions (:shortUsername current-user) params)))

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
         (ok (handlers/add-subscriptions params body)))

       (context "/:subscription-uuid" []
         :path-params [subscription-uuid :- schema/SubscriptionID]

         (context "/addons" []
           (GET "/" []
             :middleware [require-authentication]
             :summary schema/ListSubscriptionAddonsSummary
             :description schema/ListSubscriptionAddonsDescription
             :return schema/SubscriptionAddonListResponse
             (ok (n/list-subscription-addons subscription-uuid)))

           (POST "/" []
             :middleware [require-authentication]
             :summary schema/AddSubscriptionAddonSummary
             :description schema/AddSubscriptionAddonDescription
             :body [body schema/AddonIDBody]
             :return schema/SubscriptionAddonResponse
             (ok (n/add-subscription-addon subscription-uuid (:uuid body))))

           (context "/:uuid" []
             :path-params [uuid :- schema/SubscriptionAddonID]

             (GET "/" []
               :middleware [require-authentication]
               :summary schema/GetSubscriptionAddonSummary
               :description schema/GetSubscriptionAddonDescription
               :return schema/SubscriptionAddonResponse
               (ok (n/get-subscription-addon uuid)))

             (PUT "/" []
               :middleware [require-authentication]
               :summary schema/UpdateSubscriptionAddonSummary
               :description schema/UpdateSubscriptionAddonDescription
               :body [body schema/UpdateSubscriptionAddon]
               (ok (n/update-subscription-addon (assoc body :uuid uuid))))

             (DELETE "/" []
               :middleware [require-authentication]
               :summary schema/DeleteSubscriptionAddonSummary
               :description schema/DeleteSubscriptionAddonDescription
               :return schema/SubscriptionAddonResponse
               (ok (n/delete-subscription-addon uuid)))))))

     (context "/addons" []
       (POST "/" []
         :middleware [require-authentication]
         :summary schema/AddAddonSummary
         :description schema/AddAddonDescription
         :body [body schema/AddOn]
         :return schema/AddonResponse
         (ok (n/add-addon body)))

       (GET "/" []
         :middleware [require-authentication]
         :summary schema/ListAddonsSummary
         :description schema/ListAddonsDescription
         :return schema/AddonListResponse
         (ok (n/list-addons)))

       (PUT "/" []
         :middleware [require-authentication]
         :summary schema/UpdateAddonSummary
         :description schema/UpdateAddonDescription
         :body [body schema/UpdateAddon]
         :return schema/AddonResponse
         (ok (n/update-addon body)))

       (DELETE "/:uuid" []
         :middleware [require-authentication]
         :summary schema/DeleteAddonSummary
         :description schema/DeleteAddonDescription
         :path-params [uuid :- schema/AddonID]
         :return schema/DeletedAddonResponse
         (ok (n/delete-addon uuid))))

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
             :summary schema/GetSubscriptionSummary
             :description schema/GetSubscriptionDescription
             :return schema/SubscriptionPlanResponse
             (ok (qms/subscription username)))

           (POST "/:resource-type/quota" []
             :middleware [require-authentication]
             :summary schema/UpdateSubscriptionQuotaSummary
             :description schema/UpdateSubscriptionQuotaDescription
             :path-params [resource-type :- schema/ResourceTypeName]
             :body [body schema/QuotaValue]
             :return schema/SubscriptionUpdateResponse
             (ok (handlers/update-subscription-quota username resource-type body)))

           (PUT "/:plan-name" []
             :middleware [require-authentication]
             :summary schema/UpdateSubscriptionSummary
             :description schema/UpdateSubscriptionDescription
             :query [params schema/AddSubscriptionParams]
             :path-params [plan-name :- schema/PlanName]
             :return schema/SuccessResponse
             (ok (qms/update-subscription username plan-name params))))

         (GET "/subscriptions" []
           :middleware [require-authentication]
           :summary schema/ListUserSubscriptionsSummary
           :description schema/ListUserSubscriptionsDescription
           :query [params schema/ListUserSubscriptionsParams]
           :return schema/SubscriptionListingResponse
           (ok (qms/list-user-subscriptions username params))))))))

(defn service-account-qms-api-routes
  []
  (optional-routes
   [config/qms-api-routes-enabled]

   (context "/qms" []
     :tags ["service-account-qms"]

     (context "/users" []
       (context "/:username" []
         :path-params [username :- schema/Username]

         (context "/plan" []
           (GET "/" []
             :middleware [[require-service-account ["cyverse-subscription-updater"]]]
             :summary schema/GetSubscriptionSummary
             :description schema/GetSubscriptionDescription
             :return schema/SubscriptionPlanResponse
             (ok (qms/subscription username)))

           (PUT "/:plan-name" []
             :middleware [[require-service-account ["cyverse-subscription-updater"]]]
             :summary schema/UpdateSubscriptionSummary
             :description schema/UpdateSubscriptionDescription
             :query [params schema/ServiceAccountAddSubscriptionParams]
             :path-params [plan-name :- schema/PlanName]
             :return schema/SuccessResponse
             (ok (qms/update-subscription username plan-name (merge {:paid true} params)))))

         (GET "/subscriptions" []
           :middleware [[require-service-account ["cyverse-subscription-updater"]]]
           :summary schema/ListUserSubscriptionsSummary
           :description schema/ListUserSubscriptionsDescription
           :query [params schema/ListUserSubscriptionsParams]
           :return schema/SubscriptionListingResponse
           (ok (qms/list-user-subscriptions username params)))))

     (context "/addons" []
       (GET "/" []
         :middleware [[require-service-account ["cyverse-subscription-updater"]]]
         :summary schema/ListAddonsSummary
         :description schema/ListAddonsDescription
         :return schema/AddonListResponse
         (ok (n/list-addons))))

     (context "/subscriptions" []
       (context "/:subscription-uuid" []
         :path-params [subscription-uuid :- schema/SubscriptionID]

         (context "/addons" []
           (GET "/" []
             :middleware [[require-service-account ["cyverse-subscription-updater"]]]
             :summary schema/ListSubscriptionAddonsSummary
             :description schema/ListSubscriptionAddonsDescription
             :return schema/SubscriptionAddonListResponse
             (ok (n/list-subscription-addons subscription-uuid)))

           (POST "/" []
             :middleware [[require-service-account ["cyverse-subscription-updater"]]]
             :summary schema/AddSubscriptionAddonSummary
             :description schema/AddSubscriptionAddonDescription
             :body [body schema/AddonIDBody]
             :return schema/SubscriptionAddonResponse
             (ok (n/add-subscription-addon subscription-uuid (:uuid body))))

           (context "/:uuid" []
             :path-params [uuid :- schema/SubscriptionAddonID]

             (GET "/" []
               :middleware [[require-service-account ["cyverse-subscription-updater"]]]
               :summary schema/GetSubscriptionAddonSummary
               :description schema/GetSubscriptionAddonDescription
               :return schema/SubscriptionAddonResponse
               (ok (n/get-subscription-addon uuid)))

             (PUT "/" []
               :middleware [[require-service-account ["cyverse-subscription-updater"]]]
               :summary schema/UpdateSubscriptionAddonSummary
               :description schema/UpdateSubscriptionAddonDescription
               :body [body schema/UpdateSubscriptionAddon]
               (ok (n/update-subscription-addon (assoc body :uuid uuid))))

             (DELETE "/" []
               :middleware [[require-service-account ["cyverse-subscription-updater"]]]
               :summary schema/DeleteSubscriptionAddonSummary
               :description schema/DeleteSubscriptionAddonDescription
               :return schema/SubscriptionAddonResponse
               (ok (n/delete-subscription-addon uuid))))))))))
