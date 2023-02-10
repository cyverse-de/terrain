(ns terrain.routes.schemas.filesystem
  (:use [common-swagger-api.schema :only [describe NonBlankString transform-enum]]
        [common-swagger-api.schema.filetypes :only [ValidInfoTypesEnum]])
  (:require [common-swagger-api.schema.data :as data-schema]
            [common-swagger-api.schema.stats :as stats-schema]
            [common-swagger-api.schema.subjects :as subjects-schema]
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
  {(s/optional-key :user)
   (describe NonBlankString "The username of the person to grant permissions to. Mutually exclusive with subject.")

   (s/optional-key :subject)
   (describe subjects-schema/BaseSubject "The subject (user or group) to grant permissions to. Mutually exclusive with user.")

   :paths
   (describe [PathShareRequest] "The paths and permission levels to grant to the user")})

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
  {(s/optional-key :user)
   (describe NonBlankString "The username of the person to revoke permissions from. Mutually exclusive with subject.")

   (s/optional-key :subject) 
   (describe subjects-schema/BaseSubject "The subject (user or group) to revoke permissions from. Mutually exclusive with user.")

   :paths
   (describe [NonBlankString] "The paths to the files or folders being unshared")})

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
  (as-> data-schema/FolderListingParams schema
    (st/required-keys schema [:limit])
    (st/update schema :sort-field transform-enum name)
    (st/update schema :entity-type transform-enum name)
    (st/assoc schema :path (describe String "The path to the folder in the data store."))
    (st/assoc schema (s/optional-key :sort-col) (st/get-in schema [:sort-field]))))

(s/defschema PagedItemListEntry
  {:infoType
   (describe (s/maybe ValidInfoTypesEnum) "The info-type of the item being listed or null for folders")

   :path
   (describe String "The path to the item")

   :date-created
   (describe Long "The date and time that the item was created as a UNIX epoch")

   :permission
   (describe data-schema/PermissionEnum "The user's permission-level for the item")

   :date-modified
   (describe Long "The date and time that the item was most recently modified as a UNIX epoch")

   :file-size
   (describe Long "The size of the item in bytes or 0 for folders")

   :badName
   (describe Boolean "True if the item name may cuase problems in the DE")

   :isFavorite
   (describe Boolean "True if the user has marked the item as a favorite")

   :label
   (describe String "The base name of the item")

   :id
   (describe UUID "The ID assigned to the item")})

(s/defschema PagedFolderListing
  (merge PagedItemListEntry
         {:folders
          (describe [PagedItemListEntry] "The list of subfolders")

          :hasSubDirs
          (describe Boolean "True if the folder has subfolders")

          :total
          (describe Long "The total number of subfolders and files in the folder")

          :totalBad
          (describe Long (str "The total number of folder or file names in the listing that may cause problems "
                              "in the DE"))

          :files
          (describe [PagedItemListEntry] "The list of files in the folder")}))
