(ns terrain.util.nats
  (:require [less.awful.ssl :as ssl]
            [java-time.api :as jt]
            [cheshire.core :as json]
            [clojure.string :as string]
            [pronto.core :as p]
            [clojure.tools.logging :as log])
  (:import [io.nats.client Nats Options$Builder] 
           [org.cyverse.de.protobufs
            AddAddonRequest
            NoParamsRequest
            UpdateAddonRequest
            ByUUID
            AssociateByUUIDs
            UpdateSubscriptionAddonRequest]))

(defn- get-options [servers-str tls? crt-fpath key-fpath ca-fpath max-reconns reconn-wait]
  (let [ssl-ctx     (when tls? (ssl/ssl-context key-fpath crt-fpath ca-fpath))
        server-list (into-array (string/split servers-str #"\,"))]
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
  (-> key name (string/replace "-" "_")))

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

(p/defmapper default-mapper [AddAddonRequest
                             NoParamsRequest
                             UpdateAddonRequest
                             ByUUID
                             AssociateByUUIDs
                             UpdateSubscriptionAddonRequest])

(defn create
  ([mapper cl m]
   (p/clj-map->proto-map mapper cl m))
  ([cl m]
   (create default-mapper cl m)))

(defn request
  ([subject cl m timeout]
   (request-json subject (create cl m) timeout))
  ([subject cl m]
   (request-json subject (create cl m))))
