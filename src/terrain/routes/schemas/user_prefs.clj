(ns terrain.routes.schemas.user-prefs
  (:require [common-swagger-api.schema :refer [describe]]
            [common-swagger-api.schema.stats :refer [DataItemPathParam]]
            [schema.core :refer [defschema Any Keyword optional-key]]))

(def default-output-dir-key :default_output_folder)

(def AnyPreferenceValue (describe Any "Any preference value"))

(defschema DefaultOutputDirPreference
  (-> {:id     DataItemPathParam
       :path   DataItemPathParam
       Keyword Any}
      (describe "The user's default analyses output directory, required by this terrain service.")))

(defschema UserPreferencesResponse
  {default-output-dir-key                  DefaultOutputDirPreference
   (describe Keyword "Any preference key") AnyPreferenceValue})

(defschema UserPreferencesResponseDocs
  (-> UserPreferencesResponse
      (merge {(optional-key :any_preference_key) AnyPreferenceValue})
      (describe (str "The User Preferences map may contain any number of key/value pairs"
                     " that the client wishes to store and retrieve between sessions."))))
