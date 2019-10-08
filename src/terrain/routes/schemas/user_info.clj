(ns terrain.routes.schemas.user-info
  (:use [common-swagger-api.schema :only [describe
                                          NonBlankString]])
  (:require [common-swagger-api.schema.groups :as groups]
            [common-swagger-api.schema.subjects :as subjects]
            [schema.core :as s]))

(def UsernameParam (describe NonBlankString "The user's ID"))
(s/defschema DetailsParam
  {groups/GroupDetailsParamKey
   (groups/GroupDetailsParamDesc "group")})

(s/defschema UserInfoRequest
  {:username (describe [s/Str] "A list containing user IDs")})

(s/defschema UserInfoResponse
  {s/Str subjects/Subject})

(s/defschema UserInfoResponseDocs
  {:username subjects/Subject})

(s/defschema GroupListing (groups/group-list-with-detail "group" "groups"))
