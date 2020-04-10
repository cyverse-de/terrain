(ns terrain.routes.settings
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.util :only [optional-routes]])
  (:require [common-swagger-api.schema.analyses :as analyses-schema]
            [terrain.clients.analyses :as ac]
            [terrain.util.config :as config]))

(defn admin-setting-routes
  "Routes for administering settings."
  []
  (optional-routes
   [#(and (config/admin-routes-enabled) (config/setting-routes-enabled))]

   (context "/settings" []
     :tags ["admin-settings"]

     (context "/concurrent-job-limits" []

       (GET "/" []
         :summary analyses-schema/ConcurrentJobLimitListingSummary
         :return analyses-schema/ConcurrentJobLimits
         :description analyses-schema/ConcurrentJobLimitListingDescription
         (ok (ac/list-concurrent-job-limits)))

       (context "/:username" []
         :path-params [username :- analyses-schema/ConcurrentJobLimitUsername]

         (GET "/" []
           :summary analyses-schema/ConcurrentJobLimitRetrievalSummary
           :return analyses-schema/ConcurrentJobLimit
           :description analyses-schema/ConcurrentJobLimitRetrievalDescription
           (ok (ac/get-concurrent-job-limit username)))

         (PUT "/" []
           :summary analyses-schema/ConcurrentJobLimitUpdateSummary
           :body [body analyses-schema/ConcurrentJobLimitUpdate]
           :return analyses-schema/ConcurrentJobLimit
           :description analyses-schema/ConcurrentJobLimitUpdateDescription
           (ok (ac/set-concurrent-job-limit username (:concurrent_jobs body))))

         (DELETE "/" []
           :summary analyses-schema/ConcurrentJobLimitRemovalSummary
           :return analyses-schema/ConcurrentJobLimit
           :description analyses-schema/ConcurrentJobLimitRemovalDescription
           (ok (ac/remove-concurrent-job-limit username))))))))
