(ns terrain.util.nats
  (:require [less.awful.ssl :as ssl]
            [java-time.api :as jt]
            [clojure.java.io :as io]
            [protobuf.core :as protobuf]
            [cheshire.core :as json])
  (:import [io.nats.client Nats Options$Builder]))

(defn- get-options [servers-str crt-fpath key-fpath ca-fpath]
  (let [ssl-ctx     (ssl/ssl-context key-fpath crt-fpath ca-fpath)
        server-list (into-array (clojure.string/split servers-str #"\,"))]
    (-> (new Options$Builder)
        (.servers server-list)
        (.sslContext ssl-ctx)
        (.build))))

(def nats-conn (atom nil))
(def nats-options (atom nil))

(defn set-connection
  []
  (if (nil? @nats-conn)
    (reset! nats-conn (Nats/connect @nats-options))
    @nats-conn))

(defn set-options [servers crt key ca]
  (if (nil? @nats-options)
    (reset! nats-options (get-options servers crt key ca))
    @nats-options))

(defn publish-json [subject out]
  (let [o (json/generate-string out)]
    (.publish @nats-conn subject o)))

(defn- parse-map
  [b]
  (json/parse-string (String. b) true))

(defn request-json
  ([subject out timeout]
   (let [msg-bytes (-> (json/generate-string out) (.getBytes))]
     (-> (.request @nats-conn subject msg-bytes timeout)
         (.getData)
         (parse-map))))
  ([subject out]
   (request-json subject out (jt/duration 20 :seconds))))