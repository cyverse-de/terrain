(ns terrain.routes.search
  "the routing code for search-related URL resources"
  (:use [clojure-commons.error-codes :only [missing-arg-response]]
        [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.search])
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

       (GET "/search-documentation" []
            :summary "Get additional documentation for the search endpoint"
            :description "Returns documentation of the available querydsl clauses and their
            arguments/types, plus the list of available sort fields."
            (ok (c-search/get-data-search-documentation)))

       (POST "/search" []
             :summary "Perform a data search"
             :body [body (doc-only SearchQuery SearchQueryDocs)]
             :description "Search utilizing the querydsl.
             This endpoint automatically filters results to those the user can see, and adds a
             \"permission\" field that summarizes the requesting user's effective permission on each result."
             (ok (c-search/do-data-search body)))

       (GET "/index" [q tags & opts]
            (if (or q tags)
              (search/search (:shortUsername user/current-user) q tags opts)
              (missing-arg-response "`q` or `tags`")))
       )))
