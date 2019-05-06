(ns terrain.clients.search
  (:use [terrain.util.config :only [search-base-url]]
        [terrain.util.transformers :only [secured-params]])
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]))

(defn do-data-search
  [body]
  (let [req-options  {:form-params  body
                      :query-params (secured-params)
                      :as           :json
                      :content-type :json}]
    (:body (http/post (str (url (search-base-url) "data" "search")) req-options))))

(defn get-data-search-documentation
  []
  (:body (http/get (str (url (search-base-url) "data" "documentation"))
                   {:as           :json
                    :content-type :json})))
