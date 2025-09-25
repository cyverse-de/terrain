(ns terrain.routes.email
  (:require
   [common-swagger-api.schema :refer [context POST]]
   [ring.util.http-response :refer [ok]]
   [terrain.auth.user-attributes :refer [require-service-account]]
   [terrain.routes.schemas.email :as s]
   [terrain.services.email :as email-service]
   [terrain.util :refer [optional-routes]]
   [terrain.util.config :as config]))

(declare body)
(defn service-account-email-routes
  []
  (optional-routes
    [config/email-api-routes-enabled]

    (context "/email" []
      :tags ["service-account-email"]

      (POST "/" []
        :middleware [[require-service-account ["cyverse-emailer"]]]
        :summary s/SendEmailSummary
        :description s/SendEmailDescription
        :body [body s/SendEmailRequestBody]
        :return s/SendEmailResponse
        (ok (email-service/send-email body))))))
