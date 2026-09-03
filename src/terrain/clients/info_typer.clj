(ns terrain.clients.info-typer
  (:require [cemerick.url :as url]
            [clj-http.client :as http]
            [terrain.util.config :as cfg]))

;; The connection pool in terrain.core sets an idle-connection TTL, not a request deadline, so
;; these keep a stalled info-typer from pinning a request thread indefinitely.
(def ^:private request-timeouts
  {:socket-timeout 10000
   :conn-timeout   10000})

(defn- info-typer-url
  [& url-path]
  (str (apply url/url (cfg/info-typer-base-url) url-path)))

(defn get-type-list
  "Lists the file types the DE can identify."
  []
  (:body (http/get (info-typer-url "file-types") (assoc request-timeouts :as :json))))

(defn set-file-type
  "Sets a file's type, or unsets it when the type is an empty string."
  [user path-uuid type]
  (:body (http/put (info-typer-url "data" path-uuid "type")
                   (assoc request-timeouts
                          :form-params  {:type type}
                          :query-params {:user user}
                          :content-type :json
                          :as           :json))))
