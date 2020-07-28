(ns terrain.routes.filesystem.navigation
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [require-authentication]]
        [terrain.util :only [optional-routes]]
        [terrain.util.transformers :only [add-current-user-to-map]])
  (:require [common-swagger-api.schema.data.navigation :as schema]
            [terrain.routes.schemas.filesystem.navigation :as terrain-nav-schema]
            [terrain.services.filesystem.directory :as dir]
            [terrain.services.filesystem.root :as root]
            [terrain.util.config :as config]))

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
           :middleware [require-authentication]
           :query [params terrain-nav-schema/DirectoryQueryParams]
           :responses terrain-nav-schema/DirectoryResponses
           :summary schema/NavigationSummary
           :description schema/NavigationDocs
           (ok (dir/do-directory (add-current-user-to-map params)))))))
