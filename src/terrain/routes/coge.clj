(ns terrain.routes.coge
  (:require [common-swagger-api.schema :refer [context GET POST]]
            [ring.util.http-response :refer [ok]]
            [terrain.routes.schemas.coge :as coge-schema]
            [terrain.services.coge :as coge]
            [terrain.util.config :as config]
            [terrain.util :refer [optional-routes]]
            [terrain.util.service]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare params genome-id body)

(defn coge-routes
  []
  (optional-routes
   [config/coge-enabled]
   (context "/coge" []
     :tags ["coge"]

     (context "/genomes" []
       (GET "/" []
         :summary "Genome Search"
         :query [params coge-schema/GenomeSearchParams]
         :return coge-schema/GenomeSearchResponse
         :description "Searches the CoGe database for genomes matching a string."
         (ok (coge/search-genomes params)))

       (POST "/:genome-id/export-fasta" []
         :summary "Genome Export"
         :path-params [genome-id :- coge-schema/GenomeIdPathParam]
         :query [params coge-schema/GenomeExportParams]
         :return coge-schema/GenomeExportResponse
         :description "Exports CoGe sequence data to a FASTA file in the CyVerse data store"
         (ok (coge/export-fasta genome-id params)))

       (POST "/load" []
         :summary "View Genomes in CoGe"
         :body [body coge-schema/GenomeLoadRequest]
         :return coge-schema/GenomeLoadResponse
         (ok (coge/get-genome-viewer-url body)))))))
