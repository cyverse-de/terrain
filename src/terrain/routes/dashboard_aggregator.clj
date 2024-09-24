(ns terrain.routes.dashboard-aggregator
  (:require [common-swagger-api.schema :refer [context GET]]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.clients.dashboard-aggregator :as dcl]
            [terrain.routes.schemas.dashboard-aggregator :refer [DashboardRequestParams DashboardAggregatorResponse]]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare params)

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
