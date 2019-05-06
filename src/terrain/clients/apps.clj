(ns terrain.clients.apps
  (:require [cheshire.core :as cheshire]
            [terrain.clients.apps.raw :as raw]
            [terrain.util.service :as service]))

(defn admin-list-tool-requests
  [params]
  (->> (raw/admin-list-tool-requests params)
       (:body)
       (service/decode-json)))

(defn list-tool-request-status-codes
  [params]
  (-> (raw/list-tool-request-status-codes params)
      (:body)
      (service/decode-json)))

(defn admin-add-tools
  [body]
  (raw/admin-add-tools (cheshire/encode body)))

(defn get-authenticated-user
  []
  (-> (raw/get-authenticated-user)
      (:body)
      (service/decode-json)))

(defn record-login
  [ip-address user-agent]
  (-> (raw/record-login ip-address user-agent)
      (:body)
      (service/decode-json)))

(defn record-logout
  [ip-address login-time]
  (raw/record-logout ip-address login-time)
  nil)

(defn bootstrap
  []
  (-> (raw/bootstrap)
      :body
      service/decode-json))
