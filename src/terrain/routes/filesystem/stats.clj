(ns terrain.routes.filesystem.stats
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.util :only [optional-routes]])
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
       (ok (stat/do-stat current-user body))))))
