(ns terrain.util.validators
  (:require [clojure.set :as set]
            [clojure-commons.error-codes :as ce]
            [cheshire.core :as json]
            [slingshot.slingshot :refer [try+ throw+]]
            [terrain.util.config :as cfg])
  (:import [java.util UUID]))

(defn parse-body
  [body]
  (try+
   (json/parse-string body true)
   (catch Exception e
     (throw+ {:error_code ce/ERR_INVALID_JSON
              :message    (str e)}))))

(defn extract-uri-uuid
  "Converts a UUID from text taken from a URI. If the text isn't a UUID, it throws an exception.

   Parameters:
     uuid-txt - The URI text containing a UUID.

   Returns:
     It returns the UUID.

   Throws:
     It throws an ce/ERR_NOT_FOUND if the text isn't a UUID."
  [uuid-txt]
  (if (instance? UUID uuid-txt)
    uuid-txt
    (try+
     (UUID/fromString uuid-txt)
     (catch IllegalArgumentException _ (throw+ {:error_code ce/ERR_NOT_FOUND})))))


(defn good-string?
  "Checks that a string doesn't contain any problematic characters.

   Params:
     to-check - The string to check

   Returns:
     It returns false if the string contains at least one problematic character, otherwise false."
  ^Boolean [^String to-check]
  (let [bad-chars      (set (seq (cfg/fs-bad-chars)))
        chars-to-check (set (seq to-check))]
    (empty? (set/intersection bad-chars chars-to-check))))
