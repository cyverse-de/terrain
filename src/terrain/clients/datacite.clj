(ns terrain.clients.datacite
  (:require [clojure.data.xml :as xml]))

(def ^:private resource-xsi "http://www.w3.org/2001/XMLSchema-instance")
(def ^:private resource-xmlns "http://datacite.org/schema/kernel-4")
(def ^:private resource-schema-location
  "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd")

(defn- merge-xml-attrs
  [xml-attrs {:keys [attr value]}]
  (if (empty? value)
    xml-attrs
    (assoc xml-attrs (keyword attr) value)))

(defn- avu->xml-element
  [{:keys [attr value avus]}]
  (when-not (empty? value)
    (xml/element attr (reduce merge-xml-attrs {} avus) value)))

(defn- avus->xml-element-list
  [avus]
  (map avu->xml-element avus))

(defn- xml-element-group
  [group-attr xml-element-list]
  (let [xml-element-list (remove nil? xml-element-list)]
    (when-not (empty? xml-element-list)
      (xml/element group-attr {} xml-element-list))))

(defn- creator->xml-element
  [{:keys [value avus]}]
  (when-not (empty? value)
    (xml/element :creator
                 (reduce merge-xml-attrs {} (filter #(= "nameType" (:attr %)) avus))
                 (xml/element :creatorName {} value)
                 (avus->xml-element-list (remove #(= "nameType" (:attr %)) avus)))))

(defn- contributor->xml-element
  [{:keys [value avus]}]
  (when-not (empty? value)
    (xml/element :contributor
                 (reduce merge-xml-attrs {} (filter #(= "contributorType" (:attr %)) avus))
                 (xml/element :contributorName {} value)
                 (avus->xml-element-list (remove #(= "contributorType" (:attr %)) avus)))))

(defn- fundingRef->xml-element
  [{:keys [avus]}]
  (xml/element :fundingReference {}
               (avus->xml-element-list avus)))

(defn- geo-location->xml-element
  [{:keys [avus]}]
  (let [geo-places (filter (comp #{"geoLocationPlace"} :attr) avus)
        geo-points (filter (comp #{"pointLatitude" "pointLongitude"} :attr) avus)
        geo-bounds (filter (comp #{"northBoundLatitude"
                                   "southBoundLatitude"
                                   "eastBoundLongitude"
                                   "westBoundLongitude"} :attr) avus)]
    (xml/element :geoLocation {}
                 (avus->xml-element-list geo-places)
                 (xml-element-group :geoLocationPoint (avus->xml-element-list geo-points))
                 (xml-element-group :geoLocationBox   (avus->xml-element-list geo-bounds)))))

(defn- avus->xml-element
  [attr avus]
  (case attr
    :creator          (xml-element-group :creators          (map creator->xml-element avus))
    :contributor      (xml-element-group :contributors      (map contributor->xml-element avus))
    :geoLocation      (xml-element-group :geoLocations      (map geo-location->xml-element avus))
    :fundingReference (xml-element-group :fundingReferences (map fundingRef->xml-element avus))

    :title               (xml-element-group :titles               (avus->xml-element-list avus))
    :subject             (xml-element-group :subjects             (avus->xml-element-list avus))
    :description         (xml-element-group :descriptions         (avus->xml-element-list avus))
    :alternateIdentifier (xml-element-group :alternateIdentifiers (avus->xml-element-list avus))
    :relatedIdentifier   (xml-element-group :relatedIdentifiers   (avus->xml-element-list avus))
    :rights              (xml-element-group :rightsList           (avus->xml-element-list avus))
    :size                (xml-element-group :sizes                (avus->xml-element-list avus))
    :format              (xml-element-group :formats              (avus->xml-element-list avus))

    :identifier      (avus->xml-element-list avus)
    :publisher       (avus->xml-element-list avus)
    :publicationYear (avus->xml-element-list avus)
    :resourceType    (avus->xml-element-list avus)
    :language        (avus->xml-element-list avus)
    :version         (avus->xml-element-list avus)

    nil))

(defn- append-metadata-group
  [xml-element-list [attr avus]]
  (conj xml-element-list (avus->xml-element attr avus)))

(defn avus->datacite-xml
  [avus]
  (let [metadata (group-by (comp keyword :attr) avus)]
    (xml/emit-str
      (xml/element :resource {:xmlns:xsi          resource-xsi
                              :xmlns              resource-xmlns
                              :xsi:schemaLocation resource-schema-location}
                   (reduce append-metadata-group [] metadata)))))
