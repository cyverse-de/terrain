(ns terrain.services.filesystem.root
  (:use [clojure-commons.validators])
  (:require [clojure-commons.json :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [terrain.clients.data-info.raw :as data-raw]))

(defn- format-roots
  [roots]
  (letfn [(format-subdir [root] (assoc root :hasSubDirs true))
          (update-subdirs [root-list] (map format-subdir root-list))]
    (update-in roots [:roots] update-subdirs)))

(defn do-root-listing
  [{user :user}]
  (-> (data-raw/list-roots (or user "anonymous"))
      :body
      (json/string->json true)
      format-roots))
