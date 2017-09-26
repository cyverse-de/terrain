(ns terrain.routes.webhooks
  (:use [compojure.core]
        [terrain.util])
  (:require [terrain.clients.metadata.raw :as metadata-client]
            [terrain.util.config :as config]
            [terrain.util.service :as service]))
(defn webhook-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (PUT "/webhooks" [:as {:keys [body]}]
      (service/success-response (metadata-client/save-webhooks body)))))