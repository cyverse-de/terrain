(ns terrain.util.service
  (:use [ring.util.codec :only [url-encode]]
        [clojure.java.io :only [reader]]
        [clojure.string :only [join blank?] :as string]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [ring.util.codec :as codec]))


(defn error-body [e]
  (cheshire/encode {:reason (.getMessage e)}))

(defn success?
  "Returns true if status-code is between 200 and 299, inclusive."
  [status-code]
  (<= 200 status-code 299))

(defn response-map?
  "Returns true if 'm' can be used as a response map. We're defining a
   response map as a map that contains a :status and :body field."
  [m]
  (and (map? m)
       (contains? m :status)
       (contains? m :body)))

(def ^:private default-content-type
  "application/json; charset=utf-8")

(defn- content-type-specified?
  [e]
  (or (contains? e :content-type)
      (contains? (:headers e) "Content-Type")))

(defn- terrain-response-from-response-map
  [e status-code]
  (let [e (select-keys e [:headers :status :body])]
    (if-not (content-type-specified? e)
      (update-in e [:headers] assoc "Content-Type" default-content-type)
      e)))

(defn- terrain-response-from-map
  [e status-code]
  {:status  status-code
   :body    (cheshire/encode e)
   :headers {"Content-Type" default-content-type}})

(defn- error-resp?
  [e status-code]
  (and (instance? Exception e)
       (not (success? status-code))))

(defn- terrain-response-from-exception
  [e status-code]
  {:status  status-code
   :body    (error-body e)
   :headers {"Content-Type" default-content-type}})

(defn- default-terrain-response
  [e status-code]
  {:status status-code
   :body   e})

(defn terrain-response
  "Generates a Terrain HTTP response map based on a value and a status code.

   If a response map is passed in, it is preserved.

   If a response map is passed in and is missing the content-type field,
   then the content-type is set to application/json.

   If it's a map but not a response map then it's JSON encoded and used as the body of the response.

   Otherwise, the value is preserved and is wrapped in a response map."
  [e status-code]
  (cond
   (response-map? e)           (terrain-response-from-response-map e status-code)
   (map? e)                    (terrain-response-from-map e status-code)
   (error-resp? e status-code) (terrain-response-from-exception e status-code)
   :else                       (default-terrain-response e status-code)))

(defn success-response
  ([]
     (success-response nil))
  ([retval]
    (terrain-response retval 200)))

(defn unrecognized-path-response
  "Builds the response to send for an unrecognized service path."
  []
  (let [msg "unrecognized service path"]
    (cheshire/encode {:reason msg})))

(defn prepare-forwarded-request
  "Prepares a request to be forwarded to a remote service."
  ([request body]
    {:content-type (or (get-in request [:headers :content-type])
                       (get-in request [:content-type]))
      :headers (dissoc
                (:headers request)
                "content-length"
                "content-type"
                "transfer-encoding")
      :body body
      :throw-exceptions false
      :as :stream})
  ([request]
     (prepare-forwarded-request request nil)))

(defn forward-post
  "Forwards a POST request to a remote service."
  ([addr request]
     (forward-post addr request (slurp (:body request))))
  ([addr request body]
     (client/post addr (prepare-forwarded-request request body))))

(defn decode-stream
  "Decodes a stream containing a JSON object."
  [stream]
  (cheshire/decode-stream (reader stream) true))

(defn decode-json
  "Decodes JSON from either a string or an input stream."
  [source]
  (if (string? source)
    (cheshire/decode source true)
    (cheshire/decode-stream (reader source) true)))
