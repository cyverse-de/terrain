(ns terrain.services.requests
  (:require [clojure-commons.exception-util :as cxu]
            [terrain.clients.requests :as rc]))

;; Request type constants
(def vice-request-type "vice")

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
  (rc/submit-request vice-request-type username (add-user-to-request-details user details)))

(defn- get-request
  "Gets information about a request and verifies that the request type is correct. If the request exists but is of
   a different type then an exception will be thrown to cause the service endpoint to return a 404."
  [request-type request-id]
  (let [request (rc/get-request request-id)]
    (when-not (= request-type (:request_type request))
      (cxu/not-found (str request-type " request " request-id " not found")))
    request))

(def get-vice-request
  "Gets information about a VICE request. If the request exists but is not a VICE request then an exception will be
   thrown to cause the service endpoint to return a 404."
  (partial get-request vice-request-type))

(defn validate-request-user
  "Verifies that the current user is the person who submitted the request. If the user did not submit the request
   then an exception will be thrown to cause the service endpoint to return a 404. This is useful in cases where
   the user is getting information about a request, and we don't want them to be able to obtain information about
   requests submitted by other users."
  [{username :shortUsername} {requesting-user :requesting_user request-id :id :as request}]
  (when-not (= username requesting-user)
    (cxu/not-found (str "request " request-id " not found")))
  request)
