(ns terrain.routes.filesystem.navigation
  (:require [common-swagger-api.schema :refer [context GET]]
            [common-swagger-api.schema.data.navigation :as schema]
            [ring.util.http-response :refer [ok]]
            [terrain.routes.schemas.filesystem.navigation :as terrain-nav-schema]
            [terrain.services.filesystem.directory :as dir]
            [terrain.services.filesystem.root :as root]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]
            [terrain.util.transformers :refer [add-current-user-to-map]]))

;; Declarations to eliminate warnings for path and query parameter bindings.
(declare params)

(defn filesystem-navigation-routes
  "The routes for filesystem navigation endpoints."
  []

  (optional-routes
    [config/filesystem-routes-enabled]

    (context "/filesystem" []
      :tags ["filesystem"]

      (GET "/root" []
           :responses terrain-nav-schema/RootResponses
           :summary schema/NavigationRootSummary
           :description schema/NavigationRootDocs
           (ok (root/do-root-listing (add-current-user-to-map {}))))

      (GET "/directory" [:as {:keys [params]}]
           :query [params terrain-nav-schema/DirectoryQueryParams]
           :responses terrain-nav-schema/DirectoryResponses
           :summary schema/NavigationSummary
           :description schema/NavigationDocs
           (ok (dir/do-directory (add-current-user-to-map params)))))))
