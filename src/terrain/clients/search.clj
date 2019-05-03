(ns terrain.clients.search
  (:use [terrain.util.config :only [search-base-url]])
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [cheshire.core :as json]))

(defn do-data-search
  [params body]
  (let [req-options  {:body         (json/encode body)
                      :query-params params
                      :as           :stream
                      :content-type "application/json"}]
    (http/post (str (url (search-base-url) "data" "search")) req-options)))

(defn get-data-search-documentation
  []
  (:body (http/get (str (url (search-base-url) "data" "documentation"))
                   {:as           :json
                    :content-type :json})))
