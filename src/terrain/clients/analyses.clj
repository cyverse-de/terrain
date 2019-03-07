(ns terrain.clients.analyses
  (:require [terrain.util.config :refer [analyses-base-uri]]
            [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [terrain.auth.user-attributes :refer [current-user]]))


(defn analyses-url
  ([components]
   (analyses-url components {}))
  ([components query]
   (-> (apply url (cons (analyses-base-uri) components))
       (assoc :query (assoc query :user (:username current-user)))
       (str))))

(defn get-badge
  [id]
  (:body (http/get (analyses-url ["badges" id]) {:as :json})))

(defn delete-badge
  [id]
  (http/delete (analyses-url ["badges" id]) {:as :json}) {:id id}
  {:id id})

(defn update-badge
  [id submission-info]
  (:body (http/put (analyses-url ["badges" id])
                   {:content-type  :json
                    :as            :json
                    :form-params   submission-info})
         {:id id :submission submission-info}))

(defn add-badge
  [submission-info]
  (:body (http/post (analyses-url ["badges"])
                    {:content-type :json
                     :as           :json
                     :form-params  submission-info})
         {:submission submission-info}))
