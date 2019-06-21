(ns terrain.routes.fileio
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.routes.schemas.fileio])
  (:require [terrain.util.config :as config]
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
       (fio/download current-user params))

     (POST "/upload" [dest :as req]
       (util/controller req fio/upload :params req))

     (POST "/urlupload" [:as req]
       (util/controller req fio/urlupload :params :body))

     (POST "/save" [:as req]
       (util/controller req fio/save :params :body))

     (POST "/saveas" [:as req]
       (util/controller req fio/saveas :params :body)))))
