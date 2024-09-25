(ns terrain.routes.schemas.filetypes
  (:require [common-swagger-api.schema :refer [describe NonBlankString]]
            [common-swagger-api.schema.filetypes :as filetype-schema]
            [schema.core :as s]))

(s/defschema FileType
  (assoc filetype-schema/FileType
    :path (describe NonBlankString "The iRODS path to the file")))

(def FileTypeReturn filetype-schema/FileTypeReturn)
(def TypesList filetype-schema/TypesList)
