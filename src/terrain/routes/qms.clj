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

     (context "/resource-types" []
       (GET "/" []
         :middleware [require-authentication]
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
             (ok (handlers/list-subscription-addons subscription-uuid)))
           
           (POST "/" []
             :middleware [require-authentication]
             :summary schema/AddSubscriptionAddonSummary
             :description schema/AddSubscriptionAddonDescription
             :body [body schema/AddonIDBody]
             :return schema/SubscriptionAddonResponse
             (ok (handlers/add-subscription-addon subscription-uuid (:uuid body))))
           
           (context "/:uuid" []
             :path-params [uuid :- schema/SubscriptionAddonID]
             
             (GET "/" []
               :middleware [require-authentication]
               :summary schema/GetSubscriptionAddonSummary
               :description schema/GetSubscriptionAddonDescription
               :return schema/SubscriptionAddon
               (ok (handlers/get-subscription-addon uuid)))
           
             (PUT "/" []
               :middleware [require-authentication]
               :summary schema/UpdateSubscriptionAddonSummary
               :description schema/UpdateSubscriptionAddonDescription
               :body [body schema/UpdateSubscriptionAddon]
               (ok (handlers/update-subscription-addon (assoc body :uuid uuid))))
             
             (DELETE "/" []
               :middleware [require-authentication]
               :summary schema/DeleteSubscriptionAddonSummary
               :description schema/DeleteSubscriptionAddonDescription
               :return schema/SubscriptionAddon
               (ok (handlers/delete-subscription-addon uuid)))))))

     (context "/addons" []
       (POST "/" []
         :middleware [require-authentication]
         :summary schema/AddAddonSummary
         :description schema/AddAddonDescription
         :body [body schema/AddOn]
         :return schema/AddonResponse
         (ok (handlers/add-addon body)))

       (GET "/" []
         :middleware [require-authentication]
         :summary schema/ListAddonsSummary
         :description schema/ListAddonsDescription
         :return schema/AddonListResponse
         (ok (handlers/list-addons)))
       
       (PUT "/" []
         :middleware [require-authentication]
         :summary schema/UpdateAddonSummary
         :description schema/UpdateAddonDescription
         :body [body schema/UpdateAddon]
         :return schema/AddonResponse
         (ok (handlers/update-addon body)))
       
       (DELETE "/:uuid" []
         :middleware [require-authentication]
         :summary schema/DeleteAddonSummary
         :description schema/DeleteAddonDescription
         :path-params [uuid :- schema/AddonID]
         :return schema/DeletedAddonResponse
         (ok (handlers/delete-addon uuid))))

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
             (ok (qms/update-subscription username plan-name params)))))))))

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
           :summary schema/UpdateSubscriptionSummary
           :description schema/UpdateSubscriptionDescription
           :path-params [plan-name :- schema/PlanName]
           :return schema/SuccessResponse
           (ok (qms/update-subscription username plan-name {:paid true}))))))))
