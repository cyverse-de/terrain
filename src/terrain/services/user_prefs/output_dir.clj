(ns terrain.services.user-prefs.output-dir
  (:use [terrain.auth.user-attributes :only [current-user]]
        [terrain.routes.schemas.user-prefs :only [default-output-dir-key]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [terrain.clients.data-info :as di]
            [terrain.util.config :as cfg]))

(defn- convert-default-output-dir-keys
  [prefs]
  (clojure.set/rename-keys prefs {:defaultOutputFolder    default-output-dir-key
                                  :systemDefaultOutputDir :system_default_output_dir}))

(defn add-default-output-dir
  "Adds the default output directory to a set of user preferences."
  [prefs path]
  (assoc prefs default-output-dir-key {:id path :path path}))

(defn extract-default-output-dir
  "Gets the default output directory from a set of user preferences."
  [prefs]
  (if (map? (default-output-dir-key prefs))
    (:path (default-output-dir-key prefs))
    (default-output-dir-key prefs)))

(defn- system-default-output-dir
  []
  (di/build-path
   (di/user-home-folder (:shortUsername current-user))
   (cfg/default-output-dir)))

(defn- generate-default-output-dir
  "Generates a default output directory for the user and stores it in the preferences."
  [user prefs]
  (add-default-output-dir prefs (di/gen-output-dir (system-default-output-dir))))

(defn- add-system-default-output-dir
  "Adds system default output directory to the preferences that are passed in."
  [prefs]
  (assoc prefs
    :system_default_output_dir {:id   (system-default-output-dir)
                                :path (system-default-output-dir)}))

(defn- sysdefoutdir
  [prefs]
  (let [out-dir (:system_default_output_dir prefs)]
    (if (map? out-dir)
      (:path out-dir)
      out-dir)))

(defn- create-default-output-dir
  "Creates the default output directory if it doesn't exist already. If the user has selected a
   default output directory then this function ensures that it exists. Otherwise, this function
   ensures that the system default output directory exists."
  [prefs]
  (let [sys-output-dir (ft/rm-last-slash (sysdefoutdir prefs))
        output-dir     (ft/rm-last-slash (extract-default-output-dir prefs))
        user           (:shortUsername current-user)]
    (log/debug "sys-output-dir" sys-output-dir)
    (log/debug "output-dir" output-dir)
    (cond
      (not (string/blank? output-dir))     (di/ensure-dir-created user output-dir)
      (not (string/blank? sys-output-dir)) (di/ensure-dir-created user sys-output-dir)
      :else                                (log/warn "Not creating default output directory for"
                                                     user))
    prefs))

(defn handle-blank-default-output-dir
  [user prefs]
  (let [output-dir (extract-default-output-dir prefs)]
    (if (string/blank? output-dir)
      (generate-default-output-dir user prefs)
      prefs)))

(defn handle-string-default-output-dir
  [prefs]
  (let [output-dir (default-output-dir-key prefs)]
    (if-not (map? output-dir)
      (assoc prefs default-output-dir-key {:id output-dir :path output-dir})
      prefs)))

(defn validate-selected-output-dir
  [prefs]
  (let [user                (:shortUsername current-user)
        user-default        (default-output-dir-key prefs)
        sys-default         (:system_default_output_dir prefs)
        restore-sys-default #(assoc prefs default-output-dir-key sys-default)]
    (cond (= user-default sys-default)                   prefs
          (di/can-create-dir? user (:path user-default)) prefs
          :else                                          (restore-sys-default))))

(defn process-outgoing
  [user prefs]
  (->> prefs
       (convert-default-output-dir-keys)
       (handle-blank-default-output-dir user)
       (handle-string-default-output-dir)
       (add-system-default-output-dir)
       (validate-selected-output-dir)
       (create-default-output-dir)))

(defn process-incoming
  [user prefs]
  (->> prefs
       (add-system-default-output-dir)
       (create-default-output-dir)))
