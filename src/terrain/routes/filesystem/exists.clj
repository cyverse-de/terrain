(ns terrain.routes.filesystem.exists
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.util :only [optional-routes]])
  (:require [common-swagger-api.schema.data.exists :as schema]
            [terrain.clients.data-info :as data]
            [terrain.util.config :as config]))

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
