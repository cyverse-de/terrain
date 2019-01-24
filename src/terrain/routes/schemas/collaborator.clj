(ns terrain.routes.schemas.collaborator
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema optional-key]])
  (:require [common-swagger-api.schema.groups :as group-schema]))

(def CollaboratorListNamePathParam (describe String "The name of the collaborator list"))

(defschema CollaboratorListSearchParams
  {(optional-key :search)
   (describe String "The collaborator list name substring to search for")})

(defschema GetCollaboratorListsResponse (group-schema/group-list "collaborator list" "collaborator lists"))
