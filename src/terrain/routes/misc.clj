(ns terrain.routes.misc
  (:require [clojure.string :as string]
            [common-swagger-api.schema :refer [routes GET]]
            [ring.util.http-response :refer [internal-server-error ok]])
  (:import [java.util UUID]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare expecting req)

(defn unsecured-misc-routes
  []
  (routes
    (GET "/" [:as {{expecting :expecting} :params :as req}]
      (if (and expecting (not= expecting "terrain"))
        (internal-server-error (str "The infinite is attainable with Terrain!\nError: expecting " expecting "."))
        (ok "The infinite is attainable with Terrain!\n")))

    (GET "/uuid" []
      (string/upper-case (str (UUID/randomUUID))))))
