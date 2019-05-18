(ns terrain.routes.apps.reference-genomes
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.util :only [optional-routes]])
  (:require [common-swagger-api.schema.apps.reference-genomes :as schema]
            [terrain.clients.apps.raw :as apps]
            [terrain.util.config :as config]))

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
