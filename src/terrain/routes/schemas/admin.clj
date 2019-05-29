(ns terrain.routes.schemas.admin
  (:use [common-swagger-api.schema
         :only [describe
                SortFieldDocs
                SortFieldOptionalKey]]
        [schema.core :only [defschema enum]])
  (:require [common-swagger-api.schema.apps.admin.apps :as apps-schema]))

;; Convert the keywords in AdminAppSearchValidSortFields to strings,
;; so that the correct param format is passed through to the apps service.
(defschema AdminAppSearchParams
  (merge apps-schema/AdminAppSearchParams
         {SortFieldOptionalKey
          (describe (apply enum (map name apps-schema/AdminAppSearchValidSortFields))
                    SortFieldDocs)

          apps-schema/AppSubsetOptionalKey
          (describe (apply enum (map name apps-schema/AppSubsets))
                    apps-schema/AppSubsetDocs :default :public)}))

(defschema StatusResponse
  {:iRODS             (describe Boolean "True if the data store appears to be functional")
   :jex               (describe Boolean "True if the job execution system appears to be functional")
   :apps              (describe Boolean "True if the apps service appears to be functional")
   :notificationagent (describe Boolean "True if hte notification agent appears to be functional")
   :ezid              (describe String "A brief description of the status of the EZID API")})
