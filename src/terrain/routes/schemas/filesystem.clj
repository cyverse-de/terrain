(ns terrain.routes.schemas.filesystem
  (:use [common-swagger-api.schema :only [describe]])
  (:import [java.util UUID]))

(def DataIdPathParam (describe UUID "The UUID assigned to the file or folder"))
