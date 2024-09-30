(ns terrain.clients.metadata.raw
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure-commons.core :refer [remove-nil-values]]
            [metadata-client.core :as metadata-client]
            [terrain.util.config :as config]
            [terrain.util.transformers :refer [user-params]]))

(defn- metadata-url
  [& components]
  (str (apply curl/url (config/metadata-base-url) components)))

(defn- metadata-url-encoded
  [& components]
  (str (apply curl/url (config/metadata-base-url) (map curl/url-encode components))))

(def target-type-app "app")

(defn resolve-data-type
  "Returns a type converted from the type field of a stat result to a type expected by the
   metadata service endpoints."
  [type]
  (let [type (name type)]
    (if (= type "dir")
    "folder"
    type)))

(defn get-options
  ([]
   (get-options {}))
  ([params]
   {:query-params (user-params params)
    :as           :stream})
  ([params param-keys]
   (get-options (user-params params param-keys))))

(defn json-get-options
  ([]
   (json-get-options {}))
  ([params]
   {:query-params (user-params params)
    :as           :json})
  ([params param-keys]
   (json-get-options (user-params params param-keys))))

(def json-delete-options json-get-options)

(def delete-options get-options)

(defn post-options
  ([body]
   (post-options body {}))
  ([body params]
   {:query-params (user-params params)
    :body         body
    :content-type :json
    :as           :stream}))

(defn json-post-options
  ([body]
   (json-post-options body {}))
  ([body params]
   {:query-params (user-params params)
    :form-params  body
    :content-type :json
    :as           :json}))

(def put-options post-options)

(defn find-avus
  [target-type attr value]
  (metadata-client/find-avus (config/metadata-client)
                             (:user (user-params))
                             {:target-type target-type
                              :attribute   attr
                              :value       value}))

(defn update-avus
  "Adds or updates Metadata AVUs on the given target item."
  [target-type target-id avus-req]
  (metadata-client/update-avus (config/metadata-client)
                               (:user (user-params))
                               target-type
                               target-id
                               (json/encode avus-req)))

(defn list-data-comments
  [target-id]
  (:body (http/get (metadata-url "filesystem" "data" target-id "comments")
                   {:as               :json
                    :follow_redirects false})))

(defn list-app-comments
  [target-id]
  (:body (http/get (metadata-url "apps" target-id "comments")
                   {:as               :json
                    :follow_redirects false})))

(defn list-comments-by-user
  [commenter-id]
  (:body (http/get (metadata-url "admin" "comments" commenter-id) (json-get-options))))

(defn delete-comments-by-user
  [commenter-id]
  (http/delete (metadata-url "admin" "comments" commenter-id) (delete-options)))

(defn add-data-comment
  [target-id data-type body]
  (:body (http/post (metadata-url "filesystem" "data" target-id "comments")
                    (json-post-options body {:data-type data-type}))))

(defn add-app-comment
  [target-id body]
  (:body (http/post (metadata-url "apps" target-id "comments") (json-post-options body))))

(defn update-data-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "filesystem" "data" target-id "comments" comment-id)
              (post-options nil {:retracted retracted})))

(defn update-app-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "apps" target-id "comments" comment-id)
              (post-options nil {:retracted retracted})))

(defn admin-update-data-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "admin" "filesystem" "data" target-id "comments" comment-id)
    (post-options nil {:retracted retracted})))

(defn admin-update-app-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "admin" "apps" target-id "comments" comment-id)
    (post-options nil {:retracted retracted})))

(defn delete-data-comment
  [target-id comment-id]
  (http/delete (metadata-url "admin" "filesystem" "data" target-id "comments" comment-id)
    (delete-options)))

(defn delete-app-comment
  [target-id comment-id]
  (http/delete (metadata-url "admin" "apps" target-id "comments" comment-id) (delete-options)))

(defn list-favorites
  [entity-type]
  (http/get (metadata-url "favorites" "filesystem") (get-options {:entity-type entity-type})))

(defn remove-selected-favorites
  [entity-type]
  (http/delete (metadata-url "favorites" "filesystem") (delete-options {:entity-type entity-type})))

(defn remove-favorite
  [target-id]
  (http/delete (metadata-url "favorites" "filesystem" target-id) (delete-options)))

(defn add-favorite
  [target-id data-type]
  (http/put (metadata-url "favorites" "filesystem" target-id)
            (post-options nil {:data-type data-type})))

(defn filter-favorites
  [uuids]
  (http/post (metadata-url "favorites" "filter") (post-options (json/encode {:filesystem uuids}))))

(defn list-all-attached-tags
  []
  (:body (http/get (metadata-url "filesystem" "data" "tags") (json-get-options))))

