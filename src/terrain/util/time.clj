(ns terrain.util.time
  (:require [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [java-time.api :as jt]
            [clojure.string :as string])
  (:import [com.google.protobuf Timestamp]
           [java.time Instant]))

(defn format-timestamp
  "Formats a timestamp in a standard format."
  [timestamp]
  (if-not (or (string/blank? timestamp) (= "0" timestamp))
    (tf/unparse (:date-time tf/formatters) (tc/from-long (Long/parseLong timestamp)))
    ""))

(defn protobuf-timestamp
  [timestamp-str]
  (let [instant (jt/instant timestamp-str)
        builder (Timestamp/newBuilder)]
    (-> builder
        (.setSeconds (.getEpochSecond instant))
        (.setNanos (.getNano instant))
        (.build))))

(defn timestamp->map
  [timestamp-str]
  (let [ts (protobuf-timestamp timestamp-str)]
    {:seconds (.getSeconds ts)
     :nanos   (.getNanos ts)}))

(defn map->timestamp
  [{:keys [seconds nanos]}]
  (.toString (Instant/ofEpochSecond seconds nanos)))
