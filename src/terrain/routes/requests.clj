(ns terrain.routes.requests
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [schema.core :only [Any]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.util :only [optional-routes]])
  (:require [schema-tools.core :as st]
            [terrain.util.config :as config]
            [terrain.routes.schemas.requests :as schema]
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
         :return schema/ViceRequestListing
         :description "Returns VICE access requests that were submitted by the authentiated user."
         (ok (requests/list-vice-requests current-user)))

       (POST "/" []
         :summary "Request VICE Access"
         :body [body schema/ViceRequestDetails]
         :return (st/dissoc schema/ViceRequest :updates)
         :description "Submits a request for VICE access for the authenticated user."
         (ok (requests/submit-vice-request current-user body)))

       (context "/:request-id" []
         :path-params [request-id :- schema/RequestId]

         (GET "/" []
           :summary "Get VICE Request Information"
           :return schema/ViceRequest
           :description "Returns information about an existing request."
           (->> (requests/get-vice-request request-id)
                (requests/validate-request-user current-user)
                ok)))))))
