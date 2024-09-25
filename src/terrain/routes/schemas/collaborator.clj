(ns terrain.routes.schemas.collaborator
  (:require [common-swagger-api.schema :refer [describe NonBlankString]]
            [common-swagger-api.schema.groups :as group-schema]
            [common-swagger-api.schema.subjects :as subject-schema]
            [schema.core :refer [defschema optional-key]]))

(defn group-member [group-descriptor member-descriptor]
  (assoc subject-schema/Subject
    :display_name (describe NonBlankString (str "The displayable " group-descriptor " " member-descriptor " name"))))

(defn group-members [group-descriptor member-descriptor member-descriptor-plural]
  {:members (describe [(group-member group-descriptor member-descriptor)]
                      (str "The list of " group-descriptor " " member-descriptor-plural))})

;; Collaborator List Schemas

(def CollaboratorListNamePathParam (describe NonBlankString "The name of the collaborator list"))

(defschema CollaboratorListSearchParams
  {(optional-key :search)
   (describe NonBlankString "The collaborator list name substring to search for")

   group-schema/GroupDetailsParamKey
   (group-schema/GroupDetailsParamDesc "collaborator list")})

(defschema CollaboratorListRetainPermissionsParams
  {(optional-key :retain-permissions)
   (describe Boolean "Select `true` if removed users should retain permission to shared resources")})

(defschema GetCollaboratorListsResponse (group-schema/group-list-with-detail "collaborator list" "collaborator lists"))
(defschema AddCollaboratorListRequest
  (select-keys (group-schema/base-group "collaborator list") [:name (optional-key :description)]))
(defschema CollaboratorList (group-schema/group "collaborator list"))
(defschema CollaboratorListUpdate
  (select-keys (group-schema/group-update "collaborator list") (map optional-key [:name :description])))
(defschema CollaboratorListStub (group-schema/group-stub "collaborator list"))
(defschema CollaboratorListMembers (group-members "collaborator list" "member" "members"))

;; Team Schemas

(def TeamNamePathParam
  (describe NonBlankString "The name of the team, including the username prefix (e.g. `username:team-name`)"))

(def TeamRequesterPathParam
  (describe NonBlankString "The username of the person requesting to join the team"))

(defn team-listing-params [descriptor plural-descriptor]
  {(optional-key :search)
   (describe NonBlankString (str "The " descriptor " name substring to search for"))

   (optional-key :creator)
   (describe NonBlankString (str "Only " plural-descriptor " created by the user with this username will be listed if "
                                 "specified"))

   group-schema/GroupDetailsParamKey
   (group-schema/GroupDetailsParamDesc descriptor)

   (optional-key :member)
   (describe NonBlankString (str "Only " plural-descriptor " to which the user with this username belongs will be "
                                 "listed if specified"))})

(defschema TeamListingParams (team-listing-params "team" "teams"))

(defschema TeamJoinRequest
  {:message (describe String "A brief message to send to the team administrators")})

(defschema TeamJoinDenial
  {:message (describe String "A brief message to send to the person requesting to join the team")})

(defschema TeamListing (group-schema/group-list-with-detail "team" "teams"))
(defschema AddTeamRequest
  (assoc (select-keys (group-schema/base-group "team") [:name (optional-key :description)])
    (optional-key :public_privileges)
    (describe [group-schema/ValidGroupPrivileges] "Team privileges granted to all DE users")))
(defschema Team (group-schema/group "team"))
(defschema UpdateTeamRequest (select-keys (group-schema/group-update "team") (map optional-key [:name :description])))
(defschema TeamStub (group-schema/group-stub "team"))
(defschema TeamMembers (group-members "team" "member" "members"))

;; Community Schemas

(def CommunityNamePathParam (describe NonBlankString "The name of the community"))

(defschema CommunityListingParams
  (dissoc (team-listing-params "community" "communities") (optional-key :creator)))

(defschema CommunityListingEntry
  (assoc (group-schema/group-with-detail "community")
    :member     (describe Boolean "True if the authenticated user belongs to the community")
    :privileges (describe [NonBlankString] "The privileges the authenticated user has for the community")))

(defschema CommunityListing
  {:groups (describe [CommunityListingEntry] "The list of communities in the result set")})

(defschema Community (group-schema/group "community"))

(defschema AddCommunityRequest
  (select-keys (group-schema/base-group "community") [:name (optional-key :description)]))

(defschema UpdateCommunityRequest
  (select-keys (group-schema/group-update "community") (map optional-key [:name :description])))

(defschema UpdateCommunityParams
  {(optional-key :retag-apps)
   (describe Boolean "Set to `true` to cause apps that are associated with a renamed community to be retagged")

   (optional-key :force-rename)
   (describe Boolean "Set to `true` to force the community to be renamed even if apps are associated with it")})

(defschema CommunityStub (group-schema/group-stub "community"))

(defschema CommunityAdmins
  {:members (describe [subject-schema/Subject] "The list of community administrators")})

(defschema CommunityMembers (group-members "community" "member" "members"))

;; Admin Community Schemas

(defschema AdminCommunityListing (group-schema/group-list-with-detail "community" "communities"))

;; Subject Schemas

(defschema SubjectSearchParams
  {:search (describe NonBlankString "A substring to search for in the subject information")})

(defschema SubjectListEntry
  (assoc subject-schema/Subject
    :display_name (describe NonBlankString "The displayable subject name")))

(defschema SubjectList
  {:subjects (describe [SubjectListEntry] "The list of subjects in the result set")})
