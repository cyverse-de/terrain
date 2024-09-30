(ns terrain.routes.admin
  (:require [common-swagger-api.schema :refer [context GET]]
            [ring.util.http-response :refer [ok]]
            [schema.core :refer [Any]]
            [terrain.util.config :as config]
            [terrain.routes.schemas.admin :as schemas]
            [terrain.services.admin :as admin]
            [terrain.util :refer [optional-routes]]))

(defn secured-admin-routes
  "The routes for the admin endpoints."
  []
  (optional-routes
   [config/admin-routes-enabled]

   (context "/" []
     :tags ["admin"]

     (GET "/config" []
       :summary "Service Configuration Listing"
       :return Any
       :description "Returns service configuration information with passwords redacted."
       (ok (admin/config)))

     (GET "/status" []
       :summary "Service Status Information"
       :return schemas/StatusResponse
       :description "Returns status information for required services."
       (ok (admin/status))))))
