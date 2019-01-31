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
