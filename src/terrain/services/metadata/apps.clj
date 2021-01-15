(ns terrain.services.metadata.apps
  (:use [clojure.java.io :only [reader]]
        [terrain.util.transformers :only [secured-params]]
        [terrain.util.service :only [success-response]])
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]
            [terrain.clients.iplant-groups :as ipg]
            [terrain.clients.apps.raw :as apps-client]
            [terrain.clients.notifications :as dn]
            [terrain.util.email :as email]))

(defn import-tools
  "This service will import deployed components into the DE and send
   notifications if notification information is included and the deployed
   components are successfully imported."
  [json]
  (let [response (apps-client/admin-add-tools json)]
    (dorun (map dn/send-tool-notification (:tools json)))
    response))

(defn submit-tool-request
  "Submits a tool request on behalf of the user found in the request params."
  [body]
  (let [tool-req     (apps-client/submit-tool-request body)
        username     (string/replace (:submitted_by tool-req) #"@.*" "")
        user-details (ipg/format-like-trellis (ipg/lookup-subject-add-empty username username))]
    (email/send-tool-request-email tool-req user-details)
    tool-req))

(defn send-support-email
  "Sends a support email from the user."
  [body]
  (email/send-support-email body))
