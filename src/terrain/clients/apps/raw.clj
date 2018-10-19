(ns terrain.clients.apps.raw
  (:use [terrain.util.transformers :only [secured-params]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as client]
            [terrain.util.config :as config]))

(def apps-sort-params [:limit :offset :sort-field :sort-dir :app-type])
(def base-search-params (conj apps-sort-params :search))
(def apps-analysis-listing-params (conj apps-sort-params :include-hidden :filter))
(def apps-search-params (conj base-search-params :start_date :end_date))
(def admin-apps-search-params (conj apps-search-params :app-subset))
(def apps-hierarchy-sort-params (conj apps-sort-params :attr))
(def tools-search-params (conj base-search-params :include-hidden :public))
(def permission-lister-params [:full-listing])

(defn- apps-url
  [& components]
  (str (apply curl/url (config/apps-base-url) components)))

(defn- apps-url-encoded
  [& components]
  (str (apply curl/url (config/apps-base-url) (map curl/url-encode components))))

(defn get-all-workflow-elements
  [params]
  (client/get (apps-url "apps" "elements")
              {:query-params     (secured-params params [:include-hidden])
               :as               :stream
               :follow-redirects false}))

(defn get-workflow-elements
  [element-type params]
  (client/get (apps-url "apps" "elements" element-type)
              {:query-params     (secured-params params [:include-hidden])
               :as               :stream
               :follow-redirects false}))

