(ns terrain.routes.admin
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [schema.core :only [Any]]
        [terrain.util])
  (:require [terrain.util.config :as config]
            [terrain.routes.schemas.admin :as schemas]
            [terrain.services.admin :as admin]
            [clojure.tools.logging :as log]))

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
