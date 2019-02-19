(ns terrain.routes.schemas.comments
  (:use [common-swagger-api.schema :only [StandardUserQueryParams]])
  (:require [common-swagger-api.schema.metadata.comments :as comment-schema]
            [schema.core :as s]))

(s/defschema RetractCommentQueryParams
  (apply dissoc comment-schema/RetractCommentQueryParams (keys StandardUserQueryParams)))
