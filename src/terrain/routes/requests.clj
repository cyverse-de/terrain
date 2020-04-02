(ns terrain.routes.requests
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [schema.core :only [Any]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.util :only [optional-routes]])
  (:require [terrain.util.config :as config]
            [terrain.routes.schemas.requests :as schemas]
            [terrain.services.requests :as requests]))

(defn request-routes
  "Routes for submitting administrative requests."
  []
  (optional-routes
   [config/request-routes-enabled]

   (context "/requests" []
     :tags ["requests"]

     (context "/vice" []

       (GET "/" []
         :summary "List VICE Access Requests"
         :return schemas/ViceRequestListing
         :description "Returns VICE access requests that were submitted by the authentiated user."
         (ok (requests/list-vice-requests current-user)))

       (POST "/" []
         :summary "Request VICE Access"
         :body [body schemas/ViceRequestDetails]
         :return schemas/ViceRequest
         :description "Submits a request for VICE access for the authenticated user."
         (ok (requests/submit-vice-request current-user body)))))))
