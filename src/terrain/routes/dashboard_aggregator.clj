(ns terrain.routes.dashboard-aggregator
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.routes.schemas.dashboard-aggregator]
        [terrain.util])
  (:require [clojure.tools.logging :as log]
            [terrain.clients.dashboard-aggregator :as dcl]))

(defn dashboard-aggregator-routes
  []
  (context "/dashboard" []
    :tags ["dashboard"]

    (GET "/" []
      :query [params DashboardRequestParams]
      :return DashboardAggregatorResponse
      :summary "Get data for the dashboard"
      :description "Returns data for populating the dashboard view."
      (ok (if current-user
        (dcl/get-dashboard-data (:username current-user) params)
        (dcl/get-dashboard-data params))))))
