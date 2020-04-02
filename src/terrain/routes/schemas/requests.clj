(ns terrain.routes.schemas.requests
  (:use [common-swagger-api.schema :only [describe NonBlankString]]
        [schema.core :only [defschema enum optional-key]])
  (:import [java.util UUID]))

(defschema ViceRequestDetails
  {(optional-key :name)
   (describe NonBlankString "The user's name")

   :institution
   (describe NonBlankString "The name of the institution that user works for")

   (optional-key :email)
   (describe NonBlankString "The user's email address")

   :intended-use
   (describe NonBlankString "The reason for requesting VICE access")

   :funding-award-number
   (describe NonBlankString "The award number from any relevant funding agency")

   (optional-key :references)
   (describe [NonBlankString] "The names of other CyVerse users who can vouch for the user")

   (optional-key :orcid)
   (describe NonBlankString "The user's ORCID identifier")

   (optional-key :concurrent-jobs)
   (describe Integer "The requested number of concurrently running VICE jobs")})

(defschema ViceRequest
  {:id              (describe UUID "The request ID")
   :request_type    (describe NonBlankString "The name of the request type")
   :requesting_user (describe NonBlankString "The username of the requesting user")
   :details         (describe ViceRequestDetails "The request details")})

(defschema ViceRequestListing
  {:requests (describe [ViceRequest] "A listing of VICE access requests")})
