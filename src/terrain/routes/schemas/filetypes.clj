(ns terrain.routes.schemas.filetypes
  (:use [common-swagger-api.schema :only [describe NonBlankString]])
  (:require [common-swagger-api.schema.filetypes :as filetype-schema]))

(def FileType
  (assoc filetype-schema/FileType
    :path (describe NonBlankString "The iRODS path to the file")))

(def FileTypeReturn filetype-schema/FileTypeReturn)
