(ns terrain.routes.fileio
  (:use [common-swagger-api.schema])
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

     (GET "/download" [:as req]
       (util/controller req fio/download :params))

     (POST "/upload" [dest :as req]
       (util/controller req fio/upload :params req))

     (POST "/urlupload" [:as req]
       (util/controller req fio/urlupload :params :body))

     (POST "/save" [:as req]
       (util/controller req fio/save :params :body))

     (POST "/saveas" [:as req]
       (util/controller req fio/saveas :params :body)))))
