(ns terrain.clients.search
  (:use [terrain.util.config :only [search-base-url]]
        [terrain.util.transformers :only [add-current-user-to-map]])
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]))

(defn do-data-search
  [body]
  (let [query-params {:user (or (:user (add-current-user-to-map {})) "anonymous")}
        req-options  {:form-params  body
                      :query-params query-params
                      :as           :json
                      :content-type :json}]
    (:body (http/post (str (url (search-base-url) "data" "search")) req-options))))

(defn get-data-search-documentation
  []
  (:body (http/get (str (url (search-base-url) "data" "documentation"))
                   {:as           :json
                    :content-type :json})))
