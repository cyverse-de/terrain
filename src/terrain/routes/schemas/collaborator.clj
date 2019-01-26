(ns terrain.routes.schemas.collaborator
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema optional-key]])
  (:require [common-swagger-api.schema.groups :as group-schema]))

(def CollaboratorListNamePathParam (describe String "The name of the collaborator list"))

(defschema CollaboratorListSearchParams
  {(optional-key :search)
   (describe String "The collaborator list name substring to search for")})

(defschema CollaboratorListRetainPermissionsParams
  {(optional-key :retain-permissions)
   (describe Boolean "Select `true` if removed users should retain permission to shared resources")})

(defschema GetCollaboratorListsResponse (group-schema/group-list "collaborator list" "collaborator lists"))
(defschema AddCollaboratorListRequest (group-schema/base-group "collaborator list"))
(defschema CollaboratorList (group-schema/group "collaborator list"))
(defschema CollaboratorListUpdate
  (select-keys (group-schema/group-update "collaborator list") (map optional-key [:name :description])))
(defschema CollaboratorListStub (group-schema/group-stub "collaborator list"))
