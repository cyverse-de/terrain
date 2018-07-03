(ns terrain.routes.token
  (:use [compojure.core :only [routes GET]]
        [terrain.services.oauth :only [get-token]]))

(defn token-routes
  []
  (routes
   (GET "/token" [:as {{:strs [authorization]} :headers}]
     (get-token authorization))))
