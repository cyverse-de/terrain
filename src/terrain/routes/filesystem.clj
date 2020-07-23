(ns terrain.routes.filesystem
  (:use [common-swagger-api.schema]
        [medley.core :only [update-existing-in]]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [require-authentication current-user]]
        [terrain.util :only [controller optional-routes]])
  (:require [clojure.string :as string]
            [compojure.api.middleware :as mw]
            [terrain.clients.data-info :as data]
            [terrain.clients.metadata.raw :as meta-raw]
            [terrain.routes.schemas.filesystem :as fs-schema]
            [terrain.services.filesystem.directory :as dir]
            [terrain.services.filesystem.metadata :as meta]
            [terrain.services.filesystem.metadata-templates :as mt]
            [terrain.services.filesystem.stat :as stat]
            [terrain.services.filesystem.updown :as ud]
            [terrain.util.config :as config]))

(defn- wrap-fix-param [handler param f]
  (if-let [keyword-param (keyword param)]
    (fn [req]
      (as-> req req
        (update-existing-in req [:params keyword-param] f)
        (update-existing-in req [:query-params (name keyword-param)] f)
        (handler req)))
    (throw (Exception. (str "Invalid parameter: " param)))))

(defn- fix-sort-col-param [param-value]
  (as-> param-value v
    (string/lower-case v)
    (cond
      (= v "id")           "path"
      (= v "lastmodified") "datemodified"
      :else                v)))

(defn secured-filesystem-routes
  "The routes for file IO endpoints."
  []
  (optional-routes
   [config/filesystem-routes-enabled]

   (context "/filesystem" []
     :tags ["filesystem"]

     (GET "/display-download" [:as req]
       :middleware [require-authentication]
       (controller req ud/do-special-download :params))

     (GET "/paged-directory" []
       :middleware [[wrap-fix-param :sort-dir string/upper-case]
                    [wrap-fix-param :sort-col fix-sort-col-param]
                    [wrap-fix-param :entity-type string/lower-case]]
       :query [params fs-schema/FolderListingParams]
       :summary "List Folder Contents"
       :description (str "Provides a paged listing of the contents of a folder in the data store.")
       :return fs-schema/PagedFolderListing
       :coercion mw/no-response-coercion
       (ok (dir/do-paged-listing current-user params)))

     (POST "/path-list-creator" [:as req]
       :middleware [require-authentication]
       (controller req data/path-list-creator :params :body))

     (POST "/directories" [:as req]
       :middleware [require-authentication]
       (controller req data/create-dirs :params :body))

     (POST "/directory/create" [:as req]
       :middleware [require-authentication]
       (controller req data/create-dir :params :body))

     (POST "/rename" [:as req]
       :middleware [require-authentication]
       (controller req data/rename :params :body))

     (POST "/delete" [:as req]
       :middleware [require-authentication]
       (controller req data/delete-paths :params :body))

     (POST "/delete-contents" [:as req]
       :middleware [require-authentication]
       (controller req data/delete-contents :params :body))

     (POST "/move" [:as req]
       :middleware [require-authentication]
       (controller req data/move :params :body))

     (POST "/move-contents" [:as req]
       :middleware [require-authentication]
       (controller req data/move-contents :params :body))

     (GET "/file/manifest" [:as req]
       :middleware [require-authentication]
       (controller req data/manifest :params))

     (POST "/user-permissions" [:as req]
       :middleware [require-authentication]
       (controller req data/collect-permissions :params :body))

     (POST "/restore" [:as req]
       :middleware [require-authentication]
       (controller req data/restore-files :params :body))

     (POST "/restore-all" [:as req]
       :middleware [require-authentication]
       (controller req data/restore-files :params))

     (DELETE "/trash" [:as req]
       :middleware [require-authentication]
       (controller req data/delete-trash :params))

     (POST "/read-chunk" [:as req]
       :middleware [require-authentication]
       (controller req data/read-chunk :params :body))

     (POST "/read-csv-chunk" [:as req]
       :middleware [require-authentication]
       (controller req data/read-tabular-chunk :params :body))

     (POST "/anon-files" [:as req]
       :middleware [require-authentication]
       (controller req data/share-with-anonymous :params :body)))))

(defn secured-filesystem-metadata-routes
  "The routes for file metadata endpoints."
  []
  (optional-routes
   [#(and (config/filesystem-routes-enabled)
          (config/metadata-routes-enabled))]

    (POST "/filesystem/metadata/csv-parser" [:as {:keys [user-info params] :as req}]
      (meta/parse-metadata-csv-file user-info params))

    (GET "/filesystem/metadata/templates" [:as req]
      (controller req mt/do-metadata-template-list))

    (GET "/filesystem/metadata/template/:template-id" [template-id :as req]
      (controller req mt/do-metadata-template-view template-id))

    (GET "/filesystem/metadata/template/:template-id/blank-csv" [template-id :as req]
      (controller req meta-raw/get-template-csv template-id))

    (GET "/filesystem/metadata/template/:template-id/guide-csv" [template-id :as req]
      (controller req meta-raw/get-template-guide template-id))

    (GET "/filesystem/metadata/template/:template-id/zip-csv" [template-id :as req]
      (controller req meta-raw/get-template-zip template-id))

    (GET "/filesystem/metadata/template/attr/:attr-id" [attr-id :as req]
      (controller req mt/do-metadata-attribute-view attr-id))

    (GET "/filesystem/:data-id/metadata" [data-id :as req]
      (controller req meta/do-metadata-get :params data-id))

    (POST "/filesystem/:data-id/metadata" [data-id :as req]
      (controller req meta/do-metadata-set data-id :params :body))

    (POST "/filesystem/:data-id/metadata/copy" [data-id :as req]
      (controller req meta/do-metadata-copy :params data-id :body))

    (POST "/filesystem/:data-id/metadata/save" [data-id :as req]
      (controller req meta/do-metadata-save data-id :params :body))

    (POST "/filesystem/:data-id/ore/save" [data-id :as req]
      (controller req meta/do-ore-save data-id :params))))

(defn admin-filesystem-metadata-routes
  "The admin routes for file metadata endpoints."
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/filesystem-routes-enabled)
           (config/metadata-routes-enabled))]

    (GET "/filesystem/metadata/templates" [:as req]
      (controller req mt/do-metadata-template-admin-list))

    (POST "/filesystem/metadata/templates" [:as req]
      (controller req mt/do-metadata-template-add :body))

    (POST "/filesystem/metadata/templates/:template-id" [template-id :as req]
      (controller req mt/do-metadata-template-edit template-id :body))

    (DELETE "/filesystem/metadata/templates/:template-id" [template-id :as req]
      (controller req mt/do-metadata-template-delete template-id :params))))
