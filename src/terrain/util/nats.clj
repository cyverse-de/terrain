(ns terrain.util.nats
  (:require [less.awful.ssl :as ssl]
            [java-time.api :as jt]
            [clojure.java.io :as io]
            [protobuf.core :as protobuf]
            [cheshire.core :as json]
            [clojure-commons.error-codes :as error])
  (:import [io.nats.client Nats Options$Builder]))

(defn- get-options [servers-str tls? crt-fpath key-fpath ca-fpath max-reconns reconn-wait]
  (let [ssl-ctx     (if tls? (ssl/ssl-context key-fpath crt-fpath ca-fpath))
        server-list (into-array (clojure.string/split servers-str #"\,"))]
    (-> (new Options$Builder)
        (.servers server-list)
        ((fn [b]
           (if tls?
             (.sslContext b ssl-ctx)
             b)))
        (.maxReconnects max-reconns)
        (.reconnectWait (jt/duration reconn-wait :seconds))
        (.build))))

(def nats-conn (atom nil))
(def nats-options (atom nil))

(defn set-connection
  []
  (if (nil? @nats-conn)
    (reset! nats-conn (Nats/connect @nats-options))
    @nats-conn))

(defn set-options [servers tls? crt key ca max-reconns reconn-wait]
  (if (nil? @nats-options)
    (reset! nats-options (get-options servers tls? crt key ca max-reconns reconn-wait))
    @nats-options))

(defn- encode-key
  [key]
  (-> key name (clojure.string/replace "-" "_")))

(defn- json-encode
  [o]
  (json/generate-string o {:key-fn encode-key}))

(defn- json-decode-bytes
  [b]
  (json/parse-string (String. b) true))

(defn publish-json [subject out]
  (let [o (json-encode out)]
    (.publish @nats-conn subject o)))

(defn request-json
  ([subject out timeout]
   (let [msg-bytes (-> (json-encode out) (.getBytes))]
     (-> (.request @nats-conn subject msg-bytes timeout)
         (.getData)
         (json-decode-bytes))))
  ([subject out]
   (request-json subject out (jt/duration 20 :seconds))))
