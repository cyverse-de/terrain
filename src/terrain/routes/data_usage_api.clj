(ns terrain.routes.data-usage-api
  (:require [terrain.util.config :as config]
            [terrain.clients.data-usage-api :as dua]
            [terrain.auth.user-attributes :refer [current-user require-authentication]]
            [terrain.util :refer [optional-routes]]
            [common-swagger-api.schema  :refer [context GET]]
            [terrain.routes.schemas.data-usage-api :as schema]
            [ring.util.http-response :refer [ok]]))

(defn data-usage-api-routes
  []
  (optional-routes
    [config/data-usage-api-routes-enabled]

    (context "/resource-usage" []
      :tags ["resource-usage"]

      (context "/data" []
        (GET "/current" []
          :middleware [require-authentication]
          :summary schema/UserCurrentDataSummary
          :description schema/UserCurrentDataDescription
          :return schema/UserCurrentDataTotal
          (ok (dua/user-current-usage (:username current-user))))))))


