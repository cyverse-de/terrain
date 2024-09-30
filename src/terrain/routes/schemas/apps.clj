(ns terrain.routes.schemas.apps
  (:require [common-swagger-api.schema :refer [describe SortFieldDocs SortFieldOptionalKey]]
            [common-swagger-api.schema.apps :as apps-schema]
            [schema.core :refer [Any defschema enum optional-key]]))

;; Convert the keywords in AppSearchValidSortFields to strings,
;; so that the correct param format is passed through to the apps service.
(defschema AppSearchParams
  (merge apps-schema/AppSearchParams
         {SortFieldOptionalKey
          (describe (apply enum (map name apps-schema/AppSearchValidSortFields))
                    SortFieldDocs)}))

(def SupportEmailSummary "Send an Email to Support")
(def SupportEmailDescription "Sends an email to the Support team.")

;; The request body for the support-email endpoint.
(defschema SupportEmailRequest
  {(optional-key :email)
   (describe String (str "The email address to use in the FROM field of the email to Support. If no email address "
                         "is specified but the user is authenticated then the user's primary email address will be "
                         "used. Otherwise, a default email address will be used"))

   :fields
   (describe Any "Arbitrary key/value pairs to include in the email to Support")

   (optional-key :subject)
   (describe String "The email subject. If no subject is provided, a default subject will be used")})
