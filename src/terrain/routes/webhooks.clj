(ns terrain.routes.webhooks
  (:use [compojure.core]
        [terrain.util])
  (:require [terrain.clients.apps.raw :as apps-client]
            [terrain.util.config :as config]
            [terrain.util.service :as service]))
(defn webhook-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (PUT "/webhooks" [:as {:keys [body]}]
      (service/success-response (apps-client/save-webhooks body)))))
