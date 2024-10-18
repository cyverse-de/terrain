(ns terrain.clients.app-exposer
  (:require [cemerick.url :as curl]
            [clj-http.client :as client]
            [terrain.util.config :as config]
            [terrain.auth.user-attributes :refer [current-user]]))

(defn- augment-query
  [query {:keys [no-user]}]
  (as-> query q
    (if no-user q (assoc q :user (:shortUsername current-user)))))

(defn- app-exposer-url
  ([components]
   (app-exposer-url components {}))
  ([components query & {:as opts}]
   (-> (apply curl/url (config/app-exposer-base-uri) components)
       (assoc :query (augment-query query opts))
       str)))

(defn get-pod-logs
  "Returns the logs for a pod"
  [analysis-id query]
  (:body (client/get (app-exposer-url ["vice" analysis-id "logs"] query) {:as :json})))

(defn get-time-limit
  "Returns the time limit for a VICE analysis"
  [analysis-id]
  (:body (client/get (app-exposer-url ["vice" analysis-id "time-limit"]) {:as :json})))

(defn admin-get-time-limit
  "Same as (get-time-limit), just with no user info and a different path"
  [analysis-id]
  (:body (client/get (app-exposer-url ["vice" "admin" "analyses" analysis-id "time-limit"] {} :no-user true) {:as :json})))

(defn set-time-limit
  "Calls the endpoint that adds two days to the time limit for a VICE analysis"
  [analysis-id]
  (:body (client/post (app-exposer-url ["vice" analysis-id "time-limit"]) {:as :json})))

(defn admin-set-time-limit
  "Same as (set-time-limit), just with no user info and a different path"
  [analysis-id]
  (:body (client/post (app-exposer-url ["vice" "admin" "analyses" analysis-id "time-limit"] {} :no-user true) {:as :json})))

(defn get-resources
  "Calls app-exposer's GET /vice/listing endpoint, with filter as the query filter map.
  Returns only results applicable to the user."
  [filter]
  (:body (client/get (app-exposer-url ["vice" "listing"] filter) {:as :json})))

(defn admin-get-resources
  "Calls app-exposer's GET /vice/admin/listing endpoint, with the filter as the
   query parameter map. Bypasses the user filter present in the non-admin version."
  [filter]
  (-> (app-exposer-url ["vice" "admin" "listing"] filter :no-user true)
      (client/get  {:as :json})
      (:body)))

(defn url-ready
  "Calls app-exposer's GET /vice/:host/url-ready endpoint, which returns a map
   containing a boolean that tells whether the analysis is ready for browser access.
   Checks if the logged in user has permission to access the analysis before returning
   a result."
  [host]
  (-> (app-exposer-url ["vice" host "url-ready"] {})
      (client/get  {:as :json})
      (:body)))

(defn admin-url-ready
  "Same as url-ready, but calls the admin version that bypasses the permissions check
   present in the non-admin version of the call."
  [host]
  (-> (app-exposer-url ["vice" "admin" host "url-ready"] {} :no-user true)
      (client/get {:as :json})
      (:body)))

(defn get-description-by-host
  "Returns a JSON description of the resources created for the analyis with the supplied
   host/subdomain."
  [host]
  (-> (app-exposer-url ["vice" host "description"] {})
      (client/get {:as :json})
      (:body)))

(defn admin-get-description-by-host
  "Returns a JSON description of the resources created for the analysis with the supplied
   host/subdomain. Bypasses the permissions check present in the non-admin version of this call."
  [host]
  (-> (app-exposer-url ["vice" "admin" host "description"] {} :no-user true)
      (client/get {:as :json})
      (:body)))

(defn cancel-analysis
  "Calls app-exposer's POST /vice/{id}/save-and-exit endpoint"
  [external-id]
  (:body (client/post (app-exposer-url ["vice", external-id, "save-and-exit"]) {:as :json})))

(defn admin-cancel-analysis
  "Same as (cancel-analysis), just with no user info and a different path and accepts the analysis-id"
  [analysis-id]
  (:body (client/post (app-exposer-url ["vice" "admin" "analyses" analysis-id "save-and-exit"] {} :no-user true) {:as :json})))

(defn readiness
  "Calls app-exposer's GET /vice/{subdomain}/url-ready endpoint"
  [subdomain]
  (:body (client/get (app-exposer-url ["vice", subdomain, "url-ready"]) {:as :json})))

(defn async-data
  "Calls app-exposer's GET /vice/async-data endpoint"
  [query]
  (:body (client/get (app-exposer-url ["vice" "async-data"] query) {:as :json})))

(defn admin-external-id
  "Gets the external-id for the provided analysis-id. VICE analyses only have a single external-id"
  [analysis-id]
  (:body (client/get (app-exposer-url ["vice" "admin" "analyses" analysis-id "external-id"] {} :no-user true) {:as :json})))

(defn admin-exit
  "Forces the analysis to exit without transferring output files"
  [analysis-id]
  (:body (client/post (app-exposer-url ["vice" "admin" "analyses" analysis-id "exit"] {} :no-user true) {:as :json})))

