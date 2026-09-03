(ns terrain.services.filesystem.stat
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [slingshot.slingshot :refer [throw+]]
            [terrain.clients.data-info.raw :as data-raw]
            [terrain.util.config :as cfg]))

(defn get-public-data-user
  "Returns the anonymous user for public data if a user is not provided"
  ([user paths ids]
   (let [paths (if (sequential? paths) paths [paths])
         has-ids-or-private-paths? (or (seq ids) (some #(not (string/starts-with? % (cfg/fs-community-data))) paths))
         request-user (if has-ids-or-private-paths?
                        user
                        (or user "anonymous"))]
     (or request-user (throw+ {:type :clojure-commons.exception/not-authorized
                               :user user}))))
  ([user paths]
   (get-public-data-user user paths nil)))

(defn do-stat
  [{user :user} body]
  (let [paths         (:paths body)
        ids           (:ids body)
        request-user  (get-public-data-user user paths ids)]
    (-> (data-raw/collect-stats request-user body)
        :body
        (json/decode true))))
