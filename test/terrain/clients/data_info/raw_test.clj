(ns terrain.clients.data-info.raw-test
  (:require [clojure.string :as string]
            [clojure.test :refer :all]
            [clj-http.client :as http]
            [clj-http.multipart :as multipart]
            [terrain.clients.data-info.raw :as raw]
            [terrain.util.config :as cfg])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [org.apache.http.entity.mime HttpMultipartMode]))

(def ^:private non-ascii-filename "test-ä-ö-ü.txt")

(defn- serialize-multipart
  "Serializes the multipart body data-info-style upload-file builds, under the
   given HttpMultipartMode, and returns the bytes decoded as UTF-8."
  [filename mode]
  (let [entity (multipart/create-multipart-entity
                [{:part-name "file"
                  :name      filename
                  :mime-type "text/plain"
                  :content   (ByteArrayInputStream. (.getBytes "hello" "UTF-8"))}]
                {:multipart-mode mode})
        baos   (ByteArrayOutputStream.)]
    (.writeTo entity baos)
    (String. (.toByteArray baos) "UTF-8")))

(deftest multipart-mode-filename-encoding
  (testing "only a UTF-8-capable multipart mode preserves non-ASCII filenames"
    (are [mode preserved?]
         (= preserved? (string/includes? (serialize-multipart non-ascii-filename mode)
                                         non-ascii-filename))
      ;; the mode upload-file now uses
      HttpMultipartMode/RFC6532 true
      ;; clj-http's default, which mangles ä/ö/ü to '?'
      HttpMultipartMode/STRICT  false)))

(deftest upload-file-uses-utf8-multipart-mode
  (let [captured (atom nil)]
    (with-redefs [cfg/data-info-base-url (constantly "http://data-info")
                  http/post              (fn [_url opts] (reset! captured opts) {:body {}})]
      (raw/upload-file "user" "/dest/dir" non-ascii-filename "text/plain"
                       (ByteArrayInputStream. (.getBytes "hello" "UTF-8")))
      (testing "requests the RFC6532 multipart mode so filenames are UTF-8 encoded"
        (is (= HttpMultipartMode/RFC6532 (:multipart-mode @captured))))
      (testing "passes the original filename through unchanged"
        (is (= non-ascii-filename (-> @captured :multipart first :name)))))))
