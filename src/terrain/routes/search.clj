(ns terrain.routes.search
  "the routing code for search-related URL resources"
  (:use [clojure-commons.error-codes :only [missing-arg-response]]
        [common-swagger-api.schema]
  (:require [terrain.auth.user-attributes :as user]
            [terrain.services.search :as search]
            [terrain.clients.search :as c-search]
            [terrain.util :as util]
            [terrain.util.config :as config]))


(defn secured-search-routes
  "The routes for search-related endpoints."
  []
  (util/optional-routes
    [config/search-routes-enabled]

    (context "/filesystem" []
       :tags ["filesystem"]

       (GET "/search-documentation" [:as req]
            (util/controller req c-search/get-data-search-documentation))
       (POST "/search" [:as req]
             (util/controller req c-search/do-data-search :params :body))

       (GET "/index" [q tags & opts]
            (if (or q tags)
              (search/search (:shortUsername user/current-user) q tags opts)
              (missing-arg-response "`q` or `tags`")))
       )))
