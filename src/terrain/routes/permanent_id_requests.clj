(ns terrain.routes.permanent-id-requests
  (:require [common-swagger-api.schema :refer [context GET POST]]
            [common-swagger-api.schema.permanent-id-requests :as schema]
            [ring.util.http-response :refer [ok]]
            [terrain.routes.schemas.permanent-id-requests :as permanent-id-request-schema]
            [terrain.services.permanent-id-requests :as permanent-id-requests]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare params body request-id)

(defn permanent-id-request-routes
  "The routes for Permanent ID Request endpoints."
  []
  (optional-routes
    [config/filesystem-routes-enabled]

    (context
      "/permanent-id-requests" []
      :tags ["permanent-id-requests"]

      (GET "/" []
           :query [params permanent-id-request-schema/PermanentIDRequestListPagingParams]
           :return permanent-id-request-schema/PermanentIDRequestList
           :summary schema/PermanentIDRequestListSummary
           :description schema/PermanentIDRequestListDescription
           (ok (permanent-id-requests/list-permanent-id-requests params)))

      (POST "/" []
            :body [body permanent-id-request-schema/PermanentIDRequest]
            :return permanent-id-request-schema/PermanentIDRequestDetails
            :summary schema/PermanentIDRequestSummary
            :description schema/PermanentIDRequestDescription
            (ok (permanent-id-requests/create-permanent-id-request body)))

      (GET "/status-codes" []
           :return schema/PermanentIDRequestStatusCodeList
           :summary schema/PermanentIDRequestStatusCodeListSummary
           :description schema/PermanentIDRequestStatusCodeListDescription
           (ok (permanent-id-requests/list-permanent-id-request-status-codes)))

      (GET "/types" []
           :return schema/PermanentIDRequestTypeList
           :summary schema/PermanentIDRequestTypesSummary
           :description schema/PermanentIDRequestTypesDescription
           (ok (permanent-id-requests/list-permanent-id-request-types)))

      (GET "/:request-id" []
           :path-params [request-id :- schema/PermanentIDRequestIdParam]
           :return permanent-id-request-schema/PermanentIDRequestDetails
           :summary schema/PermanentIDRequestDetailsSummary
           :description schema/PermanentIDRequestDetailsDescription
           (ok (permanent-id-requests/get-permanent-id-request request-id))))))

(defn admin-permanent-id-request-routes
  "The admin routes for Permanent ID Request endpoints."
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/filesystem-routes-enabled))]

    (context "/permanent-id-requests" []
             :tags ["admin-permanent-id-requests"]

             (GET "/" []
                  :query [params permanent-id-request-schema/PermanentIDRequestListPagingParams]
                  :return permanent-id-request-schema/PermanentIDRequestList
                  :summary schema/PermanentIDRequestAdminListSummary
                  :description schema/PermanentIDRequestAdminListDescription
                  (ok (permanent-id-requests/admin-list-permanent-id-requests params)))

             (context "/:request-id" []
                      :path-params [request-id :- schema/PermanentIDRequestIdParam]

                      (GET "/" []
                           :return permanent-id-request-schema/PermanentIDRequestDetails
                           :summary schema/PermanentIDRequestAdminDetailsSummary
                           :description schema/PermanentIDRequestAdminDetailsDescription
                           (ok (permanent-id-requests/admin-get-permanent-id-request request-id)))

                      (POST "/doi" [request-id]
                            :return permanent-id-request-schema/PermanentIDRequestDetails
                            :summary "Create a DOI"
                            :description
                            "This endpoint will create a DOI using the
                            [DataCite API](https://support.datacite.org/docs/api-create-dois)
                            and the requested folder's metadata,
                            add the new DOI to the folder's metadata,
                            move the folder to a curated directory,
                            then set the Permanent ID Request's status to `Completed`.
                            If an error is encountered during this process,
                            then the Permanent ID Request's status will be set to `Failed`."
                            (ok (permanent-id-requests/create-permanent-id request-id)))

                      (POST "/ezid" [request-id]
                            :deprecated true
                            :return permanent-id-request-schema/PermanentIDRequestDetails
                            :summary "Create a Permanent ID"
                            :description
                            "This endpoint has been replaced by the `POST .../doi` endpoint above,
                             which now only supports creating DOIs."
                            (ok (permanent-id-requests/create-permanent-id request-id)))

                      (GET "/preview-submission" [request-id]
                           :summary "Preview DataCite XML submission"
                           :description
                           "This endpoint returns the DataCite XML generated from the data set's metadata,
                            which will be used in the submission when creating a DOI."
                           (ok (permanent-id-requests/preview-datacite-xml request-id)))

                      (POST "/status" []
                            :body [body permanent-id-request-schema/PermanentIDRequestStatusUpdate]
                            :return permanent-id-request-schema/PermanentIDRequestDetails
                            :summary schema/PermanentIDRequestAdminStatusUpdateSummary
                            :description schema/PermanentIDRequestAdminStatusUpdateDescription
                            (ok (permanent-id-requests/update-permanent-id-request request-id body)))))))
