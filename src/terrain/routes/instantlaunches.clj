(ns terrain.routes.instantlaunches
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.instantlaunches]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.clients.app-exposer :only [latest-instant-launch-mappings-defaults]]
        [terrain.util :only [optional-routes]])
  (:require [terrain.util.config :as config]
            [clojure.tools.logging :as log]))

(defn instant-launch-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (context "/instantlaunches" []
     :tags ["instantlaunches"]

     (context "/mappings" []
       (context "/defaults" []

         (GET "/latest" []
           :summary LatestILMappingsDefaultsSummary
           :description LatestILMappingsDefaultsDescription
           :return DefaultInstantLaunchMapping
           (ok (latest-instant-launch-mappings-defaults))))))))