(defn admin-save-output-files
  "Tells the vice-transfer-files tool to transfer outputs without terminating the analysis"
  [analysis-id]
  (:body (client/post (app-exposer-url ["vice" "admin" "analyses" analysis-id "save-output-files"] {} :no-user true) {:as :json})))

(defn admin-download-input-files
  "Tells the vice-transfer-files tool to download inputs without affecting the analysis status"
  [analysis-id]
  (:body (client/post (app-exposer-url ["vice" "admin" "analyses" analysis-id "download-input-files"] {} :no-user true) {:as :json})))

(defn latest-instant-launch-mappings-defaults
  []
  (-> (app-exposer-url ["instantlaunches" "mappings" "defaults" "latest"] {} :no-user true)
      (client/get {:as :json})
      (:body)))

(defn add-latest-instant-launch-mappings-defaults
  [username latest]
  (-> (app-exposer-url ["instantlaunches" "mappings" "defaults" "latest"] {:username username} :no-user true)
      (client/put {:as           :json
                   :content-type :json
                   :form-params  latest})
      (:body)))

(defn update-latest-instant-launch-mappings-defaults
  [username latest]
  (-> (app-exposer-url ["instantlaunches" "mappings" "defaults" "latest"] {:username username} :no-user true)
      (client/post {:as           :json
                    :content-type :json
                    :form-params  latest})
      (:body)))

(defn delete-latest-instant-launch-mappings-defaults
  []
  (-> (app-exposer-url ["instantlaunches" "mappings" "defaults" "latest"] {} :no-user true)
      (client/delete {:as :json})
      (:body)))

(defn get-instant-launch-list
  []
  {:instant_launches
   (-> (app-exposer-url ["instantlaunches"] {} :no-user true)
       (client/get {:as :json})
       (:body))})

(defn get-full-instant-launch-list
  []
  {:instant_launches
   (-> (app-exposer-url ["instantlaunches" "full"] {} :no-user true)
       (client/get {:as :json})
       (:body))})

(defn get-instant-launch
  [id]
  (-> (app-exposer-url ["instantlaunches" id] {} :no-user true)
      (client/get {:as :json})
      (:body)))

(defn get-full-instant-launch
  [id]
  (-> (app-exposer-url ["instantlaunches" id "full"] {} :no-user true)
      (client/get {:as :json})
      (:body)))

(defn add-instant-launch
  [username il]
  (-> (app-exposer-url ["instantlaunches/"] {})
      (client/put {:content-type :json
                   :as           :json
                   :form-params  (update il :added_by #(or %1 username))})
      (:body)))

(defn update-instant-launch
  [id il]
  (-> (app-exposer-url ["instantlaunches" id] {})
      (client/post {:content-type :json
                    :as           :json
                    :form-params  il})
      (:body)))

(defn delete-instant-launch
  [id]
  (-> (app-exposer-url ["instantlaunches" id] {})
      (client/delete {:as :json})
      (:body)))

(defn admin-add-instant-launch
  [username il]
  (-> (app-exposer-url ["instantlaunches" "admin/"] {} :no-user true)
      (client/put {:content-type :json
                   :as           :json
                   :form-params  (update il :added_by #(or %1 username))})
      (:body)))

(defn admin-update-instant-launch
  [id il]
  (-> (app-exposer-url ["instantlaunches" "admin" id] {} :no-user true)
      (client/post {:content-type :json
                    :as           :json
                    :form-params  il})
      (:body)))

(defn admin-delete-instant-launch
  [id]
  (-> (app-exposer-url ["instantlaunches" "admin" id] {} :no-user true)
      (client/delete {:as :json})
      (:body)))

;;; For the (list-metadata) function, we bypass the query map handling built
;;; into (app-exposer-url) to use the one from clj-http because the former
;;; does not handle having an seq of values for an entry while the latter does.
;;; This isn't necessary for other calls, so the change was not made in the
;;; (app-exposer-url) function.
(defn list-metadata
  [query]
  (-> (app-exposer-url ["instantlaunches" "metadata"] {} :no-user true)
      (client/get {:as           :json
                   :query-params (assoc query :user (:shortUsername current-user))})
      (:body)))

(defn list-full-metadata
  [query username]
  {:instant_launches (-> (app-exposer-url ["instantlaunches" "metadata" "full"] {} :no-user true)
                         (client/get {:as           :json
                                      :query-params (assoc query :user username)})
                         (:body))})

(defn get-metadata
  [id]
  (-> (app-exposer-url ["instantlaunches" id "metadata"] {})
      (client/get {:as :json})
      (:body)))

(defn upsert-metadata
  [id data]
  (-> (app-exposer-url ["instantlaunches" "admin" id "metadata"] {})
      (client/post {:content-type :json
                    :as :json
                    :form-params data})
      (:body)))

(defn reset-metadata
  [id data]
  (-> (app-exposer-url ["instantlaunches" "admin" id "metadata"] {})
      (client/put {:content-type :json
                   :as :json
                   :form-params data})
      (:body)))

(defn list-quicklaunches-for-public-apps
  []
  (-> (app-exposer-url ["instantlaunches" "quicklaunches" "public"] {})
      (client/get {:as :json})
      (:body)))
