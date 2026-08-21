(ns terrain.clients.info-typer
  (:require [cemerick.url :as url]
            [clj-http.client :as http]
            [terrain.util.config :as cfg]))

;; File type detection moved out of data-info and into info-typer, which owns it. The paths
;; are the ones data-info served, so this is the same two calls against a different base URL --
;; which is what keeps the move reversible by configuration while both services still answer.

(defn- info-typer-url
  [& url-path]
  (str (apply url/url (cfg/info-typer-base-url) url-path)))

(defn get-type-list
  "Lists the file types the DE can identify."
  []
  (:body (http/get (info-typer-url "file-types") {:as :json})))

(defn set-file-type
  "Sets a file's type, or unsets it when the type is an empty string."
  [user path-uuid type]
  (:body (http/put (info-typer-url "data" path-uuid "type")
                   {:form-params  {:type type}
                    :query-params {:user user}
                    :content-type :json
                    :as           :json})))
