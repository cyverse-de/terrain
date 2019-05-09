(ns terrain.routes.webhooks
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.util])
  (:require [common-swagger-api.schema.webhooks :as schema]
            [terrain.clients.apps.raw :as apps-client]
            [terrain.util.config :as config]))
(defn webhook-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/webhooks" []
       :tags ["webhooks"]

       (GET "/" []
            :return schema/WebhookList
            :summary schema/GetWebhooksSummary
            :description schema/GetWebhooksDesc
            (ok (apps-client/get-webhooks)))

       (PUT "/" []
            :body [body schema/WebhookList]
            :return schema/WebhookList
            :summary schema/PutWebhooksSummary
            :description schema/PutWebhooksDesc
            (ok (apps-client/save-webhooks body)))

       (GET "/topics" []
            :return schema/TopicList
            :summary schema/GetWebhooksTopicSummary
            :description schema/GetWebhooksTopicDesc
            (ok (apps-client/get-webhook-topics)))

       (GET "/types" []
            :return schema/WebhookTypeList
            :summary schema/GetWebhookTypesSummary
            :description schema/GetWebhookTypesDesc
            (ok (apps-client/get-webhook-types))))))
