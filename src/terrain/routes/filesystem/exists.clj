(ns terrain.routes.filesystem.exists
  (:require [common-swagger-api.schema :refer [context POST]]
            [common-swagger-api.schema.data.exists :as schema]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.clients.data-info :as data]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare paths)

(defn filesystem-existence-routes
  "The routes for path existence endpoints."
  []

  (optional-routes
    [config/filesystem-routes-enabled]

    (context "/filesystem" []
      :tags ["filesystem"]

      (POST "/exists" []
            :body [{:keys [paths]} schema/ExistenceRequest]
            :responses schema/ExistenceResponses
            :summary schema/ExistenceSummary
            :description schema/ExistenceDocs
            (ok (data/check-existence (:shortUsername current-user) paths))))))
