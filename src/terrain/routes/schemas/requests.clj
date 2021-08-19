(ns terrain.routes.schemas.requests
  (:use [common-swagger-api.schema :only [describe NonBlankString]]
        [schema.core :only [Any defschema enum optional-key]])
  (:require [schema-tools.core :as st])
  (:import [java.util UUID]))

(def RequestId (describe UUID "The request ID"))
(def RequestTypeName (describe String "The name of the request type"))

(defschema RequestListingQueryParams
  {(optional-key :include-completed)
   (describe Boolean "If set to true, completed requests will be included in the listing")

   (optional-key :request-type)
   (describe String "If specified, only requests of the selected type will be included in the listing")

   (optional-key :requesting-user)
   (describe String "If specified, only requests submitted by the selected user will be included in the listing")})

(defschema RequestUpdate
  {:created_date  (describe NonBlankString "The date and time the update occurred")
   :id            (describe UUID "The update ID")
   :message       (describe String "The message entered by the person who updated the requst")
   :status        (describe String "The request status code")
   :updating_user (describe String "The username of the person who updated the request")})

(defschema Request
  {:id              RequestId
   :request_type    (describe NonBlankString "The name of the request type")
   :requesting_user (describe NonBlankString "The username of the requesting user")
   :details         (describe Any "The request details")
   :updates         (describe [RequestUpdate] "Updates that were made to the request")})

(defschema RequestSummary
  (st/assoc
   (st/dissoc Request :updates)

   :created_date
   (describe NonBlankString "The date and time the request was submitted")

   :status
   (describe String "The most recently assigned request status code")

   :updated_date
   (describe NonBlankString "The date and time the request was most recently updated")))

(defschema RequestListing
  {:requests (describe [RequestSummary] "A listing of administrative requests")})

(defschema RequestUpdateMessage
  {(optional-key :message)
   (describe NonBlankString "The message to store with the request.")})

(defschema ViceRequestApprovalMessage
  (st/assoc RequestUpdateMessage
            (optional-key :concurrent_jobs)
            (describe Integer "If provided, this setting overrides the number of jobs the user requested.")))

(defschema ViceRequestDetails
  {(optional-key :name)
   (describe NonBlankString "The user's name")

   (optional-key :email)
   (describe NonBlankString "The user's email address")

   :intended_use
   (describe NonBlankString "The reason for requesting VICE access")

   :concurrent_jobs
   (describe Integer "The requested number of concurrently running VICE jobs")})

(defschema ViceRequest
  (st/assoc Request
            :details (describe ViceRequestDetails "The request details")))

(defschema ViceRequestSummary
  (st/assoc RequestSummary
            :details (describe ViceRequestDetails "The request details")))

(defschema ViceRequestListing
  {:requests (describe [ViceRequestSummary] "A listing of VICE access requests")})

(defschema RequestTypeQueryParams
  {(optional-key :maximum-concurrent-requests-per-user)
   (describe Long "The maximum number of active requests of this type for a user")

   (optional-key :maximum-requests-per-user)
   (describe Long "The absolute maximum number of requests of this type for a user")})

(defschema RequestType
  {:id
   (describe UUID "The request type ID")

   :name
   (describe String "The request type name")

   :maximum_concurrent_requests_per_user
   (describe Long "The maximum number of active requests of this type for a user")

   :maximum_requests_per_user
   (describe Long "The absolute maximum number of requests of this type for a user")})

(defschema RequestTypeListing
  {:request_types (describe [RequestType] "The list of request types")})
