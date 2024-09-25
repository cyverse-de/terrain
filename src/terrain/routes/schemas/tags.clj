(ns terrain.routes.schemas.tags
  (:require [common-swagger-api.schema.metadata.tags :as tags]
            [schema.core :as s]))

(s/defschema TagId {:id tags/TagIdPathParam})

(def PatchTagsResponses
  (merge {200 {:schema      nil
               :description "The tags were attached or detached from the file or folder"}
          400 tags/PatchTags400Response
          404 tags/PatchTags404Response}
         tags/TagDefaultErrorResponses))

(def PostTagResponses
  (merge {200 {:schema      TagId
               :description "The tag was successfully created"}
          400 tags/PostTag400Response}
         tags/TagDefaultErrorResponses))

(def PatchTagResponses
  (merge {200 {:schema      nil
               :description "The tag was successfully updated"}
          400 tags/PostTag400Response}
         tags/TagDefaultErrorResponses))
