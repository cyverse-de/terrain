(ns terrain.routes.schemas.requests
  (:use [common-swagger-api.schema :only [describe NonBlankString]]
        [schema.core :only [defschema enum optional-key]])
  (:require [schema-tools.core :as st])
  (:import [java.util UUID]))

(def RequestId (describe UUID "The request ID"))

(defschema RequestUpdate
  {:created_date  (describe NonBlankString "The date and time the update occurred")
   :id            (describe UUID "The update ID")
   :message       (describe String "The message entered by the person who updated the requst")
   :status        (describe String "The request status code")
   :updating_user (describe String "The username of the person who updated the request")})

(defschema ViceRequestDetails
  {(optional-key :name)
   (describe NonBlankString "The user's name")

   :institution
   (describe NonBlankString "The name of the institution that user works for")

   (optional-key :email)
   (describe NonBlankString "The user's email address")

   :intended_use
   (describe NonBlankString "The reason for requesting VICE access")

   :funding_award_number
   (describe NonBlankString "The award number from any relevant funding agency")

   :references
   (describe [NonBlankString] "The names of other CyVerse users who can vouch for the user")

   :orcid
   (describe NonBlankString "The user's ORCID identifier")

   :concurrent_jobs
   (describe Integer "The requested number of concurrently running VICE jobs")})

(defschema ViceRequest
  {:id              RequestId
   :request_type    (describe NonBlankString "The name of the request type")
   :requesting_user (describe NonBlankString "The username of the requesting user")
   :details         (describe ViceRequestDetails "The request details")
   :updates         (describe [RequestUpdate] "Updates that were made to the request")})

(defschema ViceRequestListing
  {:requests (describe [(st/dissoc ViceRequest :updates)] "A listing of VICE access requests")})
