(ns terrain.routes.bootstrap
  (:require [common-swagger-api.schema :refer [context GET]]
            [common-swagger-api.schema.sessions :as sessions-schema]
            [ring.util.http-response :refer [ok]]
            [terrain.clients.apps.raw :as apps-client]
            [terrain.routes.schemas.bootstrap :as schema]
            [terrain.services.bootstrap :refer [bootstrap]]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

(defn secured-bootstrap-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/bootstrap" []
      :tags ["bootstrap"]

      (GET "/" []
           :return schema/TerrainBootstrapResponse
           :summary "Bootstrap Service"
           :description "This service obtains information about and initializes the workspace for the authenticated user.
           It also records the fact that the user logged in."
           (ok (bootstrap))))))
