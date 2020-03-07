(ns terrain.routes.schemas.filesystem
  (:use [common-swagger-api.schema :only [describe NonBlankString]]
        [common-swagger-api.schema.filetypes :only [ValidInfoTypesEnum]])
  (:require [common-swagger-api.schema.data :as data-schema]
            [common-swagger-api.schema.stats :as stats-schema]
            [schema.core :as s]
            [schema-tools.core :as st])
  (:import [java.util UUID]))

(def DataIdPathParam data-schema/DataIdPathParam)
(def FavoritesSortColumnEnum (s/enum :name :id :lastmodified :datecreated :size))
(def SortDirEnum (s/enum :asc :desc))
(def EntityTypeEnum (s/enum :any :file :folder))

(s/defschema PathShareRequest
  {:path (describe NonBlankString "The path to the file or folder to share")
   :permission (describe data-schema/PermissionEnum "The permission level to grant to the user")})

(s/defschema PathShareResponse
  (assoc PathShareRequest
    :success
    (describe Boolean "`true` if the file or folder was shared successfully")

    (s/optional-key :error)
    (describe s/Any "Additional information about the error if the file or folder was not shared successfully")))

(s/defschema UserShareRequest
  {:user  (describe NonBlankString "The username of the person to grant permissions to")
   :paths (describe [PathShareRequest] "The paths and permission levels to grant to the user")})

(s/defschema UserShareResponse
  (assoc (dissoc UserShareRequest :paths)
    :sharing (describe [PathShareResponse] "Responses to indicate whether or not each path was shared successfully")))

(s/defschema SharingRequest
  {:sharing (describe [UserShareRequest] "The sharing requests to process")})

(s/defschema SharingResponse
  {:sharing (describe [UserShareResponse] "Status information about each sharing request")})

(s/defschema PathUnshareResponse
  {:success
   (describe Boolean "`true` if the file or folder was unshared successfully")

   :path
   (describe NonBlankString "The path to the file or folder being unshared")

   (s/optional-key :error)
   (describe s/Any "Additional information about the error if the file or folder was not unshared successfully")})

(s/defschema UserUnshareRequest
  {:user  (describe NonBlankString "The username of the person to revoke permissions from")
   :paths (describe [NonBlankString] "The paths to the files or folders being unshared")})

(s/defschema UserUnshareResponse
  (assoc (dissoc UserUnshareRequest :paths)
    :unshare
    (describe [PathUnshareResponse] "Responses to indicate whether or not each path was unshared successfully")))

(s/defschema UnshareRequest
  {:unshare (describe [UserUnshareRequest] "The unsharing requests to process")})

(s/defschema UnshareResponse
  {:unshare (describe [UserUnshareResponse] "Status information about each sharing response")})

(s/defschema FavoriteListingParams
  {:sort-col
   (describe FavoritesSortColumnEnum "The column to sort the listing by")

   :sort-dir
   (describe SortDirEnum "The direction to sort the listing")

   :limit
   (describe Long "The maximum number of entries to return in the listing")

   :offset
   (describe Long "The sequential index of the first entry to display in the listing")

   (s/optional-key :entity-type)
   (describe EntityTypeEnum "This parameter can be used to limit the listing to files or folders")

   (s/optional-key :info-type)
   (describe [ValidInfoTypesEnum] "This parameter can be used to limit the listing to specific file types")})

(s/defschema DirStatInfo
  (assoc stats-schema/DirStatInfo
    :isFavorite (describe Boolean "True if this folder is marked as a favorite")))

(s/defschema FileStatInfo
  (assoc (dissoc stats-schema/FileStatInfo :infoType :md5)
    :isFavorite (describe Boolean "True if this file is marked as a favorite")))

(s/defschema FavoriteListing
  {:files   (describe [FileStatInfo] "The files in the favorite listing")
   :folders (describe [DirStatInfo] "The folders in the favorite listing")
   :total   (describe Long "Total number of files and folders in the listing")})

(s/defschema RemoveFavoritesQueryParams
  {(s/optional-key :entity-type)
   (describe EntityTypeEnum "This parameter can be used to limit the removal to only files or folders")})

(s/defschema UuidsToFilter
  {:filesystem (describe [UUID] "The list of UUIDs to filter")})

(s/defschema FilteredUuids
  {:filesystem (describe [UUID] "The filtered list of UUIDs")})

(s/defschema FolderListingParams
  (merge (st/required-keys data-schema/FolderListingParams [:limit])
         {:path (describe String "The path to the folder in the data store.")}))
