(ns terrain.routes.schemas.token
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema]]))

(defschema AccessTokenResponse
  {:access_token (describe String "The access token.")
   :expires_in   (describe String "The lifetime of the token in seconds")})
