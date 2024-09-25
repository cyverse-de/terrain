(ns terrain.routes.webhooks
  (:require [common-swagger-api.schema :refer [context GET PUT]]
            [common-swagger-api.schema.webhooks :as schema]
            [ring.util.http-response :refer [ok]]
            [terrain.clients.apps.raw :as apps-client]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Dedlarations to eliminate lint warnings for path and query parameter bindings.
(declare body)

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
