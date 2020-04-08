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

(defn get-quicklaunch-app-info
  [id]
  (:body (http/get (analyses-url ["quicklaunches" id "app-info"]) {:as :json})))

(defn get-all-quicklaunches
  []
  (:body (http/get (analyses-url ["quicklaunches"]) {:as :json})))

(defn get-quicklaunches-by-app
  [id]
  (:body (http/get (analyses-url ["quicklaunches" "apps" id]) {:as :json})))

(defn delete-quicklaunch
  [id]
  (:body (http/delete (analyses-url ["quicklaunches" id]) {:as :json})))

(defn update-quicklaunch
  [id ql-info]
  (:body (http/patch (analyses-url ["quicklaunches" id])
                     {:content-type  :json
                      :as            :json
                      :form-params   ql-info})))

(defn add-quicklaunch
  [ql-info]
  (let [ql-info (assoc ql-info :creator (:username current-user))]
    (:body (http/post (analyses-url ["quicklaunches"])
                      {:content-type :json
                       :as           :json
                       :form-params  ql-info}))))

(defn get-quicklaunch-favorite
  [id]
  (:body (http/get (analyses-url ["quicklaunch" "favorites" id]) {:as :json})))

(defn get-all-quicklaunch-favorites
  []
  (:body (http/get (analyses-url ["quicklaunch" "favorites"]) {:as :json})))

(defn delete-quicklaunch-favorite
  [id]
  (:body (http/delete (analyses-url ["quicklaunch" "favorites" id]) {:as :json})))

(defn add-quicklaunch-favorite
  [ql]
  (:body (http/post (analyses-url ["quicklaunch" "favorites"])
                    {:content-type :json
                     :as           :json
                     :form-params  ql})))

(defn get-quicklaunch-user-default
  [id]
  (:body (http/get (analyses-url ["quicklaunch" "defaults" "user" id]) {:as :json})))

(defn get-all-quicklaunch-user-defaults
  []
  (:body (http/get (analyses-url ["quicklaunch" "defaults" "user"]) {:as :json})))

(defn delete-quicklaunch-user-default
  [id]
  (:body (http/delete (analyses-url ["quicklaunch" "defaults" "user" id]) {:as :json})))

(defn update-quicklaunch-user-default
  [id ql]
  (:body (http/patch (analyses-url ["quicklaunch" "defaults" "user" id])
                     {:content-type :json
                      :as           :json
                      :form-params   ql})))

(defn add-quicklaunch-user-default
  [ql]
  (:body (http/post (analyses-url ["quicklaunch" "defaults" "user"])
                    {:content-type :json
                     :as           :json
                     :form-params  ql})))

(defn get-quicklaunch-global-default
  [id]
  (:body (http/get (analyses-url ["quicklaunch" "defaults" "global" id]) {:as :json})))

(defn get-all-quicklaunch-global-defaults
  []
  (:body (http/get (analyses-url ["quicklaunch" "defaults" "global"]) {:as :json})))

(defn delete-quicklaunch-global-default
  [id]
  (:body (http/delete (analyses-url ["quicklaunch" "defaults" "global" id]) {:as :json})))

(defn update-quicklaunch-global-default
  [id ql]
  (:body (http/patch (analyses-url ["quicklaunch" "defaults" "global" id])
                     {:content-type :json
                      :as           :json
                      :form-params  ql})))

(defn add-quicklaunch-global-default
  [ql]
  (:body (http/post (analyses-url ["quicklaunch" "defaults" "global"])
                    {:content-type :json
                     :as           :json
                     :form-params  ql})))

(defn list-concurrent-job-limits
  []
  (:body (http/get (analyses-url ["settings" "concurrent-job-limits"])
                   {:as :json})))

(defn get-concurrent-job-limit
  [username]
  (:body (http/get (analyses-url ["settings" "concurrent-job-limits" username])
                   {:as :json})))

(defn set-concurrent-job-limit
  [username concurrent-jobs]
  (:body (http/put (analyses-url ["settings" "concurrent-job-limits" username])
                   {:content-type :json
                    :as           :json
                    :form-params  {:concurrent_jobs concurrent-jobs}})))

(defn remove-concurrent-job-limit
  [username]
  (:body (http/delete (analyses-url ["settings" "concurrent-job-limits" username])
                      {:as :json})))
