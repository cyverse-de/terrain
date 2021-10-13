(ns terrain.routes.schemas.permanent-id-requests
  (:use [common-swagger-api.schema :only [describe]])
  (:require [common-swagger-api.schema.permanent-id-requests :as schema]
            [common-swagger-api.schema.stats :as stats-schema]
            [schema-tools.core :as st]
            [schema.core :as s])
  (:import [java.util UUID]))

(s/defschema PermanentIDRequest
  (st/merge schema/PermanentIDRequest
            {:folder (describe UUID "The UUID of the data folder for which the persistent ID is being requested")}))

(s/defschema PermanentIDRequestBase
  (st/merge schema/PermanentIDRequestBase
            {:folder (describe stats-schema/DirStatInfo "The target folder's details")}))

(s/defschema PermanentIDRequestStatusUpdate
  (st/dissoc schema/PermanentIDRequestStatusUpdate :permanent_id))

(s/defschema PermanentIDRequestDetails
  (st/merge schema/PermanentIDRequestDetails
            PermanentIDRequestBase
            {:requested_by (describe s/Any "The user that submitted the Permanent ID Request")}))

(s/defschema PermanentIDRequestListing
  (st/merge schema/PermanentIDRequestListing PermanentIDRequestBase))

(s/defschema PermanentIDRequestList
  (st/merge schema/PermanentIDRequestList
            {:requests (describe [PermanentIDRequestListing] "A list of Permanent ID Requests")}))
