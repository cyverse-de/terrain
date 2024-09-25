(ns terrain.routes.schemas.filesystem.navigation
  (:require [common-swagger-api.schema :refer [describe NonBlankString]]
            [common-swagger-api.schema.data.navigation :as nav-schema]
            [schema.core :as s]
            [schema-tools.core :as schema-tools]))

(def HasSubDirsParam (describe Boolean "Flag indicating whether the folder has any subdirectories"))

(s/defschema DirectoryQueryParams
  {(s/optional-key :path) (describe NonBlankString "The IRODS path to a directory")})

(s/defschema RootListing
  (merge nav-schema/RootListing
         {:hasSubDirs HasSubDirsParam}))

(s/defschema FolderListing
  (merge nav-schema/FolderListing
         {:hasSubDirs HasSubDirsParam
          :badName    (describe Boolean "Flag indicating whether the folder should be disabled/avoided in the client UI")
          :isFavorite (describe Boolean "Flag indicating whether the folder is marked as a favorite by the user")
          (s/optional-key :folders)
                      (describe [(s/recursive #'FolderListing)] "Subdirectories of this directory")}))

(s/defschema DirectoryResponse
  ((comp schema-tools/optional-keys merge)
   FolderListing
   {:roots (describe [FolderListing]
                     "This `roots` key will only be returned when the `path` param is omitted in the request,
                      and then only this `roots` key will be included at the top-level of the response")}))

(s/defschema RootResponses
  (update-in nav-schema/NavigationRootResponses
             [200 :schema]
             merge {:roots [RootListing]}))

(s/defschema DirectoryResponses
  (update-in nav-schema/NavigationResponses
             [200]
             merge {:schema DirectoryResponse}))
