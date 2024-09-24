(ns terrain.routes.groups
  (:require [common-swagger-api.schema :refer [routes DELETE GET PUT]]
            [terrain.clients.apps.raw :as apps]
            [terrain.clients.iplant-groups :as ipg]
            [terrain.util.service :as service]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare subject-id body)

(defn admin-groups-routes
  []
  (routes
   (DELETE "/groups/de-users/members/:subject-id" [subject-id]
     (service/success-response (ipg/remove-de-user subject-id)))

   (GET "/groups/workshop" []
     (service/success-response (apps/get-workshop-group)))

   (GET "/groups/workshop/members" []
     (service/success-response (apps/get-workshop-group-members)))

   (PUT "/groups/workshop/members" [:as {:keys [body]}]
     (service/success-response (apps/update-workshop-group-members body)))))
