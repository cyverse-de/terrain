(ns terrain.services.requests
  (:use [potemkin :only [import-vars]])
  (:require [clojure-commons.exception-util :as cxu]
            [terrain.clients.analyses :as ac]
            [terrain.clients.requests :as rc]))

(import-vars
 [terrain.clients.requests
  get-request])

;; Request type constants
(def vice-request-type "vice")
(def vice-request-type-opts
  {:maximum-requests-per-user            1
   :maximum-concurrent-requests-per-user 10})

(defn list-request-types
  "List request types."
  []
  (rc/list-request-types))

(defn get-request-type
  "Obtains information about the request type with the given name."
  [name]
  (rc/get-request-type name))

(defn add-request-type
  "Adds a request type of the given name with the given options."
  [name opts]
  (rc/add-request-type name opts))

(defn update-request-type
  "Updates the request type of the given name to use the given options."
  [name opts]
  (rc/update-request-type name opts))

(defn list-requests
  "Lists requests for administrative endpoints."
  [params]
  (rc/list-requests params))

(defn list-vice-requests
  "Lists VICE requests. for the currently authenticated user."
  [{username :shortUsername}]
  (rc/list-requests {:request-type    vice-request-type
                     :requesting-user username}))

(defn- add-user-to-request-details
  [{name :commonName :keys [email]} details]
  (assoc details
         :name name
         :email email))

(defn submit-vice-request
  "Submits a VICE request. Details about the currently authenticated user are automatically added to the request."
  [{username :shortUsername :as user} details]
  (rc/submit-request
   vice-request-type
   vice-request-type-opts
   username
   (add-user-to-request-details user details)))

(defn- validate-request-type
  "Verifies that a request has the expected type. This is useful for endpoints where the request type is included
   in the URL path."
  [request-type {request-id :id :as request}]
  (when-not (= request-type (:request_type request))
    (cxu/not-found (str request-type " request " request-id " not found")))
  request)

(def get-vice-request
  "Gets information about a VICE request. If the request exists but is not a VICE request then an exception will be
   thrown to cause the service endpoint to return a 404."
  (comp (partial validate-request-type vice-request-type) get-request))

(defn validate-request-user
  "Verifies that the current user is the person who submitted the request. If the user did not submit the request
   then an exception will be thrown to cause the service endpoint to return a 404. This is useful in cases where
   the user is getting information about a request, and we don't want them to be able to obtain information about
   requests submitted by other users."
  [{username :shortUsername} {requesting-user :requesting_user request-id :id :as request}]
  (when-not (= username requesting-user)
    (cxu/not-found (str "request " request-id " not found")))
  request)

(defn- request-update-fn
  "Returns a function that can be used to update a request with a default update message and a given request
   status code."
  [default-message request-status-code]
  (fn [{username :shortUsername} request-id {:keys [message] :or {message default-message}}]
    (rc/update-request username request-id message request-status-code)))

(def request-in-progress
  "Marks a request as being in progress."
  (request-update-fn "Your request is in progress." "in-progress"))

(def mark-request-rejected
  "Marks a request as having been rejected."
  (request-update-fn "No denial reason given." "rejected"))

(def mark-request-approved
  "Marks a request as having been approved."
  (request-update-fn "Your request has been approved." "approved"))

(defn reject-vice-request
  "Rejects a request for VICE access by changing the user's limit for the number of concurrently running VICE
   analyses to zero."
  [{username :requesting_user} message-body]
  (ac/set-concurrent-job-limit username 0))

(def rejection-fns
  "The functions required to reject different types of requests."
  {vice-request-type reject-vice-request})

(defn- reject-request
  "Performs actions required to reject a request. The specific action taken varies depending on the type of
   request being rejected. In some cases, no action will be taken at all."
  [{request-type :request_type :as request} message-body]
  (when-let [rejection-fn (rejection-fns request-type)]
    (rejection-fn request message-body)))

(defn request-rejected
  "Performs actions required to reject a request and marks the request as rejected."
  [user request-id message-body]
  (let [request (get-request request-id)]
    (reject-request request message-body)
    (mark-request-rejected user request-id message-body)))

(defn fulfill-vice-request
  "Fulfills a request for VICE access by changing the user's limit for the number of cuncurrently running VICE
   analyses to the requested number."
  [{username :requesting_user request-details :details} message-body]
  (let [concurrent-jobs (some :concurrent_jobs [message-body request-details])]
    (ac/set-concurrent-job-limit username concurrent-jobs)))

(def fulfillment-fns
  "The functions required to fulfill different types of requests."
  {vice-request-type fulfill-vice-request})

(defn fulfill-request
  "Performs actions required to fulfill a request. The specific action taken varies depending on the type of
   request being fulfilled."
  [{request-type :request_type :as request} message-body]
  (if-let [fulfillment-fn (fulfillment-fns request-type)]
    (fulfillment-fn request message-body)
    (cxu/internal-system-error (str "request type " request-type " is not supported yet"))))

(defn request-approved
  "Performs actions required to fulfill a request and marks the request as approved."
  [user request-id message-body]
  (let [request (get-request request-id)]
    (fulfill-request request message-body)
    (mark-request-approved user request-id message-body)))
