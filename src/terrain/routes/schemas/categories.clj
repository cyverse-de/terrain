(ns terrain.routes.schemas.categories
  (:use [common-swagger-api.schema]
        [schema.core :only [defschema enum]])
  (:require [common-swagger-api.schema.apps :as apps-schema]
            [common-swagger-api.schema.apps.categories :as categories-schema]))

;; Convert the keywords in AppListingValidSortFields to strings,
;; so that the correct param format is passed through to the apps service.
(def AppListingValidSortFieldStrings
  {SortFieldOptionalKey
   (describe (apply enum (map name apps-schema/AppListingValidSortFields))
             SortFieldDocs)})

(defschema AppListingPagingParams
  (merge apps-schema/AppListingPagingParams
         AppListingValidSortFieldStrings))

(defschema OntologyAppListingPagingParams
  (merge categories-schema/OntologyAppListingPagingParams
         AppListingValidSortFieldStrings))
