(ns terrain.routes.filesystem.tickets
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.filesystem.tickets]
        [terrain.util :only [optional-routes]]
        [terrain.util.transformers :only [add-current-user-to-map]])
  (:require [common-swagger-api.schema.data :as data-schema]
            [common-swagger-api.schema.data.tickets :as schema]
            [terrain.clients.data-info :as data]
            [terrain.util.config :as config]))

;; FIXME: The `coerce-public-param` middleware is only for backwards compatibility
;; and should be removed in the next version of the API
(defn- public-param->boolean
  "Sets the `public` key in `params` to `true` if its string value is `1` or `true`,
   otherwise sets it to `false`."
  [{public "public" :as params}]
  (assoc params "public" (or (= public "1") (= public "true"))))

(defn- coerce-public-param
  "Middleware to convert public param string values of `1` to a boolean `true`."
  [handler]
  (fn [request]
    (handler (update request :query-params public-param->boolean))))

(defn filesystem-ticket-routes
  "The routes for filesystem ticket endpoints."
  []

  (optional-routes
    [config/filesystem-routes-enabled]

    (context
      "/filesystem" []
      :tags ["filesystem"]

      (POST "/tickets" []
            :middleware [coerce-public-param]
            :query [params AddTicketQueryParams]
            :body [{:keys [paths]} data-schema/Paths]
            :responses schema/AddTicketResponses
            :summary schema/AddTicketSummary
            :description schema/AddTicketDocs
            (ok (data/add-tickets (add-current-user-to-map {}) paths params)))

      (POST "/delete-tickets" []
            :body [body schema/Tickets]
            :responses schema/DeleteTicketResponses
            :summary schema/DeleteTicketSummary
            :description schema/DeleteTicketDocs
            (ok (data/delete-tickets (add-current-user-to-map {}) body)))

      (POST "/list-tickets" []
            :body [body data-schema/Paths]
            :responses schema/ListTicketResponses
            :summary schema/ListTicketSummary
            :description schema/ListTicketDocs
            (ok (data/list-tickets (add-current-user-to-map {}) body))))))
