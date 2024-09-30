(ns terrain.clients.datacite
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure.data.codec.base64 :as base64]
            [ring.util.http-response :refer [charset]]
            [slingshot.slingshot :refer [try+ throw+]]
            [terrain.util.config :as config]))

(defn- auth-params
  []
  [(config/datacite-username) (config/datacite-password)])

(defn- encode-datacite-xml
  [datacite-xml]
  (-> datacite-xml
      (.getBytes "UTF-8")
      base64/encode
      (String. "UTF-8")))

(defn- decode-datacite-xml
  [datacite-xml]
  (try+
    (-> datacite-xml
        (.getBytes "UTF-8")
        base64/decode
        (String. "UTF-8"))
    (catch Object _
      datacite-xml)))

(defn- datacite-post
  "Posts a request to the DataCite API and returns its response."
  [body & uri-parts]
  (->> (charset {:basic-auth   (auth-params)
                 :form-params  body
                 :content-type :json
                 :as           :json}
                "utf-8")
       (http/post (str (apply curl/url (config/datacite-api-url) uri-parts)))
       :body))

(defn- format-datacite-publish-request
  [datacite-xml target-url]
  {:data {:type       "dois"
          :attributes {:event  "publish"
                       :prefix (config/datacite-doi-prefix)
                       :url    target-url
                       :xml    (encode-datacite-xml datacite-xml)}}})

(defn create-doi
  "Publishes a new DOI with the given metadata, returning the new DOI.
   https://support.datacite.org/docs/api-create-dois"
  [datacite-xml target-url]
  (try+
    (let [response (datacite-post
                     (format-datacite-publish-request datacite-xml
                                                      target-url)
                     "dois")]
      (when-not (and (map? response) (get-in response [:data :id]))
        (throw+ {:type     :clojure-commons.exception/request-failed
                 :error    "DOI not found in DataCite response."
                 :response response}))
      (update-in response [:data :attributes :xml] decode-datacite-xml))
    (catch [:status 400] {:keys [body]}
      (throw+ {:type  :clojure-commons.exception/bad-request
               :error body}))))
