(ns terrain.routes.schemas.comments
  (:require [common-swagger-api.schema :refer [StandardUserQueryParams]]
            [common-swagger-api.schema.metadata.comments :as comment-schema]
            [schema.core :as s]))

(s/defschema RetractCommentQueryParams
  (apply dissoc comment-schema/RetractCommentQueryParams (keys StandardUserQueryParams)))
