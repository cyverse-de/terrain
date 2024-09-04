(ns terrain.routes.schemas.bootstrap
  (:use [common-swagger-api.schema :only [describe doc-only]]
        [schema.core
         :only [conditional
                defschema
                optional-key
                Any
                Int]])
  (:require [common-swagger-api.schema.apps.bootstrap :as apps-schema]
            [common-swagger-api.schema.data.navigation :as navigation-schema]
            [common-swagger-api.schema.sessions :as sessions-schema]
            [terrain.routes.schemas.user-prefs :as user-prefs-schema]))

(defschema UserInfo
  (-> {:username      (describe String "The authenticated user's short username")
       :full_username (describe String "The authenticated user's fully qualified username")
       :email         (describe String "The authenticated user's email")
       :first_name    (describe String "The authenticated user's first name")
       :last_name     (describe String "The authenticated user's last name")}
      (describe "User attributes obtained during the authentication process.")))

(defschema BootstrapServiceError
  {(optional-key :status) (describe Int "Status Code")
   :error                 (describe Any "Error Message")})

(defschema AppsBootstrapResponse
  (doc-only (conditional :error BootstrapServiceError
                         :else apps-schema/AppsBootstrapResponse)
            apps-schema/AppsBootstrapResponse))

(defschema DataInfoResponse
  (doc-only (conditional :error BootstrapServiceError
                         :else navigation-schema/UserBasePaths)
            navigation-schema/UserBasePaths))

(defschema UserPreferencesResponse
  (doc-only (conditional :error BootstrapServiceError
                         :else user-prefs-schema/UserPreferencesResponse)
            user-prefs-schema/UserPreferencesResponseDocs))

(defschema UserSessionResponse
  (doc-only (conditional :error BootstrapServiceError
                         :else sessions-schema/LoginResponse)
            sessions-schema/LoginResponse))

(defschema TerrainBootstrapResponse
  {:user_info   UserInfo
   :session     UserSessionResponse
   :apps_info   AppsBootstrapResponse
   :data_info   DataInfoResponse
   :preferences UserPreferencesResponse})
