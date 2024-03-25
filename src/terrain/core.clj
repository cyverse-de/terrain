(ns terrain.core
  (:gen-class)
  (:use [clojure.java.io :only [file resource]]
        [terrain.middleware :only [wrap-fake-user]])
  (:require [terrain.util.config :as config]
            [terrain.util.nats :as nats]
            [clojure.tools.nrepl.server :as nrepl]
            [me.raynes.fs :as fs]
            [clj-http.client :as http]
            [common-cli.core :as ccli]
            [terrain.services.filesystem.icat :as icat]
            [clojure.tools.logging :as log]
            [service-logging.thread-context :as tc]))

(defn- start-nrepl
  []
  (nrepl/start-server :port 7888))

(defn- iplant-conf-dir-file
  [filename]
  (when-let [conf-dir (System/getenv "IPLANT_CONF_DIR")]
    (let [f (file conf-dir filename)]
      (when (.isFile f) (.getPath f)))))

(defn- cwd-file
  [filename]
  (let [f (file filename)]
    (when (.isFile f) (.getPath f))))

(defn- classpath-file
  [filename]
  (some-> (resource filename) file))

(defn- no-configuration-found
  [filename]
  (throw (RuntimeException. (str "configuration file " filename " not found"))))

(defn- find-configuration-file
  []
  ((some-fn iplant-conf-dir-file cwd-file classpath-file no-configuration-found) "terrain.properties"))

(defn load-configuration-from-file
  "Loads the configuration properties from a file."
  ([]
   (load-configuration-from-file (find-configuration-file)))
  ([path]
   (config/load-config-from-file path)))

(defn nats-connect
  []
  (nats/set-options
   (config/nats-urls)
   (config/nats-tls-enabled)
   (config/nats-tls-crt)
   (config/nats-tls-key)
   (config/nats-tls-ca)
   (config/nats-max-reconnects)
   (config/nats-reconnect-wait))
  (nats/set-connection))

(defn lein-ring-init
  "This function is used by leiningen ring plugin to initialize terrain."
  []
  (load-configuration-from-file)
  (icat/configure-icat)
  (nats-connect)
  (start-nrepl))

(defn repl-init
  "This function is used to manually initialize terrain from the leiningen REPL."
  []
  (load-configuration-from-file)
  (icat/configure-icat))

(defn cli-options
  []
  (concat
   (ccli/cli-options)
   [["-u" "--fake-user USERNAME" "The username to use for fake authentication"]]))

(def svc-info
  {:desc "DE service for business logic"
   :app-name "terrain"
   :group-id "org.cyverse"
   :art-id "terrain"
   :service "terrain"})

;; terrain.routes MUST be required and eval'd here or the configuration won't yet be loaded
(defn dev-handler
  [req]
  (tc/with-logging-context svc-info
    (require 'terrain.routes)
    ((wrap-fake-user (eval 'terrain.routes/app-wrapper) (System/getenv "CYVERSE_USERNAME")) req)))

;; terrain.routes MUST be required and eval'd here or the configuration won't yet be loaded
(defn run-jetty
  [port fake-user]
  (require 'terrain.routes 'ring.adapter.jetty)
  (log/warn "Started listening on" port)
  ((eval 'ring.adapter.jetty/run-jetty)
   (wrap-fake-user (eval 'terrain.routes/app-wrapper) fake-user)
   {:port port}))

(defn- load-config
  [config-path]
  (let [config-path (or config-path "/etc/iplant/de/terrain.properties")]
    (when-not (fs/exists? config-path)
      (ccli/exit 1 (str "The config file does not exist.")))
    (when-not (fs/readable? config-path)
      (ccli/exit 1 "The config file is not readable."))
    (config/load-config-from-file config-path)))

(defn- get-port
  [{:keys [port]}]
  (or port (config/listen-port)))

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options]} (ccli/handle-args svc-info args cli-options)]
      (load-config (:config options))
      (nats-connect)
      (http/with-connection-pool {:timeout 5 :threads 10 :insecure? false :default-per-route 10}
        (icat/configure-icat)
        (run-jetty (get-port options) (:fake-user options))))))
