(ns terrain.routes.pref
  (:require [common-swagger-api.schema :refer [GET POST DELETE]]
            [terrain.services.user-prefs :as prefs]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare body)

(defn secured-pref-routes
  []
  (optional-routes
   [config/pref-routes-enabled]

   (GET "/preferences" []
        (prefs/do-get-prefs))

   (POST "/preferences" [:as {body :body}]
         (prefs/do-post-prefs (slurp body)))

   (DELETE "/preferences" []
           (prefs/remove-prefs))))
