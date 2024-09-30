(ns terrain.routes.schemas.fileio
  (:require [common-swagger-api.schema :refer [describe NonBlankString]]
            [common-swagger-api.schema.stats :refer [FileStat]]
            [ring.swagger.upload :as upload]
            [schema.core :as s]))

(s/defschema FileDownloadQueryParams
  {:path (describe NonBlankString "The path to the file to download in the data store.")})

(s/defschema FileUploadQueryParams
  {:dest (describe NonBlankString "The destination directory for the uploaded file.")})

(def DataStoreUpload
  "Schema for a data store upload file parameter. The multipart request has to be processed in middleware because
  the input stream for the file contents is closed before the body of the endpoint implementation is reached. We're
  handling this by forwarding the request to the data-info service in custom middleware. The custom middleware then
  places the file stat information in the multipart params, which can then be returned as the response body to the
  upload endpoint."
  (upload/->Upload
   (assoc FileStat
     :filename     (describe String "The name of the file being uploaded")
     :content-type (describe String "The MIME type of the file being uploaded"))))

(s/defschema UrlUploadRequestBody
  {:dest    (describe NonBlankString "The destination directory for the uploaded file.")
   :address (describe NonBlankString "The URL to retrieve the file contents from.")})

(s/defschema UrlUploadResponseBody
  {:msg   (describe NonBlankString "A brief message indicating the result of the request.")
   :url   (describe NonBlankString "The URL that the file contents are being retrieved from.")
   :label (describe NonBlankString "The name of the uploaded file.")
   :dest  (describe NonBlankString "The destination directory for the uploaded file.")})

(s/defschema FileSaveRequestBody
  {:content (describe String "The contents of the file to save.")
   :dest    (describe NonBlankString "The path to the file to save in the data store.")})
