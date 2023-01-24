(ns terrain.util.nats
  (:require [less.awful.ssl :as ssl]
            [java-time.api :as jt]
            [clojure.java.io :as io]
            [protobuf.core :as protobuf]
            [cheshire.core :as json])
  (:import [io.nats.client Nats Options$Builder]))

(defn options [servers-str crt-fpath key-fpath ca-fpath]
  (let [ssl-ctx     (ssl/ssl-context key-fpath crt-fpath ca-fpath)
        server-list (into-array (clojure.string/split servers-str #"\,"))]
    (-> (new Options$Builder)
        (.servers server-list)
        (.sslContext ssl-ctx)
        (.build))))

(defn connection [opts]
  (Nats/connect opts))

(defn publish-json [conn subject out]
  (let [o (json/generate-string out)]
    (.publish conn subject o)))

(defn- parse-map
  [b]
  (json/parse-string (String. b) true))

(defn request-json
  ([conn subject in timeout]
   (let [msg-bytes (-> (json/generate-string in) (.getBytes))]
     (->> (.request conn subject msg-bytes timeout)
          (.getData)
          (parse-map))))
  ([conn subject in]
   (request-json conn subject in (jt/duration 20 :seconds))))