(ns terrain.routes.data
  (:require [common-swagger-api.routes]                     ;; Required for :description-file
            [common-swagger-api.schema :refer [context describe POST GET DELETE]]
            [schema.core :as s]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [require-authentication current-user]]
            [terrain.clients.data-info :as data]
            [terrain.clients.saved-searches :as saved]
            [terrain.routes.schemas.filesystem :refer [SharingRequest SharingResponse UnshareRequest UnshareResponse]]
            [terrain.routes.schemas.filetypes :refer [FileType FileTypeReturn TypesList]]
            [terrain.services.sharing :refer [share unshare]]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]
            [terrain.util.transformers :refer [add-current-user-to-map]]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare body req)

(defn secured-data-routes
  "The routes for data sharing endpoints."
  []
  (optional-routes
   [config/data-routes-enabled]

   (context "/filetypes" []
     :tags ["data"]

     (POST "/type" []
       :middleware [require-authentication]
       :summary "Set or Remove File Type Labels"
       :body [body (describe FileType "The file type to set")]
       :return FileTypeReturn
       :description-file "docs/post-filetypes.md"
       (ok (data/set-file-type (add-current-user-to-map {}) body)))

     (GET "/type-list" [:as req]
       :summary "List Supported File Type Labels"
       :return TypesList
       :description "Lists the file type labels supported by the Discovery Environment."
       (ok (data/get-type-list))))

   (POST "/share" []
     :middleware [require-authentication]
     :tags ["data"]
     :summary "Share Files or Folders"
     :body [body SharingRequest]
     :return SharingResponse
     :description "Allows users to share files and folders with other users."
     (ok (share body)))

   (POST "/unshare" [:as req]
     :middleware [require-authentication]
     :tags ["data"]
     :summary "Unshare Files or Folders"
     :body [body UnshareRequest]
     :return UnshareResponse
     :description "Allows users to revoke permissions to files and folders that have been granted to other users."
     (ok (unshare body)))

   (context "/saved-searches" []
     :tags ["data"]

     (GET "/" []
       :middleware [require-authentication]
       :summary "Get Saved Searches"
       :return (describe s/Any "Previously stored saved searches")
       :description-file "docs/get-saved-searches.md"
       (ok (saved/get-saved-searches (:username current-user))))

     (POST "/" []
       :middleware [require-authentication]
       :summary "Set Saved Searches"
       :body [body (describe s/Any "The saved searches to store")]
       :description-file "docs/post-saved-searches.md"
       (saved/set-saved-searches (:username current-user) body)
       (ok))

     (DELETE "/" []
       :middleware [require-authentication]
       :summary "Delete Saved Searches"
       :description "Deletes all previously saved searches for the authenticated user."
       (saved/delete-saved-searches (:username current-user))
       (ok)))))
