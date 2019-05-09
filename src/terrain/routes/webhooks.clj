(ns terrain.routes.webhooks
  (:use [common-swagger-api.schema]
        [terrain.util])
  (:require [terrain.clients.apps.raw :as apps-client]
            [terrain.util.config :as config]
            [terrain.util.service :as service]))
(defn webhook-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/webhooks" []
       :tags ["webhooks"]

       (PUT "/" [:as {:keys [body]}]
            (service/success-response (apps-client/save-webhooks body)))

       (GET "/types" [:as {:keys []}]
            (service/success-response (apps-client/get-webhook-types))))))
