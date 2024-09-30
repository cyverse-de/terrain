(ns terrain.routes.schemas.admin
  (:require [common-swagger-api.schema
             :refer [describe
                     NonBlankString
                     SortFieldDocs
                     SortFieldOptionalKey]]
            [common-swagger-api.schema.apps :as apps-schema]
            [common-swagger-api.schema.apps.admin.apps :as admin-apps-schema]
            [schema.core :refer [defschema enum]]))

;; Convert Date params and keywords in enum values to strings,
;; so that the correct param formats are passed through to the apps service.
(defschema AdminAppSearchParams
  (merge admin-apps-schema/AdminAppSearchParams
         {apps-schema/AppJobStatsStartDateOptionalParam
          (describe NonBlankString apps-schema/AppJobStatsStartDateParamDocs)

          apps-schema/AppJobStatsEndDateOptionalParam
          (describe NonBlankString apps-schema/AppJobStatsEndDateParamDocs)

          SortFieldOptionalKey
          (describe (apply enum (map name admin-apps-schema/AdminAppSearchValidSortFields))
                    SortFieldDocs)

          admin-apps-schema/AppSubsetOptionalKey
          (describe (apply enum (map name admin-apps-schema/AppSubsets))
                    admin-apps-schema/AppSubsetDocs :default :public)}))

(defschema StatusResponse
  {:iRODS             (describe Boolean "True if the data store appears to be functional")
   :jex               (describe Boolean "True if the job execution system appears to be functional")
   :apps              (describe Boolean "True if the apps service appears to be functional")
   :notificationagent (describe Boolean "True if the notification agent appears to be functional")
   :datacite          (describe Boolean "True if the DataCite API responds with an OK status")})
