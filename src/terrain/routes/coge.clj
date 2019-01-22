(ns terrain.routes.coge
  (:use [compojure.api.core]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.coge]
        [terrain.services.coge]
        [terrain.util.service]
        [terrain.util])
  (:require [terrain.util.config :as config]))

(defn coge-routes
  []
  (optional-routes
   [config/coge-enabled]
   (context "/coge" []
     :tags ["coge"]

     (context "/genomes" []
       (GET "/" []
         :summary "Genome Search"
         :query [params GenomeSearchParams]
         :return GenomeSearchResponse
         :description "Searches the CoGe database for genomes matching a string."
         (ok (search-genomes params)))

       (POST "/:genome-id/export-fasta" []
         :summary "Genome Export"
         :path-params [genome-id :- GenomeIdPathParam]
         :query [params GenomeExportParams]
         :return GenomeExportResponse
         :description "Exports CoGe sequence data to a FASTA file in the CyVerse data store"
         (ok (export-fasta genome-id params)))

       (POST "/load" []
         :summary "View Genomes in CoGe"
         :body [body GenomeLoadRequest]
         :return GenomeLoadResponse
         (ok (get-genome-viewer-url body)))))))
