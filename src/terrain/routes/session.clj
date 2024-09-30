(ns terrain.routes.session
  (:require [common-swagger-api.schema :refer [GET POST DELETE]]
            [terrain.services.user-sessions :refer [user-session remove-session]]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare body)

(defn secured-session-routes
  []
  (optional-routes
   [config/session-routes-enabled]

   (GET "/sessions" []
        (user-session))

   (POST "/sessions" [:as {body :body}]
         (user-session (slurp body)))

   (DELETE "/sessions" []
           (remove-session))))
