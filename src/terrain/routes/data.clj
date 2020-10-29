(ns terrain.routes.data
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.services.sharing :only [share unshare]]
        [terrain.auth.user-attributes]
        [terrain.routes.schemas.filetypes]
        [terrain.routes.schemas.filesystem]
        [terrain.util]
        [terrain.util.transformers :only [add-current-user-to-map]])
  (:require [common-swagger-api.routes]                     ;; Required for :description-file
            [schema.core :as s]
            [terrain.util.config :as config]
            [terrain.clients.data-info :as data]
            [terrain.clients.saved-searches :as saved]))

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
