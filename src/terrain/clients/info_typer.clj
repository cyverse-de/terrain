(ns terrain.clients.info-typer
  (:require [cemerick.url :as url]
            [clj-http.client :as http]
            [terrain.util.config :as cfg]))

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
