(ns terrain.routes.schemas.admin
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema]]))

(defschema StatusResponse
  {:iRODS             (describe Boolean "True if the data store appears to be functional")
   :jex               (describe Boolean "True if the job execution system appears to be functional")
   :apps              (describe Boolean "True if the apps service appears to be functional")
   :notificationagent (describe Boolean "True if hte notification agent appears to be functional")
   :ezid              (describe String "A brief description of the status of the EZID API")})
