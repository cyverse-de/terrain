(ns terrain.routes.tags
  (:require [common-swagger-api.schema :refer [context GET DELETE PATCH POST]]
            [common-swagger-api.schema.metadata :refer [TargetIdParam]]
            [common-swagger-api.schema.metadata.tags :as schema]
            [compojure.api.middleware :as middleware]
            [ring.util.http-response :refer [ok]]
            [terrain.routes.schemas.tags :as ts]
            [terrain.services.metadata.tags :as tags]
            [terrain.util :as util]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare entry-id params body tag-id)

(defn secured-tag-routes
  []
  (util/optional-routes
   [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

   (context "/filesystem/entry" []
            :tags ["tags"]

      (GET "/tags" []
        :summary schema/GetTagsSummary
        :description schema/GetTagsDescription
        :responses schema/GetTagsResponses
        (ok (tags/list-all-attached-tags)))

      (DELETE "/tags" []
        :summary schema/DeleteTagsSummary
        :description schema/DeleteTagsDescription
        :coercion middleware/no-response-coercion
        :responses schema/DeleteTagsResponses
        (tags/remove-all-attached-tags)
        (ok))

     (context "/:entry-id/tags" []
        :path-params [entry-id :- TargetIdParam]

        (GET "/" []
          :summary schema/GetAttachedTagSummary
          :description schema/GetAttachedTagDescription
          :responses schema/GetAttachedTagResponses
          (ok (tags/list-attached-tags entry-id)))

        (PATCH "/" []
          :query [params schema/TagTypeEnum]
          :body [body schema/TagIdList]
          :summary schema/PatchTagsSummary
          :description schema/PatchTagsDescription
          :coercion middleware/no-response-coercion
          :responses ts/PatchTagsResponses
          (tags/handle-patch-file-tags entry-id params body)
          (ok))))

   (context "/tags" []
            :tags ["tags"]
     (GET "/suggestions" []
        :query [params (dissoc schema/TagSuggestQueryParams :user)]
        :summary schema/GetTagSuggestionsSummary
        :description schema/GetTagSuggestionsDescription
        :responses schema/GetTagSuggestionsResponses
        (ok (tags/suggest-tags (:contains params) (:limit params))))

     (context "/user" []
        (GET "/" []
          :summary schema/GetUserTagsSummary
          :description schema/GetUserTagsDescription
          :responses schema/GetUserTagsResponses
          (ok (tags/list-user-tags)))

        (DELETE "/" []
          :summary schema/DeleteUserTagsSummary
          :description schema/DeleteUserTagsDescription
          :coercion middleware/no-response-coercion
          :responses schema/DeleteUserTagsResponses
          (tags/delete-all-user-tags)
          (ok))

        (POST "/" []
          :body [body schema/TagRequest]
          :summary schema/PostTagSummary
          :description schema/PostTagDescription
          :responses ts/PostTagResponses
          (ok (tags/create-user-tag body)))

        (context "/:tag-id" []
          :path-params [tag-id :- schema/TagIdPathParam]

          (PATCH "/" []
            :body [body schema/TagUpdateRequest]
            :summary schema/PatchTagSummary
            :description schema/PatchTagDescription
            :coercion middleware/no-response-coercion
            :responses ts/PatchTagResponses
            (tags/update-user-tag tag-id body)
            (ok))

          (DELETE "/" []
            :summary schema/DeleteTagSummary
            :description schema/DeleteTagDescription
            :coercion middleware/no-response-coercion
            :responses schema/DeleteTagResponses
            (tags/delete-user-tag tag-id)
            (ok)))))))
