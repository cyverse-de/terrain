(ns terrain.routes.requests
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [schema.core :only [Any]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.util :only [optional-routes]])
  (:require [terrain.util.config :as config]
            [terrain.routes.schemas.requests :as schema]
            [terrain.services.requests :as requests]))

(defn request-routes
  "Routes for submitting administrative requests. The non-administrative routes have the request type in the path with
   the endpoints for each type of request documented separately. The reason for this is that the request details may
   vary by request type, and defining the endpoints this way is the easiest way to ensure that we can use a slightly
   different schema for each type of request."
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
         :return schema/ViceRequestSummary
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

(defn admin-request-type-routes
  "Routes for listing and configuring administrative request types."
  []

  (optional-routes
   [#(and (config/admin-routes-enabled) (config/request-routes-enabled))]

   (context "/request-types" []
     :tags ["admin-request-types"]

     (GET "/" []
       :summary "List Administrative Request Types"
       :return schema/RequestTypeListing
       :description "Lists existing administrative request types"
       (ok (requests/list-request-types)))

     (context "/:name" []
       :path-params [name :- schema/RequestTypeName]

       (GET "/" []
         :summary "Get Request Type"
         :return schema/RequestType
         :description "Returns information about a single administrative request type"
         (ok (requests/get-request-type name)))

       (POST "/" []
         :summary "Add Request Type"
         :return schema/RequestType
         :description "Adds a new type of administrative request"
         :query [params schema/RequestTypeQueryParams]
         (ok (requests/add-request-type name params)))

       (PATCH "/" []
         :summary "Update Request Type"
         :return schema/RequestType
         :description "Updates an existing administrative request type"
         :query [params schema/RequestTypeQueryParams]
         (ok (requests/update-request-type name params)))))))

(defn admin-request-routes
  "Routes for administering requests. The administrative routes are defined in a more consolidated fashion than their
   non-administrative counterparts. The reason for doing this is that the request details don't need to be documented
   in the administrative routes (because request details are never changed after the request is submitted.)
   Consolidating the routes also makes it easier to have one administrative request processing page to handle multiple
   different types of requests if we want to."
  []
  (optional-routes
   [#(and (config/admin-routes-enabled) (config/request-routes-enabled))]

   (context "/requests" []
     :tags ["admin-requests"]

     (GET "/" []
       :summary "List Administrative Requests"
       :query [params schema/RequestListingQueryParams]
       :return schema/RequestListing
       :description "Lists administrative requests, optionally filtered by request type or requesting user."
       (ok (requests/list-requests params)))

     (context "/:request-id" []
       :path-params [request-id :- schema/RequestId]

       (GET "/" []
         :summary "Get Request Information"
         :return schema/Request
         :description "Returns information about an existing request."
         (ok (requests/get-request request-id)))

       (POST "/in-progress" []
         :summary "Mark Request as in Progress"
         :body [body schema/RequestUpdateMessage]
         :return schema/RequestUpdate
         :description "Marks a request as being in progress."
         (ok (requests/request-in-progress current-user request-id body)))

       (POST "/rejected" []
         :summary "Mark Request as Rejected"
         :body [body schema/RequestUpdateMessage]
         :return schema/RequestUpdate
         :description "Marks a request as having been rejected."
         (ok (requests/request-rejected current-user request-id body)))

       (context "/approved" []

         (POST "/" []
           :summary "Approve a Request"
           :body [body schema/RequestUpdateMessage]
           :return schema/RequestUpdate
           :description "Marks a request as approved and performs any actions required to fulfill the request."
           (ok (requests/request-approved current-user request-id body)))

         (POST "/vice" []
           :summary "Approve a Request for VICE Access"
           :body [body schema/ViceRequestApprovalMessage]
           :return schema/RequestUpdate
           :description "Marks a request for VICE access and performs actions required to fulfill the request."
           (ok (requests/request-approved current-user request-id body))))))))
