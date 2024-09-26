(ns terrain.util.service
  (:require [cheshire.core :as cheshire]
            [clojure.java.io :refer [reader]]))

(defn- error-body [e]
  (cheshire/encode {:reason (.getMessage e)}))

(defn- success?
  "Returns true if status-code is between 200 and 299, inclusive."
  [status-code]
  (<= 200 status-code 299))

(defn- response-map?
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
  [e _status-code]
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

(defn- terrain-response
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

(defn decode-json
  "Decodes JSON from either a string or an input stream."
  [source]
  (if (string? source)
    (cheshire/decode source true)
    (cheshire/decode-stream (reader source) true)))
