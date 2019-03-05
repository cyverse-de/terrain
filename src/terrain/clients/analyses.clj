(ns terrain.clients.analyses
  (:require [terrain.util.config :refer [analyses-base-uri]]
            [terrain.util.transformers :refer [add-current-user-to-map]]
            [clj-http.client :as http]
            [clojure-commons.client :refer [build-url-with-query]]
            [clojure-commons.error-codes :refer [ERR_NOT_FOUND
                                                 ERR_BAD_REQUEST
                                                 ERR_UNCHECKED_EXCEPTION]]
            [slingshot.slingshot :refer [throw+]]))


(defn analyses-url
  ([relative-url]
   (analyses-url relative-url {}))
  ([relative-url query]
   (build-url-with-query (analyses-base-uri)
                         (add-current-user-to-map query) relative-url)))

(defn process-response
  [resp err-data]
  (cond
    (= (:status resp) 404)
    (throw+ (merge {:error_code ERR_NOT_FOUND} err-data {:msg (:body resp)}))

    (= (:status resp) 400)
    (throw+ (merge {:error_code ERR_BAD_REQUEST} err-data {:msg (:body resp)}))

    (= (:status resp) 500)
    (throw+ (merge {:error_code ERR_UNCHECKED_EXCEPTION} err-data {:msg (:body resp)}))

    (not (<= 200 (:status resp) 299))
    (throw+ (merge {:error_code ERR_UNCHECKED_EXCEPTION} err-data {:msg (:body resp)}))

    :else
    (:body resp)))

(defn get-badge
  [id]
  (process-response (http/get (analyses-url "/badges" id) {:as :json}) {:id id}))

(defn delete-badge
  [id]
  (process-response (http/delete (analyses-url "/badges" id) {:as :json}) {:id id}))

(defn update-badge
  [id submission-info]
  (process-response (http/put (analyses-url "/badges" id)
                              {:content-type  :json
                               :as            :json
                               :body          submission-info})
                    {:id id :submission submission-info}))

(defn add-badge
  [submission-info]
  (process-response (http/post (analyses-url "/badges")
                               {:content-type :json
                                :as           :json
                                :body         submission-info})
                    {:submission submission-info}))
