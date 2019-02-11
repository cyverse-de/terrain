(ns terrain.routes.collaborator
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.routes.schemas.collaborator]
        [terrain.util :only [optional-routes]])
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [common-swagger-api.schema.groups :as group-schema]
            [terrain.clients.apps.raw :as apps]
            [terrain.services.collaborator-lists :as cl]
            [terrain.services.communities :as communities]
            [terrain.services.subjects :as subjects]
            [terrain.services.teams :as teams]
            [terrain.util.config :as config]
            [terrain.util.service :as service]))

(defn collaborator-list-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]
   (context "/collaborator-lists" []
     :tags ["collaborator-lists"]

     (GET "/" []
       :summary "Get Collaborator Lists"
       :query [params CollaboratorListSearchParams]
       :return GetCollaboratorListsResponse
       :description "Get or search for collaborator lists."
       (ok (cl/get-collaborator-lists current-user params)))

     (POST "/" []
       :summary "Add a Collaborator List"
       :body [body AddCollaboratorListRequest]
       :return CollaboratorList
       :description "Add a new collaborator list to the Discovery Environment."
       (ok (cl/add-collaborator-list current-user body)))

     (context "/:name" []
       :path-params [name :- CollaboratorListNamePathParam]

       (GET "/" []
         :summary "Get a Collaborator List"
         :return CollaboratorList
         :description "Get information about a single collaborator list."
         (ok (cl/get-collaborator-list current-user name)))

       (PATCH "/" []
         :summary "Update a Collaborator List"
         :body [body CollaboratorListUpdate]
         :return CollaboratorList
         :description "Update the name or description of a collaborator list."
         (ok (cl/update-collaborator-list current-user name body)))

       (DELETE "/" []
         :summary "Delete a Collaborator List"
         :query [params CollaboratorListRetainPermissionsParams]
         :return CollaboratorListStub
         :description (str "Delete a collaborator list, optionally giving former list members direct access to "
                           "resources shared with the list.")
         (ok (cl/delete-collaborator-list current-user name params)))

       (context "/members" []
         (GET "/" []
           :summary "Get Collaborator List Members"
           :return CollaboratorListMembers
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
           :query [params CollaboratorListRetainPermissionsParams]
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
       :query [params TeamListingParams]
       :return TeamListing
       :description "List or search for teams."
       (ok (teams/get-teams current-user params)))

     (POST "/" []
       :summary "Add a Team"
       :body [body AddTeamRequest]
       :return Team
       :description "Add a new team to the Discovery Environment."
       (ok (teams/add-team current-user body)))

     (context "/:name" []
       :path-params [name :- TeamNamePathParam]

       (GET "/" []
         :summary "Get a Team"
         :return Team
         :description "Get information about a single team."
         (ok (teams/get-team current-user name)))

       (PATCH "/" []
         :summary "Update a Team"
         :body [body UpdateTeamRequest]
         :return Team
         :description "Update the name or description of a team."
         (ok (teams/update-team current-user name body)))

       (DELETE "/" []
         :summary "Delete a Team"
         :return TeamStub
         :description "Delete a team."
         (ok (teams/delete-team current-user name)))

       (context "/members" []
         (GET "/" []
           :summary "Get Team Members"
           :return TeamMembers
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
           :body [{:keys [message]} TeamJoinRequest]
           :description (str "Allows the authenticated user to request to be added to a team. The request "
                             "is sent to the administrators of the team. The team administrator may then "
                             "add the user to the team using the the POST /team/{name}/members endpoint "
                             "or deny the request using the POST /team/{name}/join-request/{requester}/deny "
                             "endpoint.")
           (teams/join-request current-user name message)
           (ok))

         (POST "/:requester/deny" []
           :summary "Deny a Request to Join a Team"
           :path-params [requester :- TeamRequesterPathParam]
           :body [{:keys [message]} TeamJoinDenial]
           :description (str "Allows a team administrator to deny a request for a user to be added to a team.")
           (teams/deny-join-request current-user name requester message)
           (ok)))

       (POST "/leave" []
         :summary "Leave a team"
         :return group-schema/GroupMembersUpdateResponse
         :description "Allows the authenticated user to leave a team."
         (ok (teams/leave current-user name)))))))

(defn community-routes
  ([]
   optional-routes
   [config/collaborator-routes-enabled]

   (context "/communities" []
     :tags ["communities"]

     (GET "/" []
       :summary "List Communities"
       :query [params CommunityListingParams]
       :return CommunityListing
       :description "List or search for communities."
       (ok (communities/get-communities current-user params)))

     (POST "/" []
       :summary "Add a Community"
       :body [body AddCommunityRequest]
       :return Community
       :description "Adds a community to the Discovery Environment."
       (ok (communities/add-community current-user body)))

     (context "/:name" []
       :path-params [name :- CommunityNamePathParam]

       (GET "/" [name]
         :summary "Get Community Information"
         :return Community
         :description "Returns information about the community with the given name."
         (ok (communities/get-community current-user name)))

       (PATCH "/" []
         :summary "Update a Community"
         :query [params UpdateCommunityParams]
         :body [body UpdateCommunityRequest]
         :return Community
         :description "Updates the name or description of a community."
         (ok (communities/update-community current-user name params body)))

       (DELETE "/" []
         :summary "Delete a Community"
         :return CommunityStub
         :description "Removes a community from the Discoevery Environment."
         (ok (communities/delete-community current-user name)))

       (context "/admins" []
         (GET "/" []
           :summary "List Community Administrators"
           :return CommunityAdmins
           :description "Lists the administrators of community in the Discovery Environment."
           (ok (communities/get-community-admins current-user name)))

         (POST "/" [name :as {:keys [body]}]
           (service/success-response (communities/add-community-admins current-user name (service/decode-json body))))

         (POST "/deleter" [name :as {:keys [body]}]
           (service/success-response (communities/remove-community-admins current-user name (service/decode-json body)))))

       (GET "/members" [name]
         (service/success-response (communities/get-community-members current-user name)))

       (POST "/join" [name]
         (service/success-response (communities/join current-user name)))

       (POST "/leave" [name]
         (service/success-response (communities/leave current-user name)))))))

(defn admin-community-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (GET "/communities" [:as {:keys [params]}]
     (service/success-response (communities/admin-get-communities params)))

   (POST "/communities" [:as {:keys [body]}]
     (service/success-response (communities/add-community current-user (service/decode-json body))))

   (GET "/communities/:name" [name]
     (service/success-response (communities/admin-get-community name)))

   (PATCH "/communities/:name" [name :as {:keys [params body]}]
     (service/success-response (communities/admin-update-community name params (service/decode-json body))))

   (DELETE "/communities/:name" [name]
     (service/success-response (communities/admin-delete-community name)))

   (GET "/communities/:name/admins" [name]
     (service/success-response (communities/admin-get-community-admins name)))

   (POST "/communities/:name/admins" [name :as {:keys [body]}]
     (service/success-response (communities/admin-add-community-admins name (service/decode-json body))))

   (POST "/communities/:name/admins/deleter" [name :as {:keys [body]}]
     (service/success-response (communities/admin-remove-community-admins name (service/decode-json body))))))

(defn subject-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (GET "/subjects" [:as {:keys [params]}]
     (service/success-response (subjects/find-subjects current-user params)))))

(defn secured-collaborator-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (GET "/collaborators" []
     (service/success-response (apps/get-collaborators)))

   (POST "/collaborators" [:as {:keys [body]}]
     (service/success-response (apps/add-collaborators body)))

   (POST "/remove-collaborators" [:as {:keys [body]}]
     (service/success-response (apps/remove-collaborators body)))))
