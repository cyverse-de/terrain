(ns terrain.routes.dashboard-aggregator
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.dashboard-aggregator]
        [terrain.services.dashboard-aggregator]
        [terrain.util])

(defn secured-dashboard-aggregator-routes
  []
  (context "/dashboard" []
    :tags ["dashboard"]
    
    (GET "/" []
      :query [params DashboardRequestParams]
      :return DashboardAggregatorResponse
      :summary "Get data for the dashboard"
      :description "Returns data for populating the dashboard view."
      (ok (dashboard-data)))))