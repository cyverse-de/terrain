(ns terrain.routes.alerts
  (:require [common-swagger-api.schema :refer [routes context GET POST DELETE]]
            [ring.util.http-response :refer [ok]]))

(defn alerts-routes
  []
  (routes
    (context "/alerts" []
      :tags ["notifications"]

      (GET "/all" []
        (ok))

      (GET "/active" []
        (ok))

      (POST "/" []
        (ok))

      (DELETE "/" []
        (ok)))))