(defn list-ontologies
  []
  (client/get (apps-url "admin" "ontologies")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn delete-ontology
  [ontology-version]
  (client/delete (apps-url-encoded "admin" "ontologies" ontology-version)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn set-ontology-version
  [ontology-version]
  (client/post (apps-url-encoded "admin" "ontologies" ontology-version)
               {:query-params     (secured-params)
                :as               :stream
                :follow-redirects false}))

(defn get-app-category-hierarchy
  ([ontology-version root-iri params]
   (client/get (apps-url-encoded "admin" "ontologies" ontology-version root-iri)
               {:query-params     (secured-params params [:attr])
                :as               :stream
                :follow-redirects false}))
  ([root-iri params]
   (client/get (apps-url-encoded "apps" "hierarchies" root-iri)
               {:query-params     (secured-params params [:attr])
                :as               :stream
                :follow-redirects false})))

(defn get-app-category-hierarchies
  []
  (client/get (apps-url "apps" "hierarchies")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-hierarchy-app-listing
  ([ontology-version root-iri params]
   (client/get (apps-url-encoded "admin" "ontologies" ontology-version root-iri "apps")
               {:query-params     (secured-params params apps-hierarchy-sort-params)
                :as               :stream
                :follow-redirects false}))
  ([root-iri params]
   (client/get (apps-url-encoded "apps" "hierarchies" root-iri "apps")
               {:query-params     (secured-params params apps-hierarchy-sort-params)
                :as               :stream
                :follow-redirects false})))

(defn get-unclassified-app-listing
  ([ontology-version root-iri params]
   (client/get (apps-url-encoded "admin" "ontologies" ontology-version root-iri "unclassified")
               {:query-params     (secured-params params apps-hierarchy-sort-params)
                :as               :stream
                :follow-redirects false}))
  ([root-iri params]
   (client/get (apps-url-encoded "apps" "hierarchies" root-iri "unclassified")
               {:query-params     (secured-params params apps-hierarchy-sort-params)
                :as               :stream
                :follow-redirects false})))

(defn get-app-categories
  [params]
  (client/get (apps-url "apps" "categories")
              {:query-params     (secured-params params [:public])
               :as               :stream
               :follow-redirects false}))

(defn apps-in-category
  [system-id category-id params]
  (client/get (apps-url "apps" "categories" system-id category-id)
              {:query-params     (secured-params params apps-sort-params)
               :as               :stream
               :follow-redirects false}))

(defn apps-in-community
  [community-id]
  (client/get (apps-url "apps" "communities" community-id "apps")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn admin-get-apps-in-community
  [community-id]
  (client/get (apps-url "admin" "apps" "communities" community-id "apps")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn search-apps
  [params]
  (client/get (apps-url "apps")
              {:query-params     (secured-params params apps-search-params)
               :as               :stream
               :follow-redirects false}))

(defn create-app
  [system-id app]
  (client/post (apps-url "apps" system-id)
               {:query-params     (secured-params)
                :body             app
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn preview-args
  [system-id app]
  (client/post (apps-url "apps" system-id "arg-preview")
               {:query-params     (secured-params)
                :body             app
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn delete-apps
  [deletion-request]
  (client/post (apps-url "apps" "shredder")
               {:query-params     (secured-params)
                :body             deletion-request
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn list-permissions
  [body params]
  (client/post (apps-url "apps" "permission-lister")
               {:query-params     (secured-params params permission-lister-params)
                :body             body
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn share
  [body]
  (client/post (apps-url "apps" "sharing")
               {:query-params     (secured-params)
                :body             body
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn unshare
  [body]
  (client/post (apps-url "apps" "unsharing")
               {:query-params     (secured-params)
                :body             body
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn get-app
  [system-id app-id]
  (client/get (apps-url "apps" system-id app-id)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn delete-app
  [system-id app-id]
  (client/delete (apps-url "apps" system-id app-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn relabel-app
  [system-id app-id relabel-request]
  (client/patch (apps-url "apps" system-id app-id)
                {:query-params     (secured-params)
                 :body             relabel-request
                 :content-type     :json
                 :as               :stream
                 :follow-redirects false}))

(defn update-app
  [system-id app-id update-request]
  (client/put (apps-url "apps" system-id app-id)
              {:query-params     (secured-params)
               :body             update-request
               :content-type     :json
               :as               :stream
               :follow-redirects false}))

(defn copy-app
  [system-id app-id]
  (client/post (apps-url "apps" system-id app-id "copy")
               {:query-params     (secured-params)
                :as               :stream
                :follow-redirects false}))

(defn get-admin-app-details
  [system-id app-id]
  (client/get (apps-url "admin" "apps" system-id app-id "details")
              {:query-params      (secured-params)
               :as                :stream
               :follow-redirects  false}))

(defn get-app-details
  [system-id app-id]
  (client/get (apps-url "apps" system-id app-id "details")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn remove-favorite-app
  [system-id app-id]
  (client/delete (apps-url "apps" system-id app-id "favorite")
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn add-favorite-app
  [system-id app-id]
  (client/put (apps-url "apps" system-id app-id "favorite")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn app-publishable?
  [system-id app-id]
  (client/get (apps-url "apps" system-id app-id "is-publishable")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn admin-list-avus
  [app-id]
  (client/get (apps-url "admin" "apps" app-id "metadata")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn admin-set-avus
  [app-id body]
  (client/put (apps-url "admin" "apps" app-id "metadata")
              {:query-params     (secured-params)
               :body             body
               :content-type     :json
               :as               :stream
               :follow-redirects false}))

(defn admin-update-avus
  [app-id body]
  (client/post (apps-url "admin" "apps" app-id "metadata")
               {:query-params     (secured-params)
                :body             body
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn list-avus
  [app-id]
  (client/get (apps-url "apps" app-id "metadata")
            {:query-params     (secured-params)
             :as               :stream
             :follow-redirects false}))

(defn set-avus
  [app-id body]
  (client/put (apps-url "apps" app-id "metadata")
              {:query-params     (secured-params)
               :body             body
               :content-type     :json
               :as               :stream
               :follow-redirects false}))

(defn update-avus
  [app-id body]
  (client/post (apps-url "apps" app-id "metadata")
               {:query-params     (secured-params)
                :body             body
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn make-app-public
  [system-id app-id app]
  (client/post (apps-url "apps" system-id app-id "publish")
               {:query-params     (secured-params)
                :body             app
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn delete-rating
  [system-id app-id]
  (client/delete (apps-url "apps" system-id app-id "rating")
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn rate-app
  [system-id app-id rating]
  (client/post (apps-url "apps" system-id app-id "rating")
               {:query-params     (secured-params)
                :body             rating
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn list-app-tasks
  [system-id app-id]
  (client/get (apps-url "apps" system-id app-id "tasks")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-app-ui
  [system-id app-id]
  (client/get (apps-url "apps" system-id app-id "ui")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn add-pipeline
  [pipeline]
  (client/post (apps-url "apps" "pipelines")
               {:query-params     (secured-params)
                :content-type     :json
                :body             pipeline
                :as               :stream
                :follow-redirects false}))

(defn update-pipeline
  [app-id pipeline]
  (client/put (apps-url "apps" "pipelines" app-id)
              {:query-params     (secured-params)
               :content-type     :json
               :body             pipeline
               :as               :stream
               :follow-redirects false}))

(defn copy-pipeline
  [app-id]
  (client/post (apps-url "apps" "pipelines" app-id "copy")
               {:query-params     (secured-params)
                :as               :stream
                :follow-redirects false}))

(defn edit-pipeline
  [app-id]
  (client/get (apps-url "apps" "pipelines" app-id "ui")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn list-jobs
  [params]
  (client/get (apps-url "analyses")
              {:query-params     (secured-params params apps-analysis-listing-params)
               :as               :stream
               :follow-redirects false}))

(defn list-job-permissions
  [body params]
  (client/post (apps-url "analyses" "permission-lister")
               {:query-params     (secured-params params permission-lister-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn share-jobs
  [body]
  (client/post (apps-url "analyses" "sharing")
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn unshare-jobs
  [body]
  (client/post (apps-url "analyses" "unsharing")
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn submit-job
  [submission]
  (client/post (apps-url "analyses")
               {:query-params     (secured-params)
                :content-type     :json
                :body             submission
                :as               :stream
                :follow-redirects false}))

(defn update-job
  [analysis-id body]
  (client/patch (apps-url "analyses" analysis-id)
                {:query-params     (secured-params)
                 :content-type     :json
                 :body             body
                 :as               :stream
                 :follow-redirects false}))

(defn delete-job
  [analysis-id]
  (client/delete (apps-url "analyses" analysis-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn delete-jobs
  [body]
  (client/post (apps-url "analyses" "shredder")
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn get-job-params
  [analysis-id]
  (client/get (apps-url "analyses" analysis-id "parameters")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-job-relaunch-info
  [analysis-id]
  (client/get (apps-url "analyses" analysis-id "relaunch-info")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn list-job-steps
  [analysis-id]
  (client/get (apps-url "analyses" analysis-id "steps")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn stop-job
  [analysis-id params]
  (client/post (apps-url "analyses" analysis-id "stop")
               {:query-params     (secured-params params [:job_status])
                :as               :stream
                :follow-redirects false}))

(defn admin-get-apps
  [params]
  (client/get (apps-url "admin" "apps")
              {:query-params     (secured-params params admin-apps-search-params)
               :as               :stream
               :follow-redirects false}))

(defn categorize-apps
  [body]
  (client/post (apps-url "admin" "apps")
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn permanently-delete-apps
  [body]
  (client/post (apps-url "admin" "apps" "shredder")
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn admin-delete-app
  [system-id app-id]
  (client/delete (apps-url "admin" "apps" system-id app-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn admin-update-app
  [system-id app-id body]
  (client/patch (apps-url "admin" "apps" system-id app-id)
                {:query-params     (secured-params)
                 :content-type     :json
                 :body             body
                 :as               :stream
                 :follow-redirects false}))

(defn get-admin-app-categories
  [params]
  (client/get (apps-url "admin" "apps" "categories")
              {:query-params     (secured-params params apps-sort-params)
               :as               :stream
               :follow-redirects false}))

(defn search-admin-app-categories
  [params]
  (client/get (apps-url "admin" "apps" "categories" "search")
              {:query-params     (secured-params params [:name])
               :as               :stream
               :follow-redirects false}))

(defn add-category
  [system-id body]
  (client/post (apps-url "admin" "apps" "categories" system-id)
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn delete-category
  [system-id category-id]
  (client/delete (apps-url "admin" "apps" "categories" system-id category-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn update-category
  [system-id category-id body]
  (client/patch (apps-url "admin" "apps" "categories" system-id category-id)
                {:query-params     (secured-params)
                 :content-type     :json
                 :body             body
                 :as               :stream
                 :follow-redirects false}))

(defn get-app-docs
  [system-id app-id]
  (client/get (apps-url "apps" system-id app-id "documentation")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn edit-app-docs
  [system-id app-id docs]
  (client/patch (apps-url "apps" system-id app-id "documentation")
                {:query-params     (secured-params)
                 :content-type     :json
                 :body             docs
                 :as               :stream
                 :follow-redirects false}))

(defn add-app-docs
  [system-id app-id docs]
  (client/post (apps-url "apps" system-id app-id "documentation")
               {:query-params     (secured-params)
                :content-type     :json
                :body             docs
                :as               :stream
                :follow-redirects false}))

(defn admin-edit-app-docs
  [system-id app-id docs]
  (client/patch (apps-url "admin" "apps" system-id app-id "documentation")
                {:query-params     (secured-params)
                 :content-type     :json
                 :body             docs
                 :as               :stream
                 :follow-redirects false}))

(defn admin-add-app-docs
  [system-id app-id docs]
  (client/post (apps-url "admin" "apps" system-id app-id "documentation")
               {:query-params     (secured-params)
                :content-type     :json
                :body             docs
                :as               :stream
                :follow-redirects false}))


(defn get-oauth-access-token
  [api-name params]
  (client/get (apps-url "oauth" "access-code" api-name)
              {:query-params     (secured-params params [:code :state])
               :as               :stream
               :follow-redirects false}))

(defn get-oauth-token-info
  [api-name]
  (client/get (apps-url "oauth" "token-info" api-name)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn delete-oauth-token-info
  [api-name]
  (client/delete (apps-url "oauth" "token-info" api-name)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn get-oauth-redirect-uris
  []
  (client/get (apps-url "oauth" "redirect-uris")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-admin-oauth-token-info
  [api-name params]
  (client/get (apps-url "admin" "oauth" "token-info" api-name)
              {:query-params     (secured-params params [:proxy-user])
               :as               :stream
               :follow-redirects false}))

(defn delete-admin-oauth-token-info
  [api-name params]
  (client/delete (apps-url "admin" "oauth" "token-info" api-name)
                 {:query-params     (secured-params params [:proxy-user])
                  :as               :stream
                  :follow-redirects false}))

(defn admin-delete-tool-request-status-code
  [status-code-id]
  (client/delete (apps-url "admin" "tool-requests" "status-codes" status-code-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn admin-list-tool-requests
  [params]
  (client/get (apps-url "admin" "tool-requests")
              {:query-params     (secured-params params (conj apps-sort-params :status))
               :as               :stream
               :follow-redirects false}))

(defn admin-delete-tool-request
  [request-id]
  (client/delete (apps-url "admin" "tool-requests" request-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn list-tool-request-status-codes
  [params]
  (client/get (apps-url "tool-requests" "status-codes")
              {:query-params     (secured-params params [:filter])
               :as               :stream
               :follow-redirects false}))

(defn get-tools-in-app
  [system-id app-id]
  (client/get (apps-url "apps" system-id app-id "tools")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn admin-list-tools
  [params]
  (client/get (apps-url "admin" "tools")
              {:query-params     (secured-params params tools-search-params)
               :as               :stream
               :follow-redirects :false}))

(defn admin-add-tools
  [body]
  (client/post (apps-url "admin" "tools")
               {:query-params     (secured-params)
                :as               :stream
                :body             body
                :content-type     :json
                :follow-redirects false}))

(defn admin-delete-tool
  [tool-id]
  (client/delete (apps-url "admin" "tools" tool-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn admin-get-tool
  [tool-id]
  (client/get (apps-url "admin" "tools" tool-id)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn admin-update-tool
  [tool-id params tool]
  (client/patch (apps-url "admin" "tools" tool-id)
                {:query-params     (secured-params params [:overwrite-public])
                 :as               :stream
                 :body             tool
                 :content-type     :json
                 :follow-redirects false}))

(defn admin-get-apps-by-tool
  [tool-id]
  (client/get (apps-url "admin" "tools" tool-id "apps")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn admin-publish-tool
  [tool-id body]
  (client/post (apps-url "admin" "tools" tool-id "publish")
               {:query-params     (secured-params)
                :body             body
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn list-tools
  [params]
  (client/get (apps-url "tools")
              {:query-params     (secured-params params tools-search-params)
               :as               :stream
               :follow-redirects :false}))

(defn create-private-tool
  [body]
  (client/post (apps-url "tools")
               {:query-params     (secured-params)
                :body             body
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn list-tool-permissions
  [body params]
  (client/post (apps-url "tools" "permission-lister")
               {:query-params     (secured-params params permission-lister-params)
                :body             body
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn share-tool
  [body]
  (client/post (apps-url "tools" "sharing")
               {:query-params     (secured-params)
                :body             body
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn unshare-tool
  [body]
  (client/post (apps-url "tools" "unsharing")
               {:query-params     (secured-params)
                :body             body
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn delete-private-tool
  [tool-id params]
  (client/delete (apps-url "tools" tool-id)
                 {:query-params     (secured-params params [:force-delete])
                  :as               :stream
                  :follow-redirects false}))

(defn get-tool
  [tool-id]
  (client/get (apps-url "tools" tool-id)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn update-private-tool
  [tool-id tool]
  (client/patch (apps-url "tools" tool-id)
                {:query-params     (secured-params)
                 :as               :stream
                 :body             tool
                 :content-type     :json
                 :follow-redirects false}))

(defn get-apps-by-tool
  [tool-id]
  (client/get (apps-url "tools" tool-id "apps")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn list-reference-genomes
  [params]
  (client/get (apps-url "reference-genomes")
              {:query-params     (secured-params params [:deleted])
               :as               :stream
               :follow-redirects false}))

(defn get-reference-genome
  [reference-genome-id]
  (client/get (apps-url "reference-genomes" reference-genome-id)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn admin-delete-reference-genome
  [reference-genome-id params]
  (client/delete (apps-url "admin" "reference-genomes" reference-genome-id)
                 {:query-params     (secured-params params [:permanent])
                  :as               :stream
                  :follow-redirects false}))

(defn get-collaborators
  []
  (client/get (apps-url "collaborators")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn add-collaborators
  [body]
  (client/post (apps-url "collaborators")
               {:query-params     (secured-params)
                :as               :stream
                :body             body
                :content-type     :json
                :follow-redirects false}))

(defn remove-collaborators
  [body]
  (client/post (apps-url "collaborators" "shredder")
               {:query-params     (secured-params)
                :as               :stream
                :body             body
                :content-type     :json
                :follow-redirects false}))

(defn get-users-by-id
  [body]
  (client/post (apps-url "users" "by-id")
               {:query-params     (secured-params)
                :as               :stream
                :body             body
                :content-type     :json
                :follow-redirects false}))

(defn get-authenticated-user
  []
  (client/get (apps-url "users" "authenticated")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn record-login
  [ip-address user-agent]
  (let [params {:ip-address ip-address :user-agent user-agent}]
    (client/post (apps-url "users" "login")
                 {:query-params     (secured-params params)
                  :as               :stream
                  :follow-redirects false})))

(defn record-logout
  [ip-address login-time]
  (let [params {:ip-address ip-address :login-time login-time}]
    (client/post (apps-url "users" "logout")
                 {:query-params     (secured-params params)
                  :as               :stream
                  :follow-redirects false})))

(defn list-integration-data
  [params]
  (client/get (apps-url "admin" "integration-data")
              {:query-params     (secured-params params base-search-params)
               :as               :stream
               :follow-redirects false}))

(defn add-integration-data
  [body]
  (client/post (apps-url "admin" "integration-data")
               {:query-params     (secured-params)
                :as               :stream
                :body             body
                :content-type     :json
                :follow-redirects false}))

(defn get-integration-data
  [integration-data-id]
  (client/get (apps-url "admin" "integration-data" integration-data-id)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn update-integration-data
  [integration-data-id body]
  (client/put (apps-url "admin" "integration-data" integration-data-id)
              {:query-params     (secured-params)
               :as               :stream
               :body             body
               :content-type     :json
               :follow-redirects false}))

(defn delete-integration-data
  [integration-data-id]
  (client/delete (apps-url "admin" "integration-data" integration-data-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn get-app-integration-data
  [system-id app-id]
  (client/get (apps-url "apps" system-id app-id "integration-data")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-tool-integration-data
  [tool-id]
  (client/get (apps-url "tools" tool-id "integration-data")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn update-app-integration-data
  [system-id app-id integration-data-id]
  (client/put (apps-url "admin" "apps" system-id app-id "integration-data" integration-data-id)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn update-tool-integration-data
  [tool-id integration-data-id]
  (client/put (apps-url "admin" "tools" tool-id "integration-data" integration-data-id)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-workshop-group
  []
  (client/get (apps-url "admin" "groups" "workshop")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-workshop-group-members
  []
  (client/get (apps-url "admin" "groups" "workshop" "members")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn update-workshop-group-members
  [body]
  (client/put (apps-url "admin" "groups" "workshop" "members")
              {:query-params     (secured-params)
               :as               :stream
               :body             body
               :content-type     :json
               :follow-redirects false}))

(defn bootstrap
  []
  (client/get (apps-url "bootstrap")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn save-webhooks
  [webhooks]
  (client/put (apps-url "webhooks")
            {:query-params     (secured-params)
             :as               :stream
             :body             webhooks
             :content-type     :json
             :follow-redirects false}))

(defn admin-list-workspaces
  [params]
  (client/get (apps-url "admin" "workspaces")
              {:query-params     (secured-params params [:username])
               :as               :stream
               :follow-redirects false}))

(defn admin-delete-workspaces
  [params]
  (client/delete (apps-url "admin" "workspaces")
                 {:query-params     (secured-params params [:username])
                  :as               :stream
                  :follow-redirects false}))

(defn get-webhook-types
  []
  (client/get (apps-url "webhooks" "types")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))
