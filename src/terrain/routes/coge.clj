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
         :summary "Service Configuration Listing"
         :query [params GenomeSearchParams]
         :return GenomeSearchResponse
         :description "Returns service configuration information with passwords redacted."
         (ok (search-genomes params)))

       (POST "/:genome-id/export-fasta" [genome-id :as {:keys [params]}]
         (success-response (export-fasta genome-id params)))

       (POST "/load" [:as {:keys [body]}]
         (success-response (get-genome-viewer-url body)))))))
