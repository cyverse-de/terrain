(ns terrain.routes.schemas.fileio
  (:use [common-swagger-api.schema :only [describe]]
        [common-swagger-api.schema.stats :only [DataItemPathParam]]
        [schema.core :only [defschema]]))

(defschema FileDownloadQueryParams
  {:path (describe DataItemPathParam "The path to the file to download in the data store.")})
