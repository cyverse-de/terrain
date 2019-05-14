(ns terrain.routes.schemas.bootstrap
  (:use [common-swagger-api.schema :only [describe doc-only]]
        [schema.core :only [defschema]])
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

(defschema TerrainBootstrapResponse
  {:user_info   UserInfo
   :session     sessions-schema/LoginResponse
   :apps_info   apps-schema/AppsBootstrapResponse
   :data_info   navigation-schema/UserBasePaths
   :preferences (doc-only user-prefs-schema/UserPreferencesResponse
                          user-prefs-schema/UserPreferencesResponseDocs)})
