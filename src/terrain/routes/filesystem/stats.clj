(ns terrain.routes.filesystem.stats
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.util :only [optional-routes]]
        [terrain.util.transformers :only [add-current-user-to-map]])
  (:require [common-swagger-api.schema.data :as data-schema]
            [common-swagger-api.schema.stats :as schema]
            [terrain.services.filesystem.stat :as stat]
            [terrain.util.config :as config]))

(defn filesystem-stat-routes
  "The routes for filesystem stat endpoints."
  []

  (optional-routes
   [config/filesystem-routes-enabled]

   (context "/filesystem" []
     :tags ["filesystem"]

     (POST "/stat" []
       :body [body data-schema/OptionalPathsOrDataIds]
       :responses schema/StatResponses
       :summary schema/StatSummary
       :description schema/StatDocs
       (ok (stat/do-stat (add-current-user-to-map {}) body))))))
