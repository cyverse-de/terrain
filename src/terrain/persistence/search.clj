(ns terrain.persistence.search
  "provides the functions that interact directly with elasticsearch"
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.document :as doc]
            [slingshot.slingshot :refer [try+ throw+]]
            [terrain.util.config :as cfg]
            [clojure-commons.exception :as cx])
  (:import [java.net ConnectException]
           [java.util UUID]
           [clojure.lang IPersistentMap ISeq]))


(def ^:private es-uninitialized {:type   ::cx/invalid-cfg
                                 :error "elasticsearch has not been initialized"})

(defn- connect
  []
  (let [url (cfg/es-url)
        http-opts (if (or (empty? (cfg/es-user)) (empty? (cfg/es-password)))
                    {}
                    {:basic-auth [(cfg/es-user) (cfg/es-password)]
                     :content-type :application/json})]
    (try+
      (es/connect url http-opts)
      (catch ConnectException _
        (throw+ {:type ::cx/invalid-cfg
                 :error "cannot connect to elasticsearch"})))))


(defn update-with-script
  "Scripted updates which are only compatible with Elasticsearch 5.x and greater."
  [es index mapping-type id script params]
  (es/post es (es/record-update-url es index mapping-type id)
             {:body {:script {:inline script :lang "painless" :params params}}}))

(defn index-tag
  "Inserts a tag into the search index.

   Parameters:
     tag - the tag document to insert.

   Throws:
     ::cx/invalid-cfg - This is thrown if there is a problem with elasticsearch"
  [^IPersistentMap tag]
  (try+
    (doc/create (connect) (cfg/es-index) "tag" tag :id (:id tag))
    (catch [:status 404] {:keys []}
      (throw+ es-uninitialized))))


(defn update-tag
  "Updates a tag's label, description, modification date.

   Parameters:
     tag-id - the id of the tag to update
     updates - a map containing the updated values.

   Throws:
     ::cx/invalid-cfg - This is thrown if there is a problem with elasticsearch"
  [^UUID tag-id ^IPersistentMap updates]
  (try+
    (let [script "ctx._source.value = params.value;
                  ctx._source.description = params.description;
                  ctx._source.dateModified = params.dateModified"]
      (update-with-script (connect) (cfg/es-index) "tag" (str tag-id) script updates))
    (catch [:status 404] {:keys []}
      (throw+ es-uninitialized))))


(defn update-tag-targets
  "Updates a tag's target list.

   Parameters:
     tag-id - the id of the tag to update
     targets - a list of the current targets docs.

   Throws:
     ::cx/invalid-cfg - This is thrown if there is a problem with elasticsearch"
  [^UUID tag-id ^ISeq targets]
  (try+
    (let [script "ctx._source.targets = params.targets"
          update {:targets (map #(assoc % :type (str (:type %))) targets)}]
      (update-with-script (connect) (cfg/es-index) "tag" (str tag-id) script update))
    (catch [:status 404] {:keys []}
      (throw+ es-uninitialized))))


(defn remove-tag
  "Removes a tag from the search index.

   Parameters:
     tag-id - the id of the tag document to remove.

   Throws:
     ::cx/invalid-cfg - This is thrown if there is a problem with elasticsearch"
  [^UUID tag-id]
  (try+
    (doc/delete (connect) (cfg/es-index) "tag" (str tag-id))
    (catch [:status 404] {:keys []}
      (throw+ es-uninitialized))))
