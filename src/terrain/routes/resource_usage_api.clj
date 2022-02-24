(ns terrain.routes.resource-usage-api
  (:require [terrain.util.config :as config]
            [terrain.clients.resource-usage-api :as rua]
            [terrain.auth.user-attributes :refer [current-user require-authentication]]
            [terrain.util :refer [optional-routes]]
            [common-swagger-api.schema :refer [context GET POST DELETE]]
            [terrain.routes.schemas.resource-usage-api :as schema]
            [ring.util.http-response :refer [ok]]))

(defn resource-usage-api-routes
  []
  (optional-routes
   [config/resource-usage-api-routes-enabled]

   (context "/resource-usage" []
     :tags ["resource-usage"]

     (context "/summary" []
       (GET "/" []
         :middleware [require-authentication]
         :summary schema/ResourceSummarySummary
         :description schema/ResourceSummaryDescription
         :return schema/ResourceSummary
         (ok (rua/resource-summary (:username current-user)))))

     (context "/cpu" []
       (GET "/total" []
         :middleware [require-authentication]
         :summary schema/CurrentTotalSummary
         :description schema/CurrentTotalDescription
         :return schema/CPUHoursTotal
         (ok (rua/current-cpu-hours-total (:username current-user))))

       (GET "/total/all" []
         :middleware [require-authentication]
         :summary schema/AllTotalsSummary
         :description schema/AllTotalsDescription
         :return [schema/CPUHoursTotal]
         (ok (rua/all-cpu-hours-totals (:username current-user))))))))

(defn admin-resource-usage-api-routes
  []
  (optional-routes
   [config/resource-usage-api-routes-enabled]

   (context "/resource-usage" []
     :tags ["admin-resource-usage"]

     (context "/workers" []
       (GET "/" []
         :middleware [require-authentication]
         :summary schema/ListWorkersSummary
         :description schema/ListWorkersDescription
         :return [schema/Worker]
         (ok (rua/list-workers)))

       (GET "/:worker-id" []
         :middleware [require-authentication]
         :path-params [worker-id :- schema/WorkerID]
         :summary schema/GetWorkerSummary
         :description schema/GetWorkerDescription
         :return schema/Worker
         (ok (rua/worker worker-id))))

     (context "/cpu" []
       (GET "/totals" []
         :middleware [require-authentication]
         :summary schema/AllUsersCurrentCPUTotalSummary
         :description schema/AllUsersCurrentCPUTotalDescription
         :return [schema/CPUHoursTotal]
         (ok (rua/current-cpu-hours-total)))

       (GET "/totals/all" []
         :middleware [require-authentication]
         :summary schema/AllUsersAllCPUTotalsSummary
         :description schema/AllUsersAllCPUTotalsDescription
         :return [schema/CPUHoursTotal]
         (ok (rua/all-cpu-hours-totals)))

       (context "/events" []
         (GET "/" []
           :middleware [require-authentication]
           :summary schema/ListEventsSummary
           :description schema/ListEventsDescription
           :return [schema/Event]
           (ok (rua/list-events)))

         (context "/user/:username" []
           :path-params [username :- schema/Username]

           (GET "/" []
             :middleware [require-authentication]
             :path-params [username :- schema/Username]
             :summary schema/ListUserEventsSummary
             :description schema/ListUserEventsDescription
             :return [schema/Event]
             (ok (rua/list-events username)))

           (POST "/add/:hours" []
             :middleware [require-authentication]
             :path-params [hours :- schema/HoursNumber]
             :summary schema/AddHoursSummary
             :description schema/AddHoursDescription
             (ok (rua/add-cpu-hours username hours)))

           (POST "/subtract/:hours" []
             :middleware [require-authentication]
             :path-params [hours :- schema/HoursNumber]
             :summary schema/SubtractHoursSummary
             :description schema/SubtractHoursDescription
             (ok (rua/subtract-cpu-hours username hours)))

           (POST "/reset/:hours" []
             :middleware [require-authentication]
             :path-params [hours :- schema/HoursNumber]
             :summary schema/ResetHoursSummary
             :description schema/ResetHoursDescription
             (ok (rua/reset-cpu-hours username hours))))


         (GET "/:event-id" []
           :middleware [require-authentication]
           :path-params [event-id :- schema/EventID]
           :summary schema/GetEventSummary
           :description schema/GetEventDescription
           :return schema/Event
           (ok (rua/event event-id))))))))