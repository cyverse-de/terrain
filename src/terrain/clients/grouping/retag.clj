(ns terrain.clients.grouping.retag
  "App-tag retagging for community renames on the Groups backend. Kept separate from
   terrain.clients.groups because it needs the apps and metadata clients, which (via
   terrain.util.transformers) transitively require terrain.auth.user-attributes and would
   otherwise create a dependency cycle with the subject-lookup facade.

   NOTE: app/community tags are AVUs whose value is the qualified community group name. Tags
   created under the legacy Grouper backend use its name format, so retagging on the Groups
   backend only matches tags created with the `de:communities:<name>` format. Migrating
   existing tag values is a separate data concern."
  (:require [clojure-commons.exception-util :as cxu]
            [terrain.clients.apps.raw :as apps-client]
            [terrain.clients.metadata.raw :as metadata-client]
            [terrain.util.config :as config]))

(def ^:private community-folder "de:communities")

(defn- qualified-name
  [name]
  (str community-folder ":" name))

(defn- retag-avu
  [new-group-name avu]
  (-> avu
      (select-keys [:id :attr :unit :avus])
      (assoc :value new-group-name)))

(defn- retag-apps
  [new-group-name app-tag-avus]
  (let [app-id->avus (group-by :target_id app-tag-avus)]
    (doseq [app-id (keys app-id->avus)]
      (->> (get app-id->avus app-id)
           (map (partial retag-avu new-group-name))
           (hash-map :avus)
           (metadata-client/update-avus metadata-client/target-type-app app-id)))))

(defn check-for-tagged-apps
  "When a community is being renamed, either retag apps tagged with the old community name or
   refuse the rename if any exist (unless forced). Takes external (short) community names."
  [retag-apps? force-rename? old-name new-name]
  (let [group     (qualified-name old-name)
        new-group (qualified-name new-name)]
    (when (or retag-apps? (not force-rename?))
      (when-let [app-tag-avus (->> (metadata-client/find-avus metadata-client/target-type-app
                                                              (config/communities-metadata-attr)
                                                              group)
                                   :avus
                                   seq)]
        (if retag-apps?
          (retag-apps new-group app-tag-avus)
          (cxu/exists "Some apps have been tagged with the old community name"
                      :name group
                      :apps (:body (apps-client/admin-get-apps-in-community group :as :json))))))))
