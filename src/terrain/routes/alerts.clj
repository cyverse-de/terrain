(ns terrain.routes.alerts
  (:require [terrain.clients.user-info :as user-info]
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
            ;; body params for start/end/alert text
        (ok))

      (DELETE "/" []
              ;; body params for end/alert text to delete
        (ok)))))

