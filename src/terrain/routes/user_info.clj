(ns terrain.routes.user-info
  (:use [common-swagger-api.schema]
        [terrain.services.user-info]
        [terrain.util]
        [terrain.util.service :only [success-response]])
  (:require [terrain.clients.iplant-groups :as ipg]
            [terrain.util.config :as config]))

(defn secured-user-info-routes
  []
  (optional-routes
   [config/user-info-routes-enabled]

   (context "/user-info" []
     :tags ["user-info"]

     (GET "/" [:as {:keys [params]}]
          (user-info (as-vector (:username params)))))))

(defn admin-user-info-routes
  []
  (optional-routes
   [config/user-info-routes-enabled]

   (GET "/users/:username/groups" [username]
     (success-response (ipg/list-groups-for-user username)))))
