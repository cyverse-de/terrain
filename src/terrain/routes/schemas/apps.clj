(ns terrain.routes.schemas.apps
  (:use [common-swagger-api.schema]
        [schema.core :only [defschema enum]])
  (:require [common-swagger-api.schema.apps :as apps-schema]))

;; Convert the keywords in AppSearchValidSortFields to strings,
;; so that the correct param format is passed through to the apps service.
(defschema AppSearchParams
  (merge apps-schema/AppSearchParams
         {SortFieldOptionalKey
          (describe (apply enum (map name apps-schema/AppSearchValidSortFields))
                    SortFieldDocs)}))
