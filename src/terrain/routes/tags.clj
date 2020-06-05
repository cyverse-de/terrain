(ns terrain.routes.tags
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.metadata :only [TargetIdParam]]
        [ring.util.http-response :only [ok]])
  (:require [common-swagger-api.schema.metadata.tags :as schema]
            [terrain.services.metadata.tags :as tags]
            [terrain.util :as util]
            [terrain.util.config :as config]))


(defn secured-tag-routes
  []
  (util/optional-routes
   [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

   (context "/filesystem/entry" []
            :tags ["tags"]

      (GET "/tags" []
        :summary schema/GetTagsSummary
        :description schema/GetTagsDescription
        :return schema/AttachedTagsListing
        (ok (tags/list-all-attached-tags)))

      (DELETE "/tags" []
        :summary schema/DeleteTagsSummary
        :description schema/DeleteTagsDescription
        (tags/remove-all-attached-tags)
        (ok))

     (context "/:entry-id/tags" []
        :path-params [entry-id :- TargetIdParam]

        (GET "/" []
          :summary schema/GetAttachedTagSummary
          :description schema/GetAttachedTagDescription
          :return schema/TagList
          (ok (tags/list-attached-tags entry-id)))

        (PATCH "/" []
          :query [params schema/TagTypeEnum]
          :body [body schema/TagIdList]
          :summary schema/PatchTagsSummary
          :description schema/PatchTagsDescription
          (tags/handle-patch-file-tags entry-id params body)
        (ok))))

   (context "/tags" []
            :tags ["tags"]
     (GET "/suggestions" []
        :query [params (dissoc schema/TagSuggestQueryParams :user)]
        :summary schema/GetTagSuggestionsSummary
        :description schema/GetTagSuggestionsDescription
        :return schema/TagList
        (ok (tags/suggest-tags (:contains params) (:limit params))))

     (context "/user" []
        (GET "/" []
          :summary schema/GetUserTagsSummary
          :description schema/GetUserTagsDescription
          :return schema/TagList
          (ok (tags/list-user-tags)))

        (DELETE "/" []
          :summary schema/DeleteUserTagsSummary
          :description schema/DeleteUserTagsDescription
          (tags/delete-all-user-tags)
          (ok))

        (POST "/" []
          :body [body schema/TagRequest]
          :summary schema/PostTagSummary
          :description schema/PostTagDescription
          :return schema/TagId
          (ok (tags/create-user-tag body)))

        (context "/:tag-id" []
          :path-params [tag-id :- schema/TagIdPathParam]

          (PATCH "/" []
            :body [body schema/TagUpdateRequest]
            :summary schema/PatchTagSummary
            :description schema/PatchTagDescription
            (tags/update-user-tag tag-id body)
            (ok))

          (DELETE "/" []
            :summary schema/DeleteTagSummary
            :description schema/DeleteTagDescription
            (tags/delete-user-tag tag-id)
            (ok)))))))
