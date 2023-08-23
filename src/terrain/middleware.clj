(ns terrain.middleware
  (:require [clojure.string :as string]
            [terrain.auth.user-attributes :as user-attributes]
            [clojure-commons.response :as resp]
            [terrain.clients.apps.raw :as apps]
            [terrain.clients.data-usage-api :as dua]
            [terrain.util.transformers :refer [secured-params]]))

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
            (update-in [:params] dissoc query-param (keyword query-param))
            (update-in [:query-params] dissoc query-param (keyword query-param))
            handler)))))

(defn wrap-fake-user
  "Middleware that configures fake authentication for development testing. If the fake user is falsey,
   the handler function is not wrapped at all."
  [handler fake-user]
  (if fake-user
    (fn [request]
      (binding [user-attributes/fake-user fake-user]
        (handler request)))
    handler))

(defn check-user-data-overages
  [handler]
  (fn [req]
    (if (dua/user-data-overage? (:user (:user-info req)))
      (resp/forbidden "The account has data overages")
      (handler req))))

(defn wrap-create-workspace
  [handler]
  (fn [request]
    (when (:user-info request)
      (apps/workspace-for-user (secured-params)))
    (handler request)))
