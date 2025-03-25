(ns terrain.routes.alerts
  (:require [terrain.clients.user-info :as user-info]
            [terrain.routes.schemas.alerts :as alerts-schema]
            [common-swagger-api.schema :refer [routes context GET POST DELETE]]
            [ring.util.http-response :refer [ok]]))

(defn alerts-routes
  []
  (routes
    (context "/alerts" []
      :tags ["notifications"]

      (GET "/all" []
        (ok (user-info/list-all-alerts)))

      (GET "/active" []
        (ok (user-info/list-active-alerts))))))

(defn admin-alerts-routes
  []
  (routes
    (context "/alerts" []
      :tags ["notifications"]

      (POST "/" []
        :body [body alerts-schema/Alert]
        (ok (user-info/add-alert (:start-date body) (:end-date body) (:alert-text body))))

      (DELETE "/" []
        :body [body alerts-schema/Alert]
        (ok (user-info/add-alert (:end-date body) (:alert-text body)))))))

