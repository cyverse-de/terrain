(ns terrain.routes.vice
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.services.user-info]
        [terrain.util])
  (:require [terrain.clients.app-exposer :as vice]
            [terrain.routes.schemas.vice :as vice-schema]
            [terrain.util.config :as config]))

(defn admin-vice-routes
  []
  (optional-routes
    [config/app-routes-enabled]
    
    (context "/vice" []
      :tags ["admin-vice"]
      
      (GET "/resources" []
        :query [filter vice-schema/FilterParams]
        :return vice-schema/FullResourceListing
        :summary "List Kubernetes resources deployed in the cluster"
        :description "Lists all Kubernetes resources associated with an analysis running in the cluster."
        (ok (vice/get-resources filter))))))