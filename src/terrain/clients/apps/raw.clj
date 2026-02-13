(ns terrain.clients.apps.raw
  (:require [cemerick.url :as curl]
            [clj-http.client :as client]
            [clojure-commons.core :refer [remove-nil-values]]
            [terrain.util :refer [disable-redirects]]
            [terrain.util.config :as config]
            [terrain.util.transformers :refer [secured-params]]))

(def apps-sort-params [:limit :offset :sort-field :sort-dir :app-type])
(def base-search-params (conj apps-sort-params :search))
(def apps-hierarchy-sort-params (conj apps-sort-params :attr))
(def tools-search-params (conj base-search-params :include-hidden :public))

(defn- apps-url
  [& components]
  (str (apply curl/url (config/apps-base-url) components)))

(defn- apps-url-encoded
  [& components]
  (str (apply curl/url (config/apps-base-url) (map curl/url-encode components))))

(defn get-all-workflow-elements
  [params]
  (:body
   (client/get (apps-url "apps" "elements")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn get-workflow-elements
  [element-type params]
  (:body
   (client/get (apps-url "apps" "elements" element-type)
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn list-ontologies
  []
  (client/get (apps-url "admin" "ontologies")
              (disable-redirects
               {:query-params (secured-params)
                :as           :stream})))

(defn delete-ontology
  [ontology-version]
  (client/delete (apps-url-encoded "admin" "ontologies" ontology-version)
                 (disable-redirects
                  {:query-params (secured-params)
                   :as           :stream})))

(defn set-ontology-version
  [ontology-version]
  (client/post (apps-url-encoded "admin" "ontologies" ontology-version)
               (disable-redirects
                {:query-params (secured-params)
                 :as           :stream})))

(defn get-app-category-hierarchy
  ([ontology-version root-iri params]
   (client/get (apps-url-encoded "admin" "ontologies" ontology-version root-iri)
               (disable-redirects
                {:query-params (secured-params params [:attr])
                 :as           :stream})))
  ([root-iri params]
   (:body
    (client/get (apps-url-encoded "apps" "hierarchies" root-iri)
                (disable-redirects
                 {:query-params (secured-params params [:attr])
                  :as           :json})))))

(defn get-app-category-hierarchies
  []
  (:body
   (client/get (apps-url "apps" "hierarchies")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-hierarchy-app-listing
  ([ontology-version root-iri params]
   (client/get (apps-url-encoded "admin" "ontologies" ontology-version root-iri "apps")
               (disable-redirects
                {:query-params (secured-params params apps-hierarchy-sort-params)
                 :as           :stream})))
  ([root-iri params]
   (:body
    (client/get (apps-url-encoded "apps" "hierarchies" root-iri "apps")
                (disable-redirects
                 {:query-params (secured-params params apps-hierarchy-sort-params)
                  :as           :json})))))

(defn get-unclassified-app-listing
  ([ontology-version root-iri params]
   (client/get (apps-url-encoded "admin" "ontologies" ontology-version root-iri "unclassified")
               (disable-redirects
                {:query-params (secured-params params apps-hierarchy-sort-params)
                 :as           :stream})))
  ([root-iri params]
   (:body
    (client/get (apps-url-encoded "apps" "hierarchies" root-iri "unclassified")
                (disable-redirects
                 {:query-params (secured-params params apps-hierarchy-sort-params)
                  :as           :json})))))

(defn get-app-categories
  [params]
  (:body
   (client/get (apps-url "apps" "categories")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn apps-in-category
  [system-id category-id params]
  (:body
   (client/get (apps-url "apps" "categories" system-id category-id)
               (disable-redirects
                {:query-params (secured-params params)
                 :as            :json}))))

(defn featured-apps
  [params]
  (:body
   (client/get (apps-url "apps" "categories" "featured")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn apps-in-community
  [community-id]
  (:body
   (client/get (apps-url "apps" "communities" community-id "apps")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn admin-get-apps-in-community
  [community-id  & {:keys [as] :or {as :stream}}]
  (client/get (apps-url "admin" "apps" "communities" community-id "apps")
              (disable-redirects
               {:query-params (secured-params)
                :as           as})))

(defn search-apps
  [params]
  (:body
   (client/get (apps-url "apps")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn create-app
  [system-id app]
  (:body
   (client/post (apps-url "apps" system-id)
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  app
                  :content-type :json
                  :as           :json}))))

(defn create-app-version
  [system-id app-id app]
  (:body
   (client/post (apps-url "apps" system-id app-id "versions")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  app
                  :content-type :json
                  :as           :json}))))

(defn set-app-versions-order
  [system-id app-id update-request]
  (:body
    (client/put (apps-url "apps" system-id app-id "versions")
                (disable-redirects
                  {:query-params (secured-params)
                   :form-params  update-request
                   :content-type :json
                   :as           :json}))))

(defn preview-args
  [system-id app]
  (:body
   (client/post (apps-url "apps" system-id "arg-preview")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  app
                  :content-type :json
                  :as           :json}))))

(defn delete-apps
  [deletion-request]
  (:body
   (client/post (apps-url "apps" "shredder")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  deletion-request
                  :content-type :json
                  :as           :json}))))

(defn list-permissions
  [body params]
  (:body
   (client/post (apps-url "apps" "permission-lister")
                (disable-redirects
                 {:query-params (secured-params params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn share
  [body]
  (:body
   (client/post (apps-url "apps" "sharing")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn unshare
  [body]
  (:body
   (client/post (apps-url "apps" "unsharing")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn admin-share
  [body]
  (:body
   (client/post (apps-url "admin" "apps" "sharing")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn admin-unshare
  [body]
  (:body
   (client/post (apps-url "admin" "apps" "unsharing")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn get-app
  [system-id app-id]
  (:body
   (client/get (apps-url "apps" system-id app-id)
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-app-version
  [system-id app-id version-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "versions" version-id)
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn delete-app
  [system-id app-id]
  (:body
   (client/delete (apps-url "apps" system-id app-id)
                  (disable-redirects
                   {:query-params (secured-params)
                    :as           :json}))))

(defn delete-app-version
  [system-id app-id version-id]
  (:body
   (client/delete (apps-url "apps" system-id app-id "versions" version-id)
                  (disable-redirects
                   {:query-params (secured-params)
                    :as           :json}))))

(defn relabel-app
  [system-id app-id relabel-request]
  (:body
   (client/patch (apps-url "apps" system-id app-id)
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  relabel-request
                   :content-type :json
                   :as           :json}))))

(defn relabel-app-version
  [system-id app-id version-id relabel-request]
  (:body
   (client/patch (apps-url "apps" system-id app-id "versions" version-id)
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  relabel-request
                   :content-type :json
                   :as           :json}))))

(defn update-app
  [system-id app-id update-request]
  (:body
   (client/put (apps-url "apps" system-id app-id)
               (disable-redirects
                {:query-params (secured-params)
                 :form-params  update-request
                 :content-type :json
                 :as           :json}))))

(defn update-app-version
  [system-id app-id version-id update-request]
  (:body
   (client/put (apps-url "apps" system-id app-id "versions" version-id)
               (disable-redirects
                {:query-params (secured-params)
                 :form-params  update-request
                 :content-type :json
                 :as           :json}))))

(defn admin-remove-app-blessing
  [system-id app-id]
  (client/delete (apps-url "admin" "apps" system-id app-id "blessing")
                 (disable-redirects {:query-params (secured-params)})))

(defn admin-bless-app
  [system-id app-id]
  (client/post (apps-url "admin" "apps" system-id app-id "blessing")
               (disable-redirects {:query-params (secured-params)})))

(defn copy-app
  [system-id app-id]
  (:body
   (client/post (apps-url "apps" system-id app-id "copy")
                (disable-redirects
                 {:query-params (secured-params)
                  :as           :json}))))

(defn copy-app-version
  [system-id app-id version-id]
  (:body
   (client/post (apps-url "apps" system-id app-id "versions" version-id "copy")
                (disable-redirects
                 {:query-params (secured-params)
                  :as           :json}))))

(defn list-single-app
  [system-id app-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "listing")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-admin-app-details
  [system-id app-id]
  (:body
   (client/get (apps-url "admin" "apps" system-id app-id "details")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-admin-app-version-details
  [system-id app-id version-id]
  (:body
   (client/get (apps-url "admin" "apps" system-id app-id "versions" version-id "details")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-app-details
  [system-id app-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "details")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-app-version-details
  [system-id app-id version-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "versions" version-id "details")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn remove-favorite-app
  [system-id app-id]
  (:body
   (client/delete (apps-url "apps" system-id app-id "favorite")
                  (disable-redirects
                   {:query-params (secured-params)
                    :as           :json}))))

(defn add-favorite-app
  [system-id app-id]
  (:body
   (client/put (apps-url "apps" system-id app-id "favorite")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn app-publishable?
  [system-id app-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "is-publishable")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn remove-app-from-communities
  [app-id body]
  (:body
   (client/delete (apps-url "apps" app-id "communities")
                  (disable-redirects
                   {:query-params (secured-params)
                    :form-params  body
                    :content-type :json
                    :as           :json}))))

(defn update-app-communities
  [app-id body]
  (:body
   (client/post (apps-url "apps" app-id "communities")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn admin-list-avus
  [app-id]
  (client/get (apps-url "admin" "apps" app-id "metadata")
              (disable-redirects
               {:query-params (secured-params)
                :as           :stream})))

(defn admin-set-avus
  [app-id body]
  (client/put (apps-url "admin" "apps" app-id "metadata")
              (disable-redirects
               {:query-params (secured-params)
                :body         body
                :content-type :json
                :as           :stream})))

(defn admin-update-avus
  [app-id body]
  (client/post (apps-url "admin" "apps" app-id "metadata")
               (disable-redirects
                {:query-params (secured-params)
                 :body         body
                 :content-type :json
                 :as           :stream})))

(defn list-avus
  [app-id]
  (:body
   (client/get (apps-url "apps" app-id "metadata")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn set-avus
  [app-id body]
  (:body
   (client/put (apps-url "apps" app-id "metadata")
               (disable-redirects
                {:query-params (secured-params)
                 :form-params  body
                 :content-type :json
                 :as           :json}))))

(defn update-avus
  [app-id body]
  (:body
   (client/post (apps-url "apps" app-id "metadata")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn make-app-public
  [system-id app-id app]
  (:body
   (client/post (apps-url "apps" system-id app-id "publish")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  app
                  :content-type :json
                  :as           :json}))))

(defn admin-publish-app
  [system-id app-id body]
  (:body
   (client/post (apps-url "admin" "apps" system-id app-id "publish")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn delete-rating
  [system-id app-id]
  (:body
   (client/delete (apps-url "apps" system-id app-id "rating")
                  (disable-redirects
                   {:query-params (secured-params)
                    :as           :json}))))

(defn rate-app
  [system-id app-id rating]
  (:body
   (client/post (apps-url "apps" system-id app-id "rating")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  rating
                  :content-type :json
                  :as           :json}))))

(defn list-app-tasks
  [system-id app-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "tasks")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn list-app-version-tasks
  [system-id app-id version-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "versions" version-id "tasks")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-app-ui
  [system-id app-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "ui")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-app-version-ui
  [system-id app-id version-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "versions" version-id "ui")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn add-pipeline
  [pipeline]
  (:body
   (client/post (apps-url "apps" "pipelines")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  pipeline
                  :content-type :json
                  :as           :json}))))

(defn add-pipeline-version
  [app-id pipeline]
  (:body
   (client/post (apps-url "apps" "pipelines" app-id "versions")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  pipeline
                  :content-type :json
                  :as           :json}))))

(defn update-pipeline
  [app-id pipeline]
  (:body
   (client/put (apps-url "apps" "pipelines" app-id)
               (disable-redirects
                {:query-params (secured-params)
                 :form-params  pipeline
                 :content-type :json
                 :as           :json}))))

(defn update-pipeline-version
  [app-id version-id pipeline]
  (:body
   (client/put (apps-url "apps" "pipelines" app-id "versions" version-id)
               (disable-redirects
                {:query-params (secured-params)
                 :form-params  pipeline
                 :content-type :json
                 :as           :json}))))

(defn copy-pipeline
  [app-id]
  (:body
   (client/post (apps-url "apps" "pipelines" app-id "copy")
                (disable-redirects
                 {:query-params (secured-params)
                  :as           :json}))))

(defn copy-pipeline-version
  [app-id version-id]
  (:body
   (client/post (apps-url "apps" "pipelines" app-id "versions" version-id "copy")
                (disable-redirects
                 {:query-params (secured-params)
                  :as           :json}))))

(defn edit-pipeline
  [app-id]
  (:body
   (client/get (apps-url "apps" "pipelines" app-id "ui")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn edit-pipeline-version
  [app-id version-id]
  (:body
   (client/get (apps-url "apps" "pipelines" app-id "versions" version-id "ui")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn list-jobs
  [params]
  (:body
   (client/get (apps-url "analyses")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn list-job-stats
  [params]
  (:body
   (client/get (apps-url "analyses" "stats")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn list-job-permissions
  [body params]
  (:body
   (client/post (apps-url "analyses" "permission-lister")
                (disable-redirects
                 {:query-params (secured-params params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn relaunch-jobs
  [body]
  (:body
   (client/post (apps-url "analyses" "relauncher")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn share-jobs
  [body]
  (:body
   (client/post (apps-url "analyses" "sharing")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn unshare-jobs
  [body]
  (:body
   (client/post (apps-url "analyses" "unsharing")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn submit-job
  [body]
  (:body
   (client/post (apps-url "analyses")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn update-job
  [analysis-id body]
  (:body
   (client/patch (apps-url "analyses" analysis-id)
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  body
                   :content-type :json
                   :as           :json}))))

(defn delete-job
  [analysis-id]
  (:body
   (client/delete (apps-url "analyses" analysis-id)
                  (disable-redirects
                   {:query-params (secured-params)
                    :as           :json}))))

(defn delete-jobs
  [body]
  (:body
   (client/post (apps-url "analyses" "shredder")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn get-job-params
  [analysis-id]
  (:body
   (client/get (apps-url "analyses" analysis-id "parameters")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-job-relaunch-info
  [analysis-id]
  (:body
   (client/get (apps-url "analyses" analysis-id "relaunch-info")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn list-job-steps
  [analysis-id]
  (:body
   (client/get (apps-url "analyses" analysis-id "steps")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn stop-job
  [analysis-id params]
  (:body
   (client/post (apps-url "analyses" analysis-id "stop")
                (disable-redirects
                 {:query-params (secured-params params)
                  :as           :json}))))

(defn get-job-history
  [analysis-id]
  (:body
   (client/get (apps-url "analyses" analysis-id "history")
               {:query-params      (secured-params)
                :as                :json
                :follow-redirecrts false})))

(defn admin-get-apps
  [params]
  (:body
   (client/get (apps-url "admin" "apps")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn categorize-apps
  [body]
  (:body
   (client/post (apps-url "admin" "apps")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn list-app-publication-requests
  [params]
  (:body
   (client/get (apps-url "admin" "apps" "publication-requests")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn permanently-delete-apps
  [body]
  (:body
   (client/post (apps-url "admin" "apps" "shredder")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn admin-delete-app
  [system-id app-id]
  (:body
   (client/delete (apps-url "admin" "apps" system-id app-id)
                  (disable-redirects
                   {:query-params (secured-params)
                    :as           :json}))))

(defn admin-update-app
  [system-id app-id body]
  (:body
   (client/patch (apps-url "admin" "apps" system-id app-id)
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  body
                   :content-type :json
                   :as           :json}))))

(defn admin-update-app-version
  [system-id app-id version-id body]
  (:body
   (client/patch (apps-url "admin" "apps" system-id app-id "versions" version-id)
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  body
                   :content-type :json
                   :as           :json}))))

(defn get-admin-app-categories
  [params]
  (client/get (apps-url "admin" "apps" "categories")
              (disable-redirects
               {:query-params (secured-params params apps-sort-params)
                :as           :stream})))

(defn search-admin-app-categories
  [params]
  (client/get (apps-url "admin" "apps" "categories" "search")
              (disable-redirects
               {:query-params (secured-params params [:name])
                :as           :stream})))

(defn add-category
  [system-id body]
  (client/post (apps-url "admin" "apps" "categories" system-id)
               (disable-redirects
                {:query-params (secured-params)
                 :content-type :json
                 :body         body
                 :as           :stream})))

(defn delete-category
  [system-id category-id]
  (client/delete (apps-url "admin" "apps" "categories" system-id category-id)
                 (disable-redirects
                  {:query-params (secured-params)
                   :as           :stream})))

(defn update-category
  [system-id category-id body]
  (client/patch (apps-url "admin" "apps" "categories" system-id category-id)
                (disable-redirects
                 {:query-params (secured-params)
                  :content-type :json
                  :body         body
                  :as           :stream})))

(defn get-app-docs
  [system-id app-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "documentation")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-app-version-docs
  [system-id app-id version-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "versions" version-id "documentation")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn edit-app-docs
  [system-id app-id docs]
  (:body
   (client/patch (apps-url "apps" system-id app-id "documentation")
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  docs
                   :content-type :json
                   :as           :json}))))

(defn edit-app-version-docs
  [system-id app-id version-id docs]
  (:body
   (client/patch (apps-url "apps" system-id app-id "versions" version-id "documentation")
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  docs
                   :content-type :json
                   :as           :json}))))

(defn add-app-docs
  [system-id app-id docs]
  (:body
   (client/post (apps-url "apps" system-id app-id "documentation")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  docs
                  :content-type :json
                  :as           :json}))))

(defn add-app-version-docs
  [system-id app-id version-id docs]
  (:body
   (client/post (apps-url "apps" system-id app-id "versions" version-id "documentation")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  docs
                  :content-type :json
                  :as           :json}))))

(defn admin-edit-app-docs
  [system-id app-id docs]
  (:body
   (client/patch (apps-url "admin" "apps" system-id app-id "documentation")
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  docs
                   :content-type :json
                   :as           :json}))))

(defn admin-edit-app-version-docs
  [system-id app-id version-id docs]
  (:body
   (client/patch (apps-url "admin" "apps" system-id app-id "versions" version-id "documentation")
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  docs
                   :content-type :json
                   :as           :json}))))

(defn admin-add-app-docs
  [system-id app-id docs]
  (:body
   (client/post (apps-url "admin" "apps" system-id app-id "documentation")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  docs
                  :content-type :json
                  :as           :json}))))

(defn admin-add-app-version-docs
  [system-id app-id version-id docs]
  (:body
   (client/post (apps-url "admin" "apps" system-id app-id "versions" version-id "documentation")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  docs
                  :content-type :json
                  :as           :json}))))

(defn get-oauth-access-token
  [api-name params]
  (:body
   (client/get (apps-url "oauth" "access-code" api-name)
               (disable-redirects
                {:query-params (secured-params params [:code :state])
                 :as           :json}))))

(defn get-oauth-token-info
  [api-name]
  (:body
   (client/get (apps-url "oauth" "token-info" api-name)
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn delete-oauth-token-info
  [api-name]
  (:body
   (client/delete (apps-url "oauth" "token-info" api-name)
                  (disable-redirects
                   {:query-params (secured-params)
                    :as           :json}))))

(defn get-oauth-redirect-uris
  []
  (:body
   (client/get (apps-url "oauth" "redirect-uris")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-admin-oauth-token-info
  [api-name params]
  (:body
   (client/get (apps-url "admin" "oauth" "token-info" api-name)
               (disable-redirects
                {:query-params (secured-params params [:proxy-user])
                 :as           :json}))))

(defn delete-admin-oauth-token-info
  [api-name params]
  (:body
   (client/delete (apps-url "admin" "oauth" "token-info" api-name)
                  (disable-redirects
                   {:query-params (secured-params params [:proxy-user])
                    :as           :json}))))

(defn admin-delete-tool-request-status-code
  [status-code-id]
  (:body
   (client/delete (apps-url "admin" "tool-requests" "status-codes" status-code-id)
                  (disable-redirects
                   {:query-params (secured-params)
                    :as           :json}))))

(defn admin-list-tool-requests
  [params]
  (:body
   (client/get (apps-url "admin" "tool-requests")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn admin-delete-tool-request
  [request-id]
  (:body
   (client/delete (apps-url "admin" "tool-requests" request-id)
                  (disable-redirects
                   {:query-params (secured-params)
                    :as           :json}))))

(defn admin-get-tool-request
  [request-id]
  (:body
   (client/get (apps-url "admin" "tool-requests" request-id)
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn admin-update-tool-request
  [body request-id]
  (:body
   (client/post (apps-url "admin" "tool-requests" request-id "status")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn list-tool-requests
  "Lists the tool requests that were submitted by the authenticated user."
  [params]
  (:body
   (client/get (apps-url "tool-requests")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn submit-tool-request
  "Submits a tool request on behalf of the user found in the request params."
  [body]
  (:body
   (client/post (apps-url "tool-requests")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn list-tool-request-status-codes
  [params]
  (:body
   (client/get (apps-url "tool-requests" "status-codes")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn get-tools-in-app
  [system-id app-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "tools")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-tools-in-app-version
  [system-id app-id version-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "versions" version-id "tools")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn admin-list-tools
  [params]
  (:body
   (client/get (apps-url "admin" "tools")
               (disable-redirects
                {:query-params (secured-params params tools-search-params)
                 :as           :json}))))

(defn admin-add-tools
  [body]
  (:body
   (client/post (apps-url "admin" "tools")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :as           :json
                  :content-type :json}))))

(defn admin-delete-tool
  [tool-id]
  (:body
   (client/delete (apps-url "admin" "tools" tool-id)
                  (disable-redirects
                   {:query-params (secured-params)
                    :as           :json}))))

(defn admin-get-tool
  [tool-id params]
  (:body
   (client/get (apps-url "admin" "tools" tool-id)
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn admin-update-tool
  [tool-id params tool]
  (:body
   (client/patch (apps-url "admin" "tools" tool-id)
                 (disable-redirects
                  {:query-params (secured-params params [:overwrite-public])
                   :form-params  tool
                   :as           :json
                   :content-type :json}))))

(defn admin-get-apps-by-tool
  [tool-id]
  (:body
   (client/get (apps-url "admin" "tools" tool-id "apps")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn admin-publish-tool
  [tool-id body]
  (:body
   (client/post (apps-url "admin" "tools" tool-id "publish")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn list-tools
  [params]
  (:body
   (client/get (apps-url "tools")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn create-private-tool
  [body]
  (:body
   (client/post (apps-url "tools")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn list-tool-permissions
  [body params]
  (:body
   (client/post (apps-url "tools" "permission-lister")
                (disable-redirects
                 {:query-params (secured-params params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn share-tool
  [body]
  (:body
   (client/post (apps-url "tools" "sharing")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn unshare-tool
  [body]
  (:body
   (client/post (apps-url "tools" "unsharing")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :content-type :json
                  :as           :json}))))

(defn delete-private-tool
  [tool-id params]
  (:body
   (client/delete (apps-url "tools" tool-id)
                  (disable-redirects
                   {:query-params (secured-params params)
                    :as           :json}))))

(defn get-tool
  [tool-id params]
  (:body
   (client/get (apps-url "tools" tool-id)
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn update-private-tool
  [tool-id body]
  (:body
   (client/patch (apps-url "tools" tool-id)
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  body
                   :as           :json
                   :content-type :json}))))

(defn get-apps-by-tool
  [tool-id]
  (:body
   (client/get (apps-url "tools" tool-id "apps")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn list-reference-genomes
  [params]
  (:body
   (client/get (apps-url "reference-genomes")
               (disable-redirects
                {:query-params (secured-params params)
                 :as           :json}))))

(defn get-reference-genome
  [reference-genome-id]
  (:body
   (client/get (apps-url "reference-genomes" reference-genome-id)
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn admin-add-reference-genome
  [body]
  (:body
   (client/post (apps-url "admin" "reference-genomes")
                (disable-redirects
                 {:query-params (secured-params)
                  :form-params  body
                  :as           :json
                  :content-type :json}))))

(defn admin-delete-reference-genome
  [reference-genome-id params]
  (:body
   (client/delete (apps-url "admin" "reference-genomes" reference-genome-id)
                  (disable-redirects
                   {:query-params (secured-params params)
                    :as           :json}))))

(defn admin-update-reference-genome
  [body reference-genome-id]
  (:body
   (client/patch (apps-url "admin" "reference-genomes" reference-genome-id)
                 (disable-redirects
                  {:query-params (secured-params)
                   :form-params  body
                   :as           :json
                   :content-type :json}))))

(defn record-login
  [ip-address session-id login-time]
  (let [params (remove-nil-values {:ip-address ip-address :session-id session-id :login-time login-time})]
    (:body
     (client/post (apps-url "users" "login")
                  (disable-redirects
                   {:query-params (secured-params params)
                    :as           :json})))))

(defn list-logins
  [limit]
  (:body
    (client/get (apps-url "users" "logins")
                 (disable-redirects
                   {:query-params (secured-params (remove-nil-values {:limit limit}))
                    :as           :json}))))

(defn list-integration-data
  [params]
  (client/get (apps-url "admin" "integration-data")
              (disable-redirects
               {:query-params (secured-params params base-search-params)
                :as           :stream})))

(defn add-integration-data
  [body]
  (client/post (apps-url "admin" "integration-data")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :stream
                 :body         body
                 :content-type :json})))

(defn get-integration-data
  [integration-data-id]
  (client/get (apps-url "admin" "integration-data" integration-data-id)
              (disable-redirects
               {:query-params (secured-params)
                :as           :stream})))

(defn update-integration-data
  [integration-data-id body]
  (client/put (apps-url "admin" "integration-data" integration-data-id)
              (disable-redirects
               {:query-params (secured-params)
                :as           :stream
                :body         body
                :content-type :json})))

(defn delete-integration-data
  [integration-data-id]
  (client/delete (apps-url "admin" "integration-data" integration-data-id)
                 (disable-redirects
                  {:query-params (secured-params)
                   :as           :stream})))

(defn get-app-integration-data
  [system-id app-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "integration-data")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-app-version-integration-data
  [system-id app-id version-id]
  (:body
   (client/get (apps-url "apps" system-id app-id "versions" version-id "integration-data")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-tool-integration-data
  [tool-id]
  (:body
   (client/get (apps-url "tools" tool-id "integration-data")
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn update-app-integration-data
  [system-id app-id integration-data-id]
  (:body
   (client/put (apps-url "admin" "apps" system-id app-id "integration-data" integration-data-id)
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn update-app-version-integration-data
  [system-id app-id version-id integration-data-id]
  (:body
   (client/put (apps-url "admin" "apps" system-id app-id "versions" version-id "integration-data" integration-data-id)
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn update-tool-integration-data
  [tool-id integration-data-id]
  (:body
   (client/put (apps-url "admin" "tools" tool-id "integration-data" integration-data-id)
               (disable-redirects
                {:query-params (secured-params)
                 :as           :json}))))

(defn get-workshop-group
  []
  (client/get (apps-url "admin" "groups" "workshop")
              (disable-redirects
               {:query-params (secured-params)
                :as           :stream})))

(defn get-workshop-group-members
  []
  (client/get (apps-url "admin" "groups" "workshop" "members")
              (disable-redirects
               {:query-params (secured-params)
                :as           :stream})))

(defn update-workshop-group-members
  [body]
  (client/put (apps-url "admin" "groups" "workshop" "members")
              (disable-redirects
               {:query-params (secured-params)
                :as           :stream
                :body         body
                :content-type :json})))

(def workspace-for-user
  (memoize (fn [params]
             (-> (apps-url "workspaces")
                 (client/get (disable-redirects {:query-params params
                                                 :as           :json}))
                 :body))))

(defn bootstrap
  []
  (-> (apps-url "bootstrap")
      (client/get (disable-redirects {:query-params (secured-params)
                                      :as           :json}))
      :body))

(defn save-webhooks
  [webhooks]
  (:body (client/put (apps-url "webhooks")
                     (disable-redirects
                      {:query-params (secured-params)
                       :as           :json
                       :form-params  webhooks
                       :content-type :json}))))

(defn admin-list-workspaces
  [params]
  (client/get (apps-url "admin" "workspaces")
              (disable-redirects
               {:query-params (secured-params params [:username])
                :as           :stream})))

(defn admin-delete-workspaces
  [params]
  (client/delete (apps-url "admin" "workspaces")
                 (disable-redirects
                  {:query-params (secured-params params [:username])
                   :as           :stream})))

(defn get-webhook-types
  []
  (:body (client/get (apps-url "webhooks" "types")
                     (disable-redirects
                      {:query-params (secured-params)
                       :as           :json}))))

(defn get-webhook-topics
  []
  (:body (client/get (apps-url "webhooks" "topics")
                     (disable-redirects
                      {:query-params (secured-params)
                       :as           :json}))))

(defn get-webhooks
  []
  (:body (client/get (apps-url "webhooks")
                     (disable-redirects
                      {:query-params (secured-params)
                       :as           :json}))))

(defn update-tapis-job-status
  [job-id body]
  (:body (client/post (apps-url "callbacks" "tapis-job" job-id)
                      (disable-redirects
                       {:form-params  body
                        :content-type :json
                        :as           :json}))))
