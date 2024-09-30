(ns terrain.routes.collaborator
  (:require [common-swagger-api.schema :refer [context GET POST PATCH DELETE]]
            [common-swagger-api.schema.groups :as group-schema]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.routes.schemas.collaborator :as collaborator-schema]
            [terrain.services.collaborator-lists :as cl]
            [terrain.services.communities :as communities]
            [terrain.services.subjects :as subjects]
            [terrain.services.teams :as teams]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare params body message requester)

(defn collaborator-list-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]
   (context "/collaborator-lists" []
     :tags ["collaborator-lists"]

     (GET "/" []
       :summary "Get Collaborator Lists"
       :query [params collaborator-schema/CollaboratorListSearchParams]
       :return collaborator-schema/GetCollaboratorListsResponse
       :description "Get or search for collaborator lists."
       (ok (cl/get-collaborator-lists current-user params)))

     (POST "/" []
       :summary "Add a Collaborator List"
       :body [body collaborator-schema/AddCollaboratorListRequest]
       :return collaborator-schema/CollaboratorList
       :description "Add a new collaborator list to the Discovery Environment."
       (ok (cl/add-collaborator-list current-user body)))

     (context "/:name" []
       :path-params [name :- collaborator-schema/CollaboratorListNamePathParam]

       (GET "/" []
         :summary "Get a Collaborator List"
         :return collaborator-schema/CollaboratorList
         :description "Get information about a single collaborator list."
         (ok (cl/get-collaborator-list current-user name)))

       (PATCH "/" []
         :summary "Update a Collaborator List"
         :body [body collaborator-schema/CollaboratorListUpdate]
         :return collaborator-schema/CollaboratorList
         :description "Update the name or description of a collaborator list."
         (ok (cl/update-collaborator-list current-user name body)))

       (DELETE "/" []
         :summary "Delete a Collaborator List"
         :query [params collaborator-schema/CollaboratorListRetainPermissionsParams]
         :return collaborator-schema/CollaboratorListStub
         :description (str "Delete a collaborator list, optionally giving former list members direct access to "
                           "resources shared with the list.")
         (ok (cl/delete-collaborator-list current-user name params)))

       (context "/members" []
         (GET "/" []
           :summary "Get Collaborator List Members"
           :return collaborator-schema/CollaboratorListMembers
           :description "Obtain a listing of the members of a collaborator list."
           (ok (cl/get-collaborator-list-members current-user name)))

         (POST "/" []
           :summary "Add Collaborator List Members"
           :body [body group-schema/GroupMembersUpdate]
           :return group-schema/GroupMembersUpdateResponse
           :description "Add one or more users to a collaborator list."
           (ok (cl/add-collaborator-list-members current-user name body)))

         (POST "/deleter" []
           :summary "Remove Collaborator List Members"
           :query [params collaborator-schema/CollaboratorListRetainPermissionsParams]
           :body [body group-schema/GroupMembersUpdate]
           :return group-schema/GroupMembersUpdateResponse
           :description (str "Remove members from a collaborator list, optionally giving former list members direct "
                             "access to resources shared with the list.")
           (ok (cl/remove-collaborator-list-members current-user name body params))))))))