(defn remove-all-attached-tags
  []
  (:body (http/delete (metadata-url "filesystem" "data" "tags") (json-delete-options))))

(defn list-attached-tags
  [target-id]
  (:body (http/get (metadata-url "filesystem" "data" target-id "tags") (json-get-options))))

(defn update-attached-tags
  [target-id data-type params body]
  (:body (http/patch (metadata-url "filesystem" "data" target-id "tags")
              (json-post-options body
                                 {:data-type data-type
                                  :type (:type params)}))))

(defn get-tags-by-value
  [contains limit]
  (:body (http/get (metadata-url "tags" "suggestions") (json-get-options (remove-nil-values {:contains contains
                                                                                             :limit    limit})))))

(defn list-user-tags
  []
  (:body (http/get (metadata-url "tags" "user") (json-get-options))))

(defn delete-all-user-tags
  []
  (:body (http/delete (metadata-url "tags" "user") (json-delete-options))))

(defn create-user-tag
  [body]
  (:body (http/post (metadata-url "tags" "user") (json-post-options body))))

(defn update-user-tag
  [tag-id body]
  (:body (http/patch (metadata-url "tags" "user" tag-id) (json-post-options body))))

(defn delete-user-tag
  [tag-id]
  (:body (http/delete (metadata-url "tags" "user" tag-id) (json-delete-options))))

(defn list-templates
  []
  (http/get (metadata-url "templates") (get-options)))

(defn get-template
  [template-id]
  (http/get (metadata-url "templates" template-id) (get-options)))

(defn get-template-csv
  [template-id]
  (http/get (metadata-url "templates" template-id "blank-csv") (get-options)))

(defn get-template-guide
  [template-id]
  (http/get (metadata-url "templates" template-id "guide-csv") (get-options)))

(defn get-template-zip
  [template-id]
  (http/get (metadata-url "templates" template-id "zip-csv") (get-options)))

(defn get-attribute
  [attr-id]
  (http/get (metadata-url "templates" "attr" attr-id) (get-options)))

(defn admin-list-templates
  []
  (http/get (metadata-url "admin" "templates") (get-options)))

(defn admin-add-template
  [template]
  (http/post (metadata-url "admin" "templates") (post-options template)))

(defn admin-update-template
  [template-id template]
  (http/put (metadata-url "admin" "templates" template-id)
            (put-options template)))

(defn admin-delete-template
  [template-id params]
  (http/delete (metadata-url "admin" "templates" template-id) (delete-options params [:permanent])))

(defn get-ontology-hierarchies
  [ontology-version]
  (metadata-client/list-hierarchies (config/metadata-client) (:user (user-params)) ontology-version))

(defn upload-ontology
  [filename content-type istream]
  (http/post (metadata-url "admin" "ontologies")
             {:query-params     (user-params {})
              :multipart        [{:part-name "ontology-xml"
                                  :name      filename
                                  :mime-type content-type
                                  :content   istream}]}))

(defn delete-app-category-hierarchy
  [ontology-version root-iri]
  (http/delete (metadata-url-encoded "admin" "ontologies" ontology-version root-iri) (delete-options)))

(defn save-ontology-hierarchy
  [ontology-version root-iri]
  (http/put (metadata-url-encoded "admin" "ontologies" ontology-version root-iri) (get-options)))

(defn list-permanent-id-requests
  [params]
  (:body (http/get (metadata-url "permanent-id-requests")
                   (json-get-options params [:statuses
                                             :limit
                                             :offset
                                             :sort-field
                                             :sort-dir]))))

(defn create-permanent-id-request
  [request]
  (:body (http/post (metadata-url "permanent-id-requests") (json-post-options request))))

(defn list-permanent-id-request-status-codes
  []
  (:body (http/get (metadata-url "permanent-id-requests" "status-codes") (json-get-options))))

(defn list-permanent-id-request-types
  []
  (:body (http/get (metadata-url "permanent-id-requests" "types") (json-get-options))))

(defn get-permanent-id-request
  [request-id]
  (:body (http/get (metadata-url "permanent-id-requests" request-id) (json-get-options))))

(defn admin-list-permanent-id-requests
  [params]
  (:body (http/get (metadata-url "admin" "permanent-id-requests")
                   (json-get-options params [:statuses
                                             :limit
                                             :offset
                                             :sort-field
                                             :sort-dir]))))

(defn admin-get-permanent-id-request
  [request-id]
  (:body (http/get (metadata-url "admin" "permanent-id-requests" request-id) (json-get-options))))

(defn update-permanent-id-request
  [request-id request]
  (:body (http/post (metadata-url "admin" "permanent-id-requests" request-id "status")
                    (json-post-options request))))
