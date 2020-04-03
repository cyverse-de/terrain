(ns terrain.clients.requests
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [terrain.util.config :as config]))

(defn- requests-url
  [& components]
  (str (apply curl/url (config/requests-base) components)))

(def ^:private register-request-type
  "Registers a request type if it hasn't already been registered and returns the response body of the request
   type registration. This function is memoized so that we don't unnecessarily hammer the requests service."
  (memoize (fn [request-type]
             (:body (http/post (requests-url "request-types" request-type)) {:as :json}))))

(defn list-requests
  "Lists requests, optionally filtered by requesting user, request type, and whether or not completed requests
   should be included in the listing. The available options are keywords with the same names as the query
   parameters in the request listing service."
  [opts]
  (:body (http/get (requests-url "requests")
                   {:query-params opts
                    :as           :json})))

(defn submit-request
  "Submits a request to the requests service."
  [request-type username details]
  (register-request-type request-type)
  (:body (http/post (requests-url "requests")
                    {:query-params {:user username}
                     :form-params  {:request_type request-type
                                    :details      details}
                     :content-type :json
                     :as           :json})))

(defn get-request
  "Obtains information about the request with the given ID."
  [request-id]
  (:body (http/get (requests-url "requests" request-id)
                   {:as :json})))

(defn update-request
  "Updates a request."
  [username request-id message request-status-code]
  (:body (http/post (requests-url "requests" request-id "status")
                    {:query-params {:user username}
                     :form-params  {:message message
                                    :status  request-status-code}
                     :content-type :json
                     :as           :json})))
