(ns terrain.routes.qms
  (:require [terrain.util.config :as config]
            [terrain.clients.qms :as qms]
            [terrain.auth.user-attributes :refer [current-user require-authentication]]
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

     (context "/users" []
       (GET "/plan" []
         :middleware [require-authentication]
         :summary schema/GetUserPlanSummary
         :description schema/GetUserPlanDescription
         :return schema/UserPlanResponse
         (ok (qms/user-plan current-user)))))))

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

     (context "/users" []
       (context "/:username" []
         :path-params [username :- schema/Username]

         (GET "/usages" []
           :middleware [require-authentication]
           :summary schema/GetUserUsagesSummary
           :description schema/GetUserUsagesDescription
           :return schema/AdminUsagesResponse
           (ok (qms/get-usages username)))

         (PUT "/plan/:plan-name" []
           :middleware [require-authentication]
           :summary schema/UpdateUserPlanSummary
           :description schema/UpdateUserPlanDescription
           :path-params [plan-name :- schema/PlanName]
           :return schema/SuccessResponse
           (ok (qms/update-user-plan username plan-name))))))))