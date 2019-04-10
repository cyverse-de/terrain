(ns terrain.routes.apps.categories
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps :only [AppIdParam]]
        [common-swagger-api.schema.apps.pipeline]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.categories :only [AppListingPagingParams]]
        [terrain.util :only [optional-routes]])
  (:require [common-swagger-api.schema.apps :as apps-schema]
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
