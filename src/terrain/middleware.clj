(ns terrain.middleware
  (:require [clojure.string :as string]))

(defn- add-context-path
  "Adds a context path to the start of a URI path if it's not present."
  [uri-path context-path]
  (if-not (re-find (re-pattern (str "^\\Q" context-path "\\E(?:/|$)")) uri-path)
    (str context-path uri-path)
    uri-path))

(defn wrap-context-path-adder
  "Middleware that adds a context path to the start of a URI path in a request if it's not present."
  [handler context-path]
  (fn [request]
    (handler (update request :uri add-context-path context-path))))

(defn wrap-query-param-remover
  "Middleware that removes query parameters."
  [handler query-param exceptions]
  (let [exception? (apply some-fn (map #(partial re-find %) exceptions))]
    (fn [request]
      (if (exception? (:uri request))
        (handler request)
        (-> request
            (update-in [:params] dissoc query-param)
            (update-in [:params] dissoc (keyword query-param))
            (update-in [:query-params] dissoc query-param)
            (update-in [:query-params] dissoc (keyword query-param))
            handler)))))