(defn team-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]
   (context "/teams" []
     :tags ["teams"]

     (GET "/" []
       :summary "List Teams"
       :query [params collaborator-schema/TeamListingParams]
       :return collaborator-schema/TeamListing
       :description "List or search for teams."
       (ok (teams/get-teams current-user params)))

     (POST "/" []
       :summary "Add a Team"
       :body [body collaborator-schema/AddTeamRequest]
       :return collaborator-schema/Team
       :description "Add a new team to the Discovery Environment."
       (ok (teams/add-team current-user body)))

     (context "/:name" []
       :path-params [name :- collaborator-schema/TeamNamePathParam]

       (GET "/" []
         :summary "Get a Team"
         :return collaborator-schema/Team
         :description "Get information about a single team."
         (ok (teams/get-team current-user name)))

       (PATCH "/" []
         :summary "Update a Team"
         :body [body collaborator-schema/UpdateTeamRequest]
         :return collaborator-schema/Team
         :description "Update the name or description of a team."
         (ok (teams/update-team current-user name body)))

       (DELETE "/" []
         :summary "Delete a Team"
         :return collaborator-schema/TeamStub
         :description "Delete a team."
         (ok (teams/delete-team current-user name)))

       (context "/members" []
         (GET "/" []
           :summary "Get Team Members"
           :return collaborator-schema/TeamMembers
           :description "Obtain a listing of the members of a team."
           (ok (teams/get-team-members current-user name)))

         (POST "/" []
           :summary "Add Team Members"
           :body [body group-schema/GroupMembersUpdate]
           :return group-schema/GroupMembersUpdateResponse
           :description "Add one or more users to a team."
           (ok (teams/add-team-members current-user name body)))

         (POST "/deleter" []
           :summary "Remove Team Members"
           :body [body group-schema/GroupMembersUpdate]
           :return group-schema/GroupMembersUpdateResponse
           :description "Remove one or more users from a team."
           (ok (teams/remove-team-members current-user name body))))

       (context "/privileges" []
         (GET "/" []
           :summary "List Team Privileges"
           :return group-schema/Privileges
           :description (str "Privileges describe what a user or set of users is allowed to do with a team. For "
                             "example, some users may be able to administer the team whereas others may only be "
                             "able to view it. This endpoint lists the current privileges for a team.")
           (ok (teams/list-team-privileges current-user name)))

         (POST "/" []
           :summary "Update Team Privileges"
           :body [body group-schema/GroupPrivilegeUpdates]
           :return group-schema/Privileges
           :description (str "Privileges describe what a user or set of users is allowed to do with a team. For "
                             "example, some users may be able to administer the team whereas others may only be "
                             "able to view it. This endpoint assigns privleges to users.")
           (ok (teams/update-team-privileges current-user name body))))

       (POST "/join" []
         :summary "Join a Team"
         :return group-schema/GroupMembersUpdateResponse
         :description (str "Adds the authenticated user to a team, provided that he or she has permission to "
                           "join the team.")
         (ok (teams/join current-user name)))

       (context "/join-request" []
         (POST "/" []
           :summary "Request to Join a Team"
           :body [{:keys [message]} collaborator-schema/TeamJoinRequest]
           :description (str "Allows the authenticated user to request to be added to a team. The request "
                             "is sent to the administrators of the team. The team administrator may then "
                             "add the user to the team using the the POST /team/{name}/members endpoint "
                             "or deny the request using the POST /team/{name}/join-request/{requester}/deny "
                             "endpoint.")
           (teams/join-request current-user name message)
           (ok))

         (POST "/:requester/deny" []
           :summary "Deny a Request to Join a Team"
           :path-params [requester :- collaborator-schema/TeamRequesterPathParam]
           :body [{:keys [message]} collaborator-schema/TeamJoinDenial]
           :description (str "Allows a team administrator to deny a request for a user to be added to a team.")
           (teams/deny-join-request current-user name requester message)
           (ok)))

       (POST "/leave" []
         :summary "Leave a team"
         :return group-schema/GroupMembersUpdateResponse
         :description "Allows the authenticated user to leave a team."
         (ok (teams/leave current-user name)))))))

