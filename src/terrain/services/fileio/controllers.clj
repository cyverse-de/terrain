(ns terrain.services.fileio.controllers
  (:require [cemerick.url :as url-parser]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.file-utils :as ft]
            [clojure.string :as string]
            [ring.middleware.multipart-params :as multipart]
            [slingshot.slingshot :refer [throw+]]
            [terrain.clients.data-info :as data]
            [terrain.clients.data-info.raw :as data-raw]
            [terrain.services.fileio.actions :as actions])
  (:import [java.io ByteArrayInputStream]))

(defn download
  [{user :shortUsername} {:keys [path]}]
  (actions/download (or user "anonymous") path))

(defn store-fn
  "Returns a function that can be used to forward a file upload request to the data-info service. The function
  that is returned can be passed to ring.middleware.multipart-params/multipart-params-request as the :store option."
  [user dest]
  (fn [{:keys [filename content-type stream] :as file-info}]
    (merge (select-keys file-info [:filename :content-type])
           (data-raw/upload-file user dest filename content-type stream))))

(defn wrap-file-upload
  "This is a specialized replacement for ring.middleware.multipart-params/wrap-mutlipart-params that forwards uploads
  to the data-info service. The username and file destination are extracted from the request before processing the
  multipart parameters."
  [handler]
  (fn [{{:keys [user]} :user-info {:keys [dest]} :params :as req}]
    (handler (multipart/multipart-params-request req {:store (store-fn user dest)}))))

(defn overwrite-store-fn
  "Returns a multipart :store function that overwrites the existing file at dest in the data-info
  service with the uploaded part's contents. Unlike the string-based save endpoint, the part's
  stream is forwarded as-is, so binary content survives the round trip."
  [user dest]
  (fn [{:keys [stream] :as file-info}]
    (merge (select-keys file-info [:filename :content-type])
           (data/overwrite-file user dest stream))))

(defn wrap-file-overwrite
  "Like wrap-file-upload, but overwrites the existing file at the dest path instead of creating a
  new file in a destination directory."
  [handler]
  (fn [{{:keys [user]} :user-info {:keys [dest]} :params :as req}]
    (handler (multipart/multipart-params-request req {:store (overwrite-store-fn user dest)}))))

(defn saveas
  "Save a file to a location given the content in a (utf-8) string."
  [{user :shortUsername} {:keys [dest content]}]
  (let [dest    (string/trim dest)
        dir     (ft/dirname dest)
        file    (ft/basename dest)
        istream (ByteArrayInputStream. (.getBytes content "UTF-8"))]
    (data-raw/upload-file user dir file "application/octet-stream" istream)))

(defn save
  [{user :shortUsername} {:keys [dest content]}]
  (let [dest    (string/trim dest)
        istream (ByteArrayInputStream. (.getBytes content "UTF-8"))]
    (data/overwrite-file user dest istream)))

(defn- url-filename
  [address]
  (let [parsed-url (url-parser/url address)]
    (when-not (:protocol parsed-url)
      (throw+ {:error_code ce/ERR_INVALID_URL
               :url        address}))

    (when-not (:host parsed-url)
      (throw+ {:error_code ce/ERR_INVALID_URL
               :url        address}))

    (if-not (string/blank? (:path parsed-url))
      (ft/basename (:path parsed-url))
      (:host parsed-url))))

(defn urlupload
  [{user :shortUsername} {:keys [dest address]}]
  (let [dest    (string/trim dest)
        address (string/trim address)
        fname   (url-filename address)]
    (actions/urlimport user address fname dest)))
