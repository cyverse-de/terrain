(ns terrain.routes.apps.reference-genomes
  (:require [common-swagger-api.schema :refer [context GET]]
            [common-swagger-api.schema.apps.reference-genomes :as schema]
            [ring.util.http-response :refer [ok]]
            [terrain.clients.apps.raw :as apps]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare params)
(declare reference-genome-id)

(defn reference-genomes-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/reference-genomes" []
      :tags ["reference-genomes"]

      (GET "/" []
           :query [params schema/ReferenceGenomeListingParams]
           :return schema/ReferenceGenomesList
           :summary schema/ReferenceGenomeListingSummary
           :description schema/ReferenceGenomeListingDocs
           (ok (apps/list-reference-genomes params)))

      (GET "/:reference-genome-id" []
           :path-params [reference-genome-id :- schema/ReferenceGenomeIdParam]
           :return schema/ReferenceGenome
           :summary schema/ReferenceGenomeDetailsSummary
           :description schema/ReferenceGenomeDetailsDocs
           (ok (apps/get-reference-genome reference-genome-id))))))
