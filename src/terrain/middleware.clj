(ns terrain.middleware
  (:require [clojure.string :as string]))

(defn- remove-context-path
  "Removes a context path from the start of a URI path if it's present. The context path can either be followed by
   a slash or by the end of the URL path."
  [uri-path context-path]
  (string/replace uri-path
                  (re-pattern (str "^\\Q" context-path "\\E(?:/|$)"))
                  "/"))

(defn wrap-context-path-remover
  "Middleware that removes a context path from the start of the URI path in the request if it's present."
  [handler context-path]
  (fn [request]
    (handler (update request :uri remove-context-path context-path))))
