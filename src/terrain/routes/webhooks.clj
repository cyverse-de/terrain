(ns terrain.routes.webhooks
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.util])
  (:require [common-swagger-api.schema.webhooks :as schema]
            [terrain.clients.apps.raw :as apps-client]
            [terrain.util.config :as config]
            [terrain.util.service :as service]))
(defn webhook-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/webhooks" []
       :tags ["webhooks"]

       (PUT "/" []
            :body [body schema/WebhookList]
            :return schema/WebhookList
            :summary schema/PutWebhooksSummary
            :description schema/PutWebhooksDesc
            (ok (apps-client/save-webhooks body)))

       (GET "/types" [:as {:keys []}]
            (service/success-response (apps-client/get-webhook-types))))))
