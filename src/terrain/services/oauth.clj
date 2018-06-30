(ns terrain.services.oauth
  (:require [clojure.data.codec.base64 :as base64]
            [clojure.string :as string]
            [ring.util.http-response :as http-response]
            [terrain.clients.oauth :as client]
            [terrain.util.service :as service]))

(defn- get-basic-auth-credentials [authorization]
  (when-let [header-fields (some-> authorization (string/split #" "))]
    (when (= (first header-fields) "Basic")
      (some-> (second header-fields)
              (.getBytes)
              base64/decode
              (String.)
              (string/split #":" 2)))))

(defn get-token [authorization]
  (if-let [[username password] (get-basic-auth-credentials authorization)]
    (service/success-response (client/get-token username password))
    (http-response/unauthorized)))
