(ns terrain.routes.apps.categories
  (:require [common-swagger-api.routes]                     ;; for :description-file
            [common-swagger-api.schema :refer [context GET]]
            [common-swagger-api.schema.apps :as apps-schema]
            [common-swagger-api.schema.apps.categories :as schema]
            [common-swagger-api.schema.apps.pipeline]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [require-authentication]]
            [terrain.clients.apps.raw :as apps]
            [terrain.routes.schemas.categories :refer [AppListingPagingParams]]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations for path and query parameter bindings to avoid lint warnings.
(declare params system-id category-id community-id)

(defn app-category-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (context "/apps/categories" []
     :tags ["app-categories"]

     (GET "/" []
       :middleware [require-authentication]
       :query [params schema/CategoryListingParams]
       :return schema/AppCategoryListing
       :summary schema/AppCategoryListingSummary
       :description schema/AppCategoryListingDocs
       (ok (apps/get-app-categories params)))

     (GET "/featured" []
       :query [params AppListingPagingParams]
       :return schema/AppCategoryAppListing
       :summary schema/FeaturedAppListingSummary
       :description schema/FeaturedAppListingDocs
       (ok (apps/featured-apps params)))

     (GET "/:system-id/:category-id" []
       :middleware [require-authentication]
       :path-params [system-id :- apps-schema/SystemId
                     category-id :- apps-schema/AppCategoryIdPathParam]
       :query [params AppListingPagingParams]
       :return schema/AppCategoryAppListing
       :summary schema/AppCategoryAppListingSummary
       :description schema/AppCategoryAppListingDocs
       (ok (apps/apps-in-category system-id category-id params))))))

(defn app-community-routes
  []
  (optional-routes
   [#(and (config/app-routes-enabled)
          (config/metadata-routes-enabled))]

   (context "/apps/communities" []
     :tags ["app-communities"]

     (GET "/:community-id/apps" []
       :middleware [require-authentication]
       :path-params [community-id :- schema/AppCommunityGroupNameParam]
       :query [params AppListingPagingParams]
       :return apps-schema/AppListing
       :summary schema/AppCommunityAppListingSummary
       :description schema/AppCommunityAppListingDocs
       (ok (apps/apps-in-community community-id params))))))
