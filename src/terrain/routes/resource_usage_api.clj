(ns terrain.routes.resource-usage-api
  (:require [terrain.util.config :as config]
            [terrain.clients.resource-usage-api :as rua]
            [terrain.auth.user-attributes :refer [current-user require-authentication]]
            [terrain.util :refer [optional-routes]]
            [common-swagger-api.schema :refer [context GET POST DELETE]]
            [terrain.routes.schemas.resource-usage-api :as schema]
            [ring.util.http-response :refer [ok]]))

(defn resource-usage-api-routes
  []
  (optional-routes
   [config/resource-usage-api-routes-enabled]

   (context "/resource-usage" []
     :tags ["resource-usage"]

     (context "/summary" []
       (GET "/" []
         :middleware [require-authentication]
         :summary schema/ResourceSummarySummary
         :description schema/ResourceSummaryDescription
         :return schema/ResourceSummary
         (ok (rua/resource-summary (:username current-user))))))))