(ns terrain.routes.favorites
  (:use [common-swagger-api.schema :only [DELETE GET POST PUT context]]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.filesystem])
  (:require [terrain.services.metadata.favorites :as fave]
            [terrain.util :as util]
            [terrain.util.config :as config]))


(defn secured-favorites-routes
  []
  (util/optional-routes
   [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

   (context "/favorites" []
     :tags ["favorites"]

     (context "/filesystem" []

       (context "/:entry-id" []
         :path-params [entry-id :- DataIdPathParam]

         (PUT "/" []
           :summary "Add a Favorite File or Folder"
           :description "Adds a file or folder to the list of favorites."
           (fave/add-favorite entry-id)
           (ok))

         (DELETE "/" []
           :summary "Remove a Favorite File or Folder"
           :description "Rmoves a file or folder from the list of favorites."
           (fave/remove-favorite entry-id)
           (ok)))

       (GET "/" []
         :summary "List Favorite Files and Folders"
         :query [params FavoriteListingParams]
         :description "Lists files and folders that the user has marked as favorites."
         (ok (fave/list-favorite-data-with-stat params)))

       (DELETE "/" []
         :summary "Remove All Favorite Files or Folders"
         :query [params RemoveFavoritesQueryParams]
         :description "Removes all files, folders, or both from the list of favorites."
         (fave/remove-selected-favorites params)
         (ok)))

     (POST "/filter" [:as {body :body}]
       (fave/filter-accessible-favorites body)))))
