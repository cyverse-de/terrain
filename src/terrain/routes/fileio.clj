(ns terrain.routes.fileio
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.routes.schemas.fileio])
  (:require [common-swagger-api.schema.stats :as stats-schema]
            [terrain.util.config :as config]
            [terrain.services.fileio.controllers :as fio]
            [terrain.util :as util]))


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
       :query [params FileDownloadQueryParams]

       ;; fio/download returns a Ring response map.
       (fio/download current-user params))

     (POST "/upload" []
       :summary "Upload a File"
       :description "Uploads a file to the CyVerse Data Store."
       :query [params FileUploadQueryParams]
       :multipart-params [file :- DataStoreUpload]
       :middleware [fio/wrap-file-upload]
       :return stats-schema/FileStat

       ;; The upload is handled in the middleware. All that remains to be done is to return the response body.
       (ok (select-keys file [:file])))

     (POST "/urlupload" []
       :summary "Upload a File from a URL"
       :description (str "Schedules a task to have the DE retrieve the contents of a new file in the data store from "
                         "an FTP, HTTP, or HTTPS URL.")
       :body [body UrlUploadRequestBody]
       :return UrlUploadResponseBody
       (ok (fio/urlupload current-user body)))

     (POST "/save" []
       :summary "Save a File"
       :description (str "Overwrites the contents of a file in the Data Store. The file must exist already for this "
                         "endpoint to work. To save a new file, use the POST /terrain/secured/fileio/saveas endpoint.")
       :body [body FileSaveRequestBody]
       :return stats-schema/FileStat
       (ok (fio/save current-user body)))

     (POST "/saveas" [:as req]
       (util/controller req fio/saveas :params :body)))))
