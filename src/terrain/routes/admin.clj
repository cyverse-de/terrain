(ns terrain.routes.admin
  (:use [compojure.api.core]
        [ring.util.http-response :only [ok]]
        [schema.core :only [Any]]
        [terrain.routes.schemas.admin]
        [terrain.util])
  (:require [terrain.util.config :as config]
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
       :return StatusResponse
       :description "Returns status information for required services."
       (ok (admin/status))))))
