(ns terrain.routes.permanent-id-requests
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.permanent-id-requests]
        [terrain.services.permanent-id-requests]
        [terrain.util :only [optional-routes]])
  (:require [common-swagger-api.schema.permanent-id-requests :as schema]
            [terrain.util.config :as config]))

(defn permanent-id-request-routes
  "The routes for Permanent ID Request endpoints."
  []
  (optional-routes
    [config/filesystem-routes-enabled]

    (context
      "/permanent-id-requests" []
      :tags ["permanent-id-requests"]

      (GET "/" []
           :query [params PermanentIDRequestListPagingParams]
           :return PermanentIDRequestList
           :summary schema/PermanentIDRequestListSummary
           :description schema/PermanentIDRequestListDescription
           (ok (list-permanent-id-requests params)))

      (POST "/" []
            :body [body PermanentIDRequest]
            :return PermanentIDRequestDetails
            :summary schema/PermanentIDRequestSummary
            :description schema/PermanentIDRequestDescription
            (ok (create-permanent-id-request body)))

      (GET "/status-codes" []
           :return schema/PermanentIDRequestStatusCodeList
           :summary schema/PermanentIDRequestStatusCodeListSummary
           :description schema/PermanentIDRequestStatusCodeListDescription
           (ok (list-permanent-id-request-status-codes)))

      (GET "/types" []
           :return schema/PermanentIDRequestTypeList
           :summary schema/PermanentIDRequestTypesSummary
           :description schema/PermanentIDRequestTypesDescription
           (ok (list-permanent-id-request-types)))

      (GET "/:request-id" []
           :path-params [request-id :- schema/PermanentIDRequestIdParam]
           :return PermanentIDRequestDetails
           :summary schema/PermanentIDRequestDetailsSummary
           :description schema/PermanentIDRequestDetailsDescription
           (ok (get-permanent-id-request request-id))))))

(defn admin-permanent-id-request-routes
  "The admin routes for Permanent ID Request endpoints."
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/filesystem-routes-enabled))]

    (context "/permanent-id-requests" []
             :tags ["admin-permanent-id-requests"]

             (GET "/" []
                  :query [params PermanentIDRequestListPagingParams]
                  :return PermanentIDRequestList
                  :summary schema/PermanentIDRequestAdminListSummary
                  :description schema/PermanentIDRequestAdminListDescription
                  (ok (admin-list-permanent-id-requests params)))

             (context "/:request-id" []
                      :path-params [request-id :- schema/PermanentIDRequestIdParam]

                      (GET "/" []
                           :return PermanentIDRequestDetails
                           :summary schema/PermanentIDRequestAdminDetailsSummary
                           :description schema/PermanentIDRequestAdminDetailsDescription
                           (ok (admin-get-permanent-id-request request-id)))

                      (POST "/doi" [request-id]
                            :return PermanentIDRequestDetails
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
                            (ok (create-permanent-id request-id)))

                      (POST "/ezid" [request-id]
                            :deprecated true
                            :return PermanentIDRequestDetails
                            :summary "Create a Permanent ID"
                            :description
                            "This endpoint has been replaced by the `POST .../doi` endpoint above,
                             which now only supports creating DOIs."
                            (ok (create-permanent-id request-id)))

                      (GET "/preview-submission" [request-id]
                           :summary "Preview DataCite XML submission"
                           :description
                           "This endpoint returns the DataCite XML generated from the data set's metadata,
                            which will be used in the submission when creating a DOI."
                           (ok (preview-datacite-xml request-id)))

                      (POST "/status" []
                            :body [body PermanentIDRequestStatusUpdate]
                            :return PermanentIDRequestDetails
                            :summary schema/PermanentIDRequestAdminStatusUpdateSummary
                            :description schema/PermanentIDRequestAdminStatusUpdateDescription
                            (ok (update-permanent-id-request request-id body)))))))
