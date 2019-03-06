(ns terrain.clients.analyses
  (:require [terrain.util.config :refer [analyses-base-uri]]
            [cheshire.core :refer [generate-string]]
            [clojure.tools.logging :as log]
            [terrain.util.transformers :refer [add-current-user-to-map]]
            [clj-http.client :as http]
            [clojure-commons.client :refer [build-url-with-query]]
            [clojure-commons.error-codes :refer [ERR_NOT_FOUND
                                                 ERR_BAD_REQUEST
                                                 ERR_UNCHECKED_EXCEPTION]]
            [cemerick.url :refer [url]]
            [medley.core :refer [mapply]]
            [terrain.auth.user-attributes :refer [current-user]]
            [slingshot.slingshot :refer [throw+]]))


(defn analyses-url
  ([components]
   (analyses-url components {}))
  ([components query]
   (-> (apply url (cons (analyses-base-uri) components))
       (assoc :query (assoc query :user (:username current-user)))
       (str))))

(defn process-response
  [resp err-data]
  (log/warn resp)
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
  (let [u (analyses-url ["badges" id])]
    (log/info u)
    (process-response (http/get u {:as :json}) {:id id})))

(defn delete-badge
  [id]
  (process-response (http/delete (analyses-url ["badges" id]) {:as :json}) {:id id})
  {:id id})

(defn update-badge
  [id submission-info]
  (process-response (http/put (analyses-url ["badges" id])
                              {:content-type  :json
                               :as            :json
                               :body          (generate-string submission-info)})
                    {:id id :submission submission-info}))

(defn add-badge
  [submission-info]
  (process-response (http/post (analyses-url ["badges"])
                               {:content-type :json
                                :as           :json
                                :body         (generate-string submission-info)})
                    {:submission submission-info}))
