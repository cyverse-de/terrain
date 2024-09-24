(ns terrain.routes.filesystem.stats
  (:require [common-swagger-api.schema :refer [context POST]]
            [common-swagger-api.schema.data :as data-schema]
            [common-swagger-api.schema.stats :as schema]
            [ring.util.http-response :refer [ok]]
            [terrain.services.filesystem.stat :as stat]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]
            [terrain.util.transformers :refer [add-current-user-to-map]]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare body)

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
       (ok (stat/do-stat (add-current-user-to-map {} nil) body))))))
