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
        :return nil
        (ok (tags/remove-all-attached-tags)))

      (GET "/:entry-id/tags" []
        :path-params [entry-id :- TargetIdParam]
        :summary schema/GetAttachedTagSummary
        :description schema/GetAttachedTagDescription
        :return schema/TagList
        (ok (tags/list-attached-tags entry-id)))

      (PATCH "/:entry-id/tags" [entry-id type :as {body :body}]
        (tags/handle-patch-file-tags entry-id type body))
      )

   (GET "/tags/suggestions" [contains limit]
     (tags/suggest-tags contains limit))

   (GET "/tags/user" []
     (tags/list-user-tags))

   (DELETE "/tags/user" []
     (tags/delete-all-user-tags))

   (POST "/tags/user" [:as {body :body}]
     (tags/create-user-tag body))

   (PATCH "/tags/user/:tag-id" [tag-id :as {body :body}]
     (tags/update-user-tag tag-id body))

   (DELETE "/tags/user/:tag-id" [tag-id]
     (tags/delete-user-tag tag-id))))
