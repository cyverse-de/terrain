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

         (DELETE "/" [entry-id]
           (fave/remove-favorite entry-id)))

       (GET "/" [sort-col sort-dir limit offset entity-type info-type]
         (fave/list-favorite-data-with-stat sort-col sort-dir limit offset entity-type info-type))

       (DELETE "/" [entity-type]
         (fave/remove-selected-favorites entity-type)))

     (POST "/filter" [:as {body :body}]
       (fave/filter-accessible-favorites body)))))
