(ns terrain.routes.schemas.user-info
  (:use [common-swagger-api.schema :only [describe]])
  (:require [common-swagger-api.schema.subjects :as subjects]
            [schema.core :as s]))

(s/defschema UserInfoRequest
  {:username (describe [s/Str] "A list containing user IDs")})

(s/defschema UserInfoResponse
  {s/Str subjects/Subject})

(s/defschema UserInfoResponseDocs
  {:username subjects/Subject})