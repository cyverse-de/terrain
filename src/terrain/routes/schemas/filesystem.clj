(ns terrain.routes.schemas.filesystem
  (:use [common-swagger-api.schema :only [describe NonBlankString]])
  (:require [common-swagger-api.schema.data :as data-schema]
            [schema.core :as s])
  (:import [java.util UUID]))

(def DataIdPathParam data-schema/DataIdPathParam)

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
