(ns terrain.routes.schemas.token
  (:require [common-swagger-api.schema :refer [describe]]
            [schema.core :refer [defschema optional-key]]))

(defschema AccessTokenResponse
  {:access_token
   (describe String "The access token")

   :expires_in
   (describe Integer "The lifetime of the token in seconds")

   (optional-key :refresh_expires_in)
   (describe Integer "The lifetime of the refresh token in seconds")

   (optional-key :refresh_token)
   (describe String "The refresh token")

   (optional-key :token_type)
   (describe String "The type of the access token")

   (optional-key :not-before-policy)
   (describe Integer "The number of seconds before the token can be used")

   (optional-key :session_state)
   (describe String "A string representing the login state of the user")

   (optional-key :scope)
   (describe String "The scopes granted to the access token")})

(defschema AdminKeycloakTokenParams
  {:username (describe String "The username of the person to impersonate")})
