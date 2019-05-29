(ns terrain.routes.apps.admin.reference-genomes
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps.reference-genomes
         :only [ReferenceGenome
                ReferenceGenomeIdParam]]
        [ring.util.http-response :only [ok]]
        [terrain.util :only [optional-routes]]
        [terrain.util.config :as config])
  (:require [common-swagger-api.schema.apps.admin.reference-genomes :as schema]
            [terrain.clients.apps.raw :as apps]))

(defn admin-reference-genomes-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/app-routes-enabled))]

    (context "/reference-genomes" []
      :tags ["admin-reference-genomes"]

      (POST "/" []
            :body [body schema/ReferenceGenomeAddRequest]
            :return ReferenceGenome
            :summary schema/ReferenceGenomeAddSummary
            :description schema/ReferenceGenomeAddDocs
            (ok (apps/admin-add-reference-genome body)))

      (DELETE "/:reference-genome-id" []
              :path-params [reference-genome-id :- ReferenceGenomeIdParam]
              :query [params schema/ReferenceGenomeDeletionParams]
              :summary schema/ReferenceGenomeDeleteSummary
              :description schema/ReferenceGenomeDeleteDocs
              (ok (apps/admin-delete-reference-genome reference-genome-id params)))

      (PATCH "/:reference-genome-id" []
             :path-params [reference-genome-id :- ReferenceGenomeIdParam]
             :body [body schema/ReferenceGenomeUpdateRequest]
             :return ReferenceGenome
             :summary schema/ReferenceGenomeUpdateSummary
             :description schema/ReferenceGenomeUpdateDocs
             (ok (apps/admin-update-reference-genome body reference-genome-id))))))
