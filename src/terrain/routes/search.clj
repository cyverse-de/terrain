(ns terrain.routes.search
  "the routing code for search-related URL resources"
  (:use [clojure-commons.error-codes :only [missing-arg-response]]
        [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [schema.core :only [Any]]
        [terrain.routes.schemas.search])
  (:require [terrain.clients.search :as c-search]
            [terrain.util :as util]
            [terrain.util.config :as config]
            [terrain.middleware :as mw]))

(defn secured-search-routes
  "The routes for search-related endpoints."
  []
  (util/optional-routes
   [config/search-routes-enabled]

   (context "/filesystem" []
     :tags ["filesystem"]

     (GET "/search-documentation" []
       :middleware [mw/check-es-enabled]
       :summary "Get additional documentation for the search endpoint"
       :return Any
       :description "Returns documentation of the available querydsl clauses and their
                    arguments/types, plus the list of available sort fields."
       (ok (c-search/get-data-search-documentation)))

     (POST "/search" []
       :middleware [mw/check-es-enabled]
       :summary "Perform a data search"
       :return Any
       :body [body SearchRequest]
       :description "Search utilizing the querydsl.
                This endpoint automatically filters results to those the user can see, and adds a
                `permission` field that summarizes the requesting user's effective permission on each result."
       (ok (c-search/do-data-search body))))))
