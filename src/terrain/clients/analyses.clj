(ns terrain.clients.analyses
  (:require [terrain.util.config :refer [analyses-base-uri]]
            [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [terrain.auth.user-attributes :refer [current-user]]))


(defn analyses-url
  ([components]
   (analyses-url components {}))
  ([components query]
   (-> (apply url (analyses-base-uri) components)
       (assoc :query (assoc query :user (:username current-user)))
       (str))))

(defn get-quicklaunch
  [id]
  (:body (http/get (analyses-url ["quicklaunches" id]) {:as :json})))

(defn delete-quicklaunch
  [id]
  (:body (http/delete (analyses-url ["quicklaunches" id]) {:as :json})))

(defn update-quicklaunch
  [id submission-info]
  (:body (http/patch (analyses-url ["quicklaunches" id])
                     {:content-type  :json
                      :as            :json
                      :form-params   submission-info})))

(defn add-quicklaunch
  [submission-info]
  (:body (http/post (analyses-url ["quicklaunches"])
                    {:content-type :json
                     :as           :json
                     :form-params  submission-info})))
