(ns terrain.clients.apps.raw
  (:use [terrain.util.transformers :only [secured-params]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as client]
            [terrain.util.config :as config]))

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
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

(defn get-workflow-elements
  [element-type params]
  (:body
    (client/get (apps-url "apps" "elements" element-type)
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

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
   (:body
     (client/get (apps-url-encoded "apps" "hierarchies" root-iri)
                 {:query-params     (secured-params params [:attr])
                  :as               :json
                  :follow-redirects false}))))

(defn get-app-category-hierarchies
  []
  (:body
    (client/get (apps-url "apps" "hierarchies")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn get-hierarchy-app-listing
  ([ontology-version root-iri params]
   (client/get (apps-url-encoded "admin" "ontologies" ontology-version root-iri "apps")
               {:query-params     (secured-params params apps-hierarchy-sort-params)
                :as               :stream
                :follow-redirects false}))
  ([root-iri params]
   (:body
     (client/get (apps-url-encoded "apps" "hierarchies" root-iri "apps")
                 {:query-params     (secured-params params apps-hierarchy-sort-params)
                  :as               :json
                  :follow-redirects false}))))

(defn get-unclassified-app-listing
  ([ontology-version root-iri params]
   (client/get (apps-url-encoded "admin" "ontologies" ontology-version root-iri "unclassified")
               {:query-params     (secured-params params apps-hierarchy-sort-params)
                :as               :stream
                :follow-redirects false}))
  ([root-iri params]
   (:body
     (client/get (apps-url-encoded "apps" "hierarchies" root-iri "unclassified")
                 {:query-params     (secured-params params apps-hierarchy-sort-params)
                  :as               :json
                  :follow-redirects false}))))

(defn get-app-categories
  [params]
  (:body
    (client/get (apps-url "apps" "categories")
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

(defn apps-in-category
  [system-id category-id params]
  (:body
    (client/get (apps-url "apps" "categories" system-id category-id)
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

(defn apps-in-community
  [community-id]
  (:body
    (client/get (apps-url "apps" "communities" community-id "apps")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn admin-get-apps-in-community
  [community-id  & {:keys [as] :or {as :stream}}]
  (client/get (apps-url "admin" "apps" "communities" community-id "apps")
              {:query-params     (secured-params)
               :as               as
               :follow-redirects false}))

(defn search-apps
  [params]
  (:body
    (client/get (apps-url "apps")
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

(defn create-app
  [system-id app]
  (:body
    (client/post (apps-url "apps" system-id)
                 {:query-params     (secured-params)
                  :form-params      app
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn preview-args
  [system-id app]
  (:body
    (client/post (apps-url "apps" system-id "arg-preview")
                 {:query-params     (secured-params)
                  :form-params      app
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn delete-apps
  [deletion-request]
  (:body
    (client/post (apps-url "apps" "shredder")
                 {:query-params     (secured-params)
                  :form-params      deletion-request
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn list-permissions
  [body params]
  (:body
    (client/post (apps-url "apps" "permission-lister")
                 {:query-params     (secured-params params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn share
  [body]
  (:body
    (client/post (apps-url "apps" "sharing")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn unshare
  [body]
  (:body
    (client/post (apps-url "apps" "unsharing")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn get-app
  [system-id app-id]
  (:body
    (client/get (apps-url "apps" system-id app-id)
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn delete-app
  [system-id app-id]
  (:body
    (client/delete (apps-url "apps" system-id app-id)
                   {:query-params     (secured-params)
                    :as               :json
                    :follow-redirects false})))

(defn relabel-app
  [system-id app-id relabel-request]
  (:body
    (client/patch (apps-url "apps" system-id app-id)
                  {:query-params     (secured-params)
                   :form-params      relabel-request
                   :content-type     :json
                   :as               :json
                   :follow-redirects false})))

(defn update-app
  [system-id app-id update-request]
  (:body
    (client/put (apps-url "apps" system-id app-id)
                {:query-params     (secured-params)
                 :form-params      update-request
                 :content-type     :json
                 :as               :json
                 :follow-redirects false})))

(defn copy-app
  [system-id app-id]
  (:body
    (client/post (apps-url "apps" system-id app-id "copy")
                 {:query-params     (secured-params)
                  :as               :json
                  :follow-redirects false})))

(defn get-admin-app-details
  [system-id app-id]
  (:body
    (client/get (apps-url "admin" "apps" system-id app-id "details")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn get-app-details
  [system-id app-id]
  (:body
    (client/get (apps-url "apps" system-id app-id "details")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn remove-favorite-app
  [system-id app-id]
  (:body
    (client/delete (apps-url "apps" system-id app-id "favorite")
                   {:query-params     (secured-params)
                    :as               :json
                    :follow-redirects false})))

(defn add-favorite-app
  [system-id app-id]
  (:body
    (client/put (apps-url "apps" system-id app-id "favorite")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn app-publishable?
  [system-id app-id]
  (:body
    (client/get (apps-url "apps" system-id app-id "is-publishable")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn remove-app-from-communities
  [app-id body]
  (:body
    (client/delete (apps-url "apps" app-id "communities")
                   {:query-params     (secured-params)
                    :form-params      body
                    :content-type     :json
                    :as               :json
                    :follow-redirects false})))

(defn update-app-communities
  [app-id body]
  (:body
    (client/post (apps-url "apps" app-id "communities")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

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
  (:body
    (client/get (apps-url "apps" app-id "metadata")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn set-avus
  [app-id body]
  (:body
    (client/put (apps-url "apps" app-id "metadata")
                {:query-params     (secured-params)
                 :form-params      body
                 :content-type     :json
                 :as               :json
                 :follow-redirects false})))

(defn update-avus
  [app-id body]
  (:body
    (client/post (apps-url "apps" app-id "metadata")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn make-app-public
  [system-id app-id app]
  (:body
    (client/post (apps-url "apps" system-id app-id "publish")
                 {:query-params     (secured-params)
                  :form-params      app
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn delete-rating
  [system-id app-id]
  (:body
    (client/delete (apps-url "apps" system-id app-id "rating")
                   {:query-params     (secured-params)
                    :as               :json
                    :follow-redirects false})))

(defn rate-app
  [system-id app-id rating]
  (:body
    (client/post (apps-url "apps" system-id app-id "rating")
                 {:query-params     (secured-params)
                  :form-params      rating
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn list-app-tasks
  [system-id app-id]
  (:body
    (client/get (apps-url "apps" system-id app-id "tasks")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn get-app-ui
  [system-id app-id]
  (:body
    (client/get (apps-url "apps" system-id app-id "ui")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn add-pipeline
  [pipeline]
  (:body
    (client/post (apps-url "apps" "pipelines")
                 {:query-params     (secured-params)
                  :form-params      pipeline
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn update-pipeline
  [app-id pipeline]
  (:body
    (client/put (apps-url "apps" "pipelines" app-id)
                {:query-params     (secured-params)
                 :form-params      pipeline
                 :content-type     :json
                 :as               :json
                 :follow-redirects false})))

(defn copy-pipeline
  [app-id]
  (:body
    (client/post (apps-url "apps" "pipelines" app-id "copy")
                 {:query-params     (secured-params)
                  :as               :json
                  :follow-redirects false})))

(defn edit-pipeline
  [app-id]
  (:body
    (client/get (apps-url "apps" "pipelines" app-id "ui")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn list-jobs
  [params]
  (:body
    (client/get (apps-url "analyses")
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

(defn list-job-permissions
  [body params]
  (:body
    (client/post (apps-url "analyses" "permission-lister")
                 {:query-params     (secured-params params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn share-jobs
  [body]
  (:body
    (client/post (apps-url "analyses" "sharing")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn unshare-jobs
  [body]
  (:body
    (client/post (apps-url "analyses" "unsharing")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn submit-job
  [body]
  (:body
    (client/post (apps-url "analyses")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn update-job
  [analysis-id body]
  (:body
    (client/patch (apps-url "analyses" analysis-id)
                  {:query-params     (secured-params)
                   :form-params      body
                   :content-type     :json
                   :as               :json
                   :follow-redirects false})))

(defn delete-job
  [analysis-id]
  (:body
    (client/delete (apps-url "analyses" analysis-id)
                   {:query-params     (secured-params)
                    :as               :json
                    :follow-redirects false})))

(defn delete-jobs
  [body]
  (:body
    (client/post (apps-url "analyses" "shredder")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn get-job-params
  [analysis-id]
  (:body
    (client/get (apps-url "analyses" analysis-id "parameters")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn get-job-relaunch-info
  [analysis-id]
  (:body
    (client/get (apps-url "analyses" analysis-id "relaunch-info")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn list-job-steps
  [analysis-id]
  (:body
    (client/get (apps-url "analyses" analysis-id "steps")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn stop-job
  [analysis-id params]
  (:body
    (client/post (apps-url "analyses" analysis-id "stop")
                 {:query-params     (secured-params params)
                  :as               :json
                  :follow-redirects false})))

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
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

(defn categorize-apps
  [body]
  (:body
    (client/post (apps-url "admin" "apps")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn list-app-publication-requests
  [params]
  (:body
   (client/get (apps-url "admin" "apps" "publication-requests")
               {:query-params     (secured-params params)
                :as               :json
                :follow-redirects false})))

(defn permanently-delete-apps
  [body]
  (:body
    (client/post (apps-url "admin" "apps" "shredder")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn admin-delete-app
  [system-id app-id]
  (:body
    (client/delete (apps-url "admin" "apps" system-id app-id)
                   {:query-params     (secured-params)
                    :as               :json
                    :follow-redirects false})))

(defn admin-update-app
  [system-id app-id body]
  (:body
    (client/patch (apps-url "admin" "apps" system-id app-id)
                  {:query-params     (secured-params)
                   :form-params      body
                   :content-type     :json
                   :as               :json
                   :follow-redirects false})))

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
  (:body
    (client/get (apps-url "apps" system-id app-id "documentation")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn edit-app-docs
  [system-id app-id docs]
  (:body
    (client/patch (apps-url "apps" system-id app-id "documentation")
                  {:query-params     (secured-params)
                   :form-params      docs
                   :content-type     :json
                   :as               :json
                   :follow-redirects false})))

(defn add-app-docs
  [system-id app-id docs]
  (:body
    (client/post (apps-url "apps" system-id app-id "documentation")
                 {:query-params     (secured-params)
                  :form-params      docs
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn admin-edit-app-docs
  [system-id app-id docs]
  (:body
    (client/patch (apps-url "admin" "apps" system-id app-id "documentation")
                  {:query-params     (secured-params)
                   :form-params      docs
                   :content-type     :json
                   :as               :json
                   :follow-redirects false})))

(defn admin-add-app-docs
  [system-id app-id docs]
  (:body
    (client/post (apps-url "admin" "apps" system-id app-id "documentation")
                 {:query-params     (secured-params)
                  :form-params      docs
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))


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
  (:body
    (client/delete (apps-url "admin" "tool-requests" "status-codes" status-code-id)
                   {:query-params     (secured-params)
                    :as               :json
                    :follow-redirects false})))

(defn admin-list-tool-requests
  [params]
  (:body
    (client/get (apps-url "admin" "tool-requests")
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

(defn admin-delete-tool-request
  [request-id]
  (:body
    (client/delete (apps-url "admin" "tool-requests" request-id)
                   {:query-params     (secured-params)
                    :as               :json
                    :follow-redirects false})))

(defn admin-get-tool-request
  [request-id]
  (:body
    (client/get (apps-url "admin" "tool-requests" request-id)
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn admin-update-tool-request
  [body request-id]
  (:body
    (client/post (apps-url "admin" "tool-requests" request-id "status")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn list-tool-requests
  "Lists the tool requests that were submitted by the authenticated user."
  [params]
  (:body
    (client/get (apps-url "tool-requests")
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

(defn submit-tool-request
  "Submits a tool request on behalf of the user found in the request params."
  [body]
  (:body
    (client/post (apps-url "tool-requests")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn list-tool-request-status-codes
  [params]
  (:body
    (client/get (apps-url "tool-requests" "status-codes")
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

(defn get-tools-in-app
  [system-id app-id]
  (:body
    (client/get (apps-url "apps" system-id app-id "tools")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn admin-list-tools
  [params]
  (:body
    (client/get (apps-url "admin" "tools")
                {:query-params     (secured-params params tools-search-params)
                 :as               :json
                 :follow-redirects :false})))

(defn admin-add-tools
  [body]
  (:body
    (client/post (apps-url "admin" "tools")
                 {:query-params     (secured-params)
                  :form-params      body
                  :as               :json
                  :content-type     :json
                  :follow-redirects false})))

(defn admin-delete-tool
  [tool-id]
  (:body
    (client/delete (apps-url "admin" "tools" tool-id)
                   {:query-params     (secured-params)
                    :as               :json
                    :follow-redirects false})))

(defn admin-get-tool
  [tool-id]
  (:body
    (client/get (apps-url "admin" "tools" tool-id)
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn admin-update-tool
  [tool-id params tool]
  (:body
    (client/patch (apps-url "admin" "tools" tool-id)
                  {:query-params     (secured-params params [:overwrite-public])
                   :form-params      tool
                   :as               :json
                   :content-type     :json
                   :follow-redirects false})))

(defn admin-get-apps-by-tool
  [tool-id]
  (:body
    (client/get (apps-url "admin" "tools" tool-id "apps")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn admin-publish-tool
  [tool-id body]
  (:body
    (client/post (apps-url "admin" "tools" tool-id "publish")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn list-tools
  [params]
  (:body
    (client/get (apps-url "tools")
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects :false})))

(defn create-private-tool
  [body]
  (:body
    (client/post (apps-url "tools")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn list-tool-permissions
  [body params]
  (:body
    (client/post (apps-url "tools" "permission-lister")
                 {:query-params     (secured-params params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn share-tool
  [body]
  (:body
    (client/post (apps-url "tools" "sharing")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn unshare-tool
  [body]
  (:body
    (client/post (apps-url "tools" "unsharing")
                 {:query-params     (secured-params)
                  :form-params      body
                  :content-type     :json
                  :as               :json
                  :follow-redirects false})))

(defn delete-private-tool
  [tool-id params]
  (:body
    (client/delete (apps-url "tools" tool-id)
                   {:query-params     (secured-params params)
                    :as               :json
                    :follow-redirects false})))

(defn get-tool
  [tool-id]
  (:body
    (client/get (apps-url "tools" tool-id)
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn update-private-tool
  [tool-id body]
  (:body
    (client/patch (apps-url "tools" tool-id)
                  {:query-params     (secured-params)
                   :form-params      body
                   :as               :json
                   :content-type     :json
                   :follow-redirects false})))

(defn get-apps-by-tool
  [tool-id]
  (:body
    (client/get (apps-url "tools" tool-id "apps")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn list-reference-genomes
  [params]
  (:body
    (client/get (apps-url "reference-genomes")
                {:query-params     (secured-params params)
                 :as               :json
                 :follow-redirects false})))

(defn get-reference-genome
  [reference-genome-id]
  (:body
    (client/get (apps-url "reference-genomes" reference-genome-id)
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn admin-add-reference-genome
  [body]
  (:body
    (client/post (apps-url "admin" "reference-genomes")
                 {:query-params     (secured-params)
                  :form-params      body
                  :as               :json
                  :content-type     :json
                  :follow-redirects false})))

(defn admin-delete-reference-genome
  [reference-genome-id params]
  (:body
    (client/delete (apps-url "admin" "reference-genomes" reference-genome-id)
                   {:query-params     (secured-params params)
                    :as               :json
                    :follow-redirects false})))

(defn admin-update-reference-genome
  [body reference-genome-id]
  (:body
    (client/patch (apps-url "admin" "reference-genomes" reference-genome-id)
                  {:query-params     (secured-params)
                   :form-params      body
                   :as               :json
                   :content-type     :json
                   :follow-redirects false})))

(defn record-login
  [ip-address user-agent]
  (let [params {:ip-address ip-address :user-agent user-agent}]
    (:body
      (client/post (apps-url "users" "login")
                   {:query-params     (secured-params params)
                    :as               :json
                    :follow-redirects false}))))

(defn record-logout
  [params]
  (:body
    (client/post (apps-url "users" "logout")
                 {:query-params     (secured-params params)
                  :as               :json
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
  (:body
    (client/get (apps-url "apps" system-id app-id "integration-data")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn get-tool-integration-data
  [tool-id]
  (:body
    (client/get (apps-url "tools" tool-id "integration-data")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn update-app-integration-data
  [system-id app-id integration-data-id]
  (:body
    (client/put (apps-url "admin" "apps" system-id app-id "integration-data" integration-data-id)
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn update-tool-integration-data
  [tool-id integration-data-id]
  (:body
    (client/put (apps-url "admin" "tools" tool-id "integration-data" integration-data-id)
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

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
  (:body
    (client/get (apps-url "bootstrap")
                {:query-params     (secured-params)
                 :as               :json
                 :follow-redirects false})))

(defn save-webhooks
  [webhooks]
  (:body (client/put (apps-url "webhooks")
            {:query-params     (secured-params)
             :as               :json
             :form-params      webhooks
             :content-type     :json
             :follow-redirects false})))

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
  (:body (client/get (apps-url "webhooks" "types")
              {:query-params     (secured-params)
               :as               :json
               :follow-redirects false})))

(defn get-webhook-topics
  []
  (:body (client/get (apps-url "webhooks" "topics")
              {:query-params     (secured-params)
               :as               :json
               :follow-redirects false})))

(defn get-webhooks
  []
  (:body (client/get (apps-url "webhooks")
              {:query-params     (secured-params)
               :as               :json
               :follow-redirects false})))
