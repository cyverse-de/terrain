(ns terrain.routes.schemas.categories
  (:require [common-swagger-api.schema :refer [describe SortFieldOptionalKey SortFieldDocs]]
            [common-swagger-api.schema.apps :as apps-schema]
            [common-swagger-api.schema.apps.categories :as categories-schema]
            [schema.core :refer [defschema enum]]))

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
