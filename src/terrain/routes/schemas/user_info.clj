(ns terrain.routes.schemas.user-info
  (:require
   [common-swagger-api.schema :refer [describe NonBlankString]]
   [common-swagger-api.schema.groups :as groups]
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

(s/defschema UserDetails
  {:username     (describe NonBlankString "The user's username.")
   :given_name   (describe NonBlankString "The user's given name.")
   :surname      (describe NonBlankString "The user's surname.")
   :common_name  (describe NonBlankString "The user's full name.")
   :email        (describe NonBlankString "The user's primary email address.")
   :organization (describe String "The name of the user's organization.")
   :department   (describe String "The name of the user's department.")})
