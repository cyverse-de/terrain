(ns terrain.routes.schemas.email
  (:require
   [common-swagger-api.schema :as s]
   [schema.core :refer [Any Keyword defschema optional-key]]))

(def SendEmailSummary "Send an Email")

(def SendEmailDescription "Submits a request to send an email to a user.")

(def SendEmailResponse
  {:status (s/describe s/NonBlankString "The status of the request")})

(defschema SendEmailRequestBody
  {:to                         (s/describe s/NonBlankString "The email address to send the message to")
   (optional-key :cc)          (s/describe [s/NonBlankString] "Email addresses to send courtecy copies to")
   (optional-key :bcc)         (s/describe [s/NonBlankString] "Email addresses to send blind courtecy copies to")
   :from_addr                  (s/describe s/NonBlankString "The email address of the message sender")
   :from_name                  (s/describe s/NonBlankString "The name of the message sender")
   :subject                    (s/describe s/NonBlankString "The message subject")
   :template                   (s/describe s/NonBlankString "The name of the email template to use")
   :values                     (s/describe {Keyword Any} "The values to plug into the email template")
   (optional-key :attachments) (s/describe [{:filename (s/describe s/NonBlankString "The attachment's file name")
                                             :data     (s/describe s/NonBlankString "The base64 encoded file data")}]
                                           "Optional email file attachments")})
