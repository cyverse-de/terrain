(ns terrain.routes.tags
  (:use [common-swagger-api.schema :only [DELETE GET PATCH POST]])
  (:require [terrain.services.metadata.tags :as tags]
            [terrain.util :as util]
            [terrain.util.config :as config]))


(defn secured-tag-routes
  []
  (util/optional-routes
   [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

   (GET "/filesystem/entry/tags" []
     (tags/list-all-attached-tags))

   (DELETE "/filesystem/entry/tags" []
     (tags/remove-all-attached-tags))

   (GET "/filesystem/entry/:entry-id/tags" [entry-id]
     (tags/list-attached-tags entry-id))

   (PATCH "/filesystem/entry/:entry-id/tags" [entry-id type :as {body :body}]
     (tags/handle-patch-file-tags entry-id type body))

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
