(ns terrain.routes.fileio
  (:require [common-swagger-api.schema :refer [context GET POST]]
            [common-swagger-api.schema.stats :as stats-schema]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [require-authentication current-user]]
            [terrain.routes.schemas.fileio :as fileio-schema]
            [terrain.services.fileio.controllers :as fio]
            [terrain.util :as util]
            [terrain.util.config :as config]
            [terrain.middleware :as mw]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare params file body)

(defn secured-fileio-routes
  "The routes for file IO endpoints."
  []
  (util/optional-routes
   [config/data-routes-enabled]

   (context "/fileio" []
     :tags ["fileio"]

     (GET "/download" []
       :summary "Retrieve File Contents"
       :description "Retrieves the contents of a file in the CyVerse Data Store."
       :query [params fileio-schema/FileDownloadQueryParams]

       ;; fio/download returns a Ring response map.
       (fio/download current-user params))

     (POST "/upload" []
       :summary "Upload a File"
       :description "Uploads a file to the CyVerse Data Store."
       :query [params fileio-schema/FileUploadQueryParams]
       :multipart-params [file :- fileio-schema/DataStoreUpload]
       ;; The overage check must run before the multipart middleware: parsing
       ;; the multipart body performs the upload, so checking afterwards would
       ;; store the file and then reject the request.
       :middleware [require-authentication mw/check-user-data-overages fio/wrap-file-upload]
       :return stats-schema/FileStat

       ;; The upload is handled in the middleware. All that remains to be done is to return the response body.
       (ok (select-keys file [:file])))

     (POST "/overwrite" []
       :summary "Overwrite a File"
       :description (str "Overwrites the contents of an existing file in the Data Store with the uploaded content. "
                         "Unlike the POST /terrain/secured/fileio/save endpoint, the content is forwarded as-is, so "
                         "binary files are safe. The file must exist already; to upload a new file, use the POST "
                         "/terrain/secured/fileio/upload endpoint.")
       :query [params fileio-schema/FileOverwriteQueryParams]
       :multipart-params [file :- fileio-schema/DataStoreUpload]
       ;; As on the upload route, the overage check must run before the
       ;; multipart middleware that performs the overwrite.
       :middleware [require-authentication mw/check-user-data-overages fio/wrap-file-overwrite]
       :return stats-schema/FileStat

       ;; The overwrite is handled in the middleware, like the upload endpoint above.
       (ok (select-keys file [:file])))

     (POST "/urlupload" []
       :summary "Upload a File from a URL"
       :description (str "Schedules a task to have the DE retrieve the contents of a new file in the data store from "
                         "an FTP, HTTP, or HTTPS URL.")
       :middleware [require-authentication mw/check-user-data-overages]
       :body [body fileio-schema/UrlUploadRequestBody]
       :return fileio-schema/UrlUploadResponseBody
       (ok (fio/urlupload current-user body)))

     (POST "/save" []
       :summary "Save a File"
       :description (str "Overwrites the contents of a file in the Data Store. The file must exist already for this "
                         "endpoint to work. To save a new file, use the POST /terrain/secured/fileio/saveas endpoint.")
       :middleware [require-authentication mw/check-user-data-overages]
       :body [body fileio-schema/FileSaveRequestBody]
       :return stats-schema/FileStat
       (ok (fio/save current-user body)))

     (POST "/saveas" []
       :summary "Save a New File"
       :description (str "Creates a new file in the data store. The file must not exist for this endpoint to work. "
                         "To overwrite an existing file, use the POST /terrain/secured/fileio/save endpoint.")
       :middleware [require-authentication mw/check-user-data-overages]
       :body [body fileio-schema/FileSaveRequestBody]
       :return stats-schema/FileStat
       (ok (fio/saveas current-user body))))))
