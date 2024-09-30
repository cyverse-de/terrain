(ns terrain.services.filesystem.validators
  (:require [clj-jargon.item-info :refer [exists?]]
            [clj-jargon.permissions :refer [is-writeable? owns?]]
            [clj-jargon.users :refer [user-exists?]]
            [clojure-commons.error-codes :as ce]
            [slingshot.slingshot :refer [throw+]]
            [terrain.services.filesystem.common-paths :as cp]))

(defn not-superuser
  [user]
  (when (cp/super-user? user)
    (throw+ {:type :clojure-commons.exception/not-authorized
             :user user})))

(defn user-exists
  [cm user]
  (when-not (user-exists? cm user)
    (throw+ {:error_code ce/ERR_NOT_A_USER
             :user user})))

(defn all-users-exist
  [cm users]
  (when-not (every? #(user-exists? cm %) users)
    (throw+ {:error_code ce/ERR_NOT_A_USER
             :users (filterv #(not (user-exists? cm %1)) users)})))

(defn path-exists
  [cm path]
  (when-not (exists? cm path)
    (throw+ {:error_code ce/ERR_DOES_NOT_EXIST
             :path path})))

(defn all-paths-exist
  [cm paths]
  (when-not (every? #(exists? cm %) paths)
    (throw+ {:error_code ce/ERR_DOES_NOT_EXIST
             :paths (filterv #(not (exists? cm  %1)) paths)})))

(defn path-writeable
  [cm user path]
  (when-not (is-writeable? cm user path)
    (throw+ {:error_code ce/ERR_NOT_WRITEABLE
             :path path})))

(defn path-not-exists
  [cm path]
  (when (exists? cm path)
    (throw+ {:path path
             :error_code ce/ERR_EXISTS})))

(defn ownage?
  [cm user path]
  (owns? cm user path))

(defn user-owns-paths
  [cm user paths]
  (let [belongs-to? (partial ownage? cm user)]
    (when-not (every? belongs-to? paths)
      (throw+ {:error_code ce/ERR_NOT_OWNER
               :user user
               :paths (filterv #(not (belongs-to? %)) paths)}))))