(defn community-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (context "/communities" []
     :tags ["communities"]

     (GET "/" []
       :summary "List Communities"
       :query [params collaborator-schema/CommunityListingParams]
       :return collaborator-schema/CommunityListing
       :description "List or search for communities."
       (ok (communities/get-communities current-user params)))

     (POST "/" []
       :summary "Add a Community"
       :body [body collaborator-schema/AddCommunityRequest]
       :return collaborator-schema/Community
       :description "Adds a community to the Discovery Environment."
       (ok (communities/add-community current-user body)))

     (context "/:name" []
       :path-params [name :- collaborator-schema/CommunityNamePathParam]

       (GET "/" [name]
         :summary "Get Community Information"
         :return collaborator-schema/Community
         :description "Returns information about the community with the given name."
         (ok (communities/get-community current-user name)))

       (PATCH "/" []
         :summary "Update a Community"
         :query [params collaborator-schema/UpdateCommunityParams]
         :body [body collaborator-schema/UpdateCommunityRequest]
         :return collaborator-schema/Community
         :description "Updates the name or description of a community."
         (ok (communities/update-community current-user name params body)))

       (DELETE "/" []
         :summary "Delete a Community"
         :return collaborator-schema/CommunityStub
         :description "Removes a community from the Discoevery Environment."
         (ok (communities/delete-community current-user name)))

       (context "/admins" []
         (GET "/" []
           :summary "List Community Administrators"
           :return collaborator-schema/CommunityAdmins
           :description "Lists the administrators of a community in the Discovery Environment."
           (ok (communities/get-community-admins current-user name)))

         (POST "/" []
           :summary "Add Community Administrators"
           :body [body group-schema/GroupMembersUpdate]
           :return group-schema/GroupMembersUpdateResponse
           :description "Add one or more administrators to a community in the Discoevery Environment."
           (ok (communities/add-community-admins current-user name body)))

         (POST "/deleter" [name :as {:keys [body]}]
           :summary "Remove Community Administrators"
           :body [body group-schema/GroupMembersUpdate]
           :return group-schema/GroupMembersUpdateResponse
           :description "Remove one or more administrators from a community in the Discoevery Environment."
           (ok (communities/remove-community-admins current-user name body))))

       (GET "/members" []
         :summary "List Community Members"
         :return collaborator-schema/CommunityMembers
         :description "Lists the members of a community in the Discoevery Environment."
         (ok (communities/get-community-members current-user name)))

       (POST "/join" []
         :summary "Join a Community"
         :return group-schema/GroupMembersUpdateResponse
         :description "Allows the caller to join a community, provided that he or she has permission to do so."
         (ok (communities/join current-user name)))

       (POST "/leave" []
         :summary "Leave a Community"
         :return group-schema/GroupMembersUpdateResponse
         :description "Allows the caller to leave a community."
         (ok (communities/leave current-user name)))))))

(defn admin-community-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (context "/communities" []
     :tags ["admin-communities"]

     (GET "/" []
       :summary "List Communities"
       :query [params collaborator-schema/CommunityListingParams]
       :return collaborator-schema/AdminCommunityListing
       :description "List or search for communities."
       (ok (communities/admin-get-communities params)))

     (POST "/" []
       :summary "Add a Community"
       :body [body collaborator-schema/AddCommunityRequest]
       :return collaborator-schema/Community
       :description "Adds a community to the Discovery Environment."
       (ok (communities/add-community current-user body)))

     (context "/:name" []
       :path-params [name :- collaborator-schema/CommunityNamePathParam]

       (GET "/" []
         :summary "Get Community Information"
         :return collaborator-schema/Community
         :description "Returns information about the community with the given name."
         (ok (communities/admin-get-community name)))

       (PATCH "/" []
         :summary "Update a Community"
         :query [params collaborator-schema/UpdateCommunityParams]
         :body [body collaborator-schema/UpdateCommunityRequest]
         :return collaborator-schema/Community
         :description "Updates the name or description of a community."
         (ok (communities/admin-update-community name params body)))

       (DELETE "/" []
         :summary "Delete a Community"
         :return collaborator-schema/CommunityStub
         :description "Removes a community from the Discoevery Environment."
         (ok (communities/admin-delete-community name)))

       (context "/admins" []
         (GET "/" []
           :summary "List Community Administrators"
           :return collaborator-schema/CommunityAdmins
           :description "Lists the administrators of a community in the Discovery Environment."
           (ok (communities/admin-get-community-admins name)))

         (POST "/" []
           :summary "Add Community Administrators"
           :body [body group-schema/GroupMembersUpdate]
           :return group-schema/GroupMembersUpdateResponse
           :description "Add one or more administrators to a community in the Discoevery Environment."
           (ok (communities/admin-add-community-admins name body)))

         (POST "/deleter" []
           :summary "Remove Community Administrators"
           :body [body group-schema/GroupMembersUpdate]
           :return group-schema/GroupMembersUpdateResponse
           :description "Remove one or more administrators from a community in the Discoevery Environment."
           (ok (communities/admin-remove-community-admins name body))))))))

(defn subject-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (context "/subjects" []
     :tags ["subjects"]

     (GET "/" []
       :summary "Search for Users or Groups"
       :query [params collaborator-schema/SubjectSearchParams]
       :return collaborator-schema/SubjectList
       :description "Searches for users or groups matching a search string."
       (ok (subjects/find-subjects current-user params))))))
