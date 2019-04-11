(ns terrain.routes.apps.categories
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps :only [AppIdParam AppListing]]
        [common-swagger-api.schema.apps.pipeline]
        [common-swagger-api.schema.ontologies :only [OntologyClassIRIParam
                                                     OntologyHierarchyFilterParams]]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.categories]
        [terrain.util :only [optional-routes]])
  (:require [common-swagger-api.routes]                     ;; for :description-file
            [common-swagger-api.schema.apps :as apps-schema]
            [common-swagger-api.schema.apps.categories :as schema]
            [terrain.clients.apps.raw :as apps]
            [terrain.util.config :as config]))

(defn app-category-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (context "/apps/categories" []
      :tags ["app-categories"]

      (GET "/" []
           :query [params schema/CategoryListingParams]
           :return schema/AppCategoryListing
           :summary schema/AppCategoryListingSummary
           :description schema/AppCategoryListingDocs
           (ok (apps/get-app-categories params)))

      (GET "/:system-id/:category-id" []
           :path-params [system-id :- apps-schema/SystemId
                         category-id :- apps-schema/AppCategoryIdPathParam]
           :query [params AppListingPagingParams]
           :return schema/AppCategoryAppListing
           :summary schema/AppCategoryAppListingSummary
           :description schema/AppCategoryAppListingDocs
           (ok (apps/apps-in-category system-id category-id params))))))

(defn app-ontology-routes
  []
  (optional-routes
    [#(and (config/app-routes-enabled)
           (config/metadata-routes-enabled))]

    (context "/apps/hierarchies" []
      :tags ["app-hierarchies"]

      (GET "/" []
           :summary schema/AppHierarchiesListingSummary
           :description-file "docs/apps/categories/hierarchies-listing.md"
           (ok (apps/get-app-category-hierarchies)))

      (context "/:root-iri" []
        :path-params [root-iri :- OntologyClassIRIParam]

        (GET "/" []
             :query [params OntologyHierarchyFilterParams]
             :summary schema/AppCategoryHierarchyListingSummary
             :description-file "docs/apps/categories/category-hierarchy-listing.md"
             (ok (apps/get-app-category-hierarchy root-iri params)))

        (GET "/apps" []
             :query [params OntologyAppListingPagingParams]
             :return AppListing
             :summary schema/AppCategoryAppListingSummary
             :description-file "docs/apps/categories/hierarchy-app-listing.md"
             (ok (apps/get-hierarchy-app-listing root-iri params)))

        (GET "/unclassified" []
             :query [params OntologyAppListingPagingParams]
             :return AppListing
             :summary schema/AppHierarchyUnclassifiedListingSummary
             :description-file "docs/apps/categories/hierarchy-unclassified-app-listing.md"
             (ok (apps/get-unclassified-app-listing root-iri params)))))))
