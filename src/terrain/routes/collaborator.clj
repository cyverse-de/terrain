(ns terrain.routes.collaborator
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.routes.schemas.collaborator]
        [terrain.util :only [optional-routes]])
  (:require [cheshire.core :as json]
            [clojure.string :as string]
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

     (POST "/" [:as {:keys [body]}]
       (service/success-response (cl/add-collaborator-list current-user (service/decode-json body))))

     (context "/:name" []
       [:path-params [name :- CollaboratorListNamePathParam]]

       (GET "/" []
         (service/success-response (cl/get-collaborator-list current-user name)))

       (PATCH "/" [:as {:keys [body]}]
         (service/success-response (cl/update-collaborator-list current-user name (service/decode-json body))))

       (DELETE "/" [:as {:keys [params]}]
         (service/success-response (cl/delete-collaborator-list current-user name params)))

       (GET "/members" []
         (service/success-response (cl/get-collaborator-list-members current-user name)))

       (POST "/members" [:as {:keys [body]}]
         (service/success-response (cl/add-collaborator-list-members current-user name (service/decode-json body))))

       (POST "/members/deleter" [:as {:keys [body params]}]
         (service/success-response
          (cl/remove-collaborator-list-members current-user name (service/decode-json body) params)))))))

(defn team-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (GET "/teams" [:as {:keys [params]}]
     (service/success-response (teams/get-teams current-user params)))

   (POST "/teams" [:as {:keys [body]}]
     (service/success-response (teams/add-team current-user (service/decode-json body))))

   (GET "/teams/:name" [name]
     (service/success-response (teams/get-team current-user name)))

   (PATCH "/teams/:name" [name :as {:keys [body]}]
     (service/success-response (teams/update-team current-user name (service/decode-json body))))

   (DELETE "/teams/:name" [name]
     (service/success-response (teams/delete-team current-user name)))

   (GET "/teams/:name/members" [name]
     (service/success-response (teams/get-team-members current-user name)))

   (POST "/teams/:name/members" [name :as {:keys [body]}]
     (service/success-response (teams/add-team-members current-user name (service/decode-json body))))

   (POST "/teams/:name/members/deleter" [name :as {:keys [body]}]
     (service/success-response (teams/remove-team-members current-user name (service/decode-json body))))

   (GET "/teams/:name/privileges" [name]
     (service/success-response (teams/list-team-privileges current-user name)))

   (POST "/teams/:name/privileges" [name :as {:keys [body]}]
     (service/success-response (teams/update-team-privileges current-user name (service/decode-json body))))

   (POST "/teams/:name/join" [name]
     (service/success-response (teams/join current-user name)))

   (POST "/teams/:name/join-request" [name :as {:keys [body]}]
     (let [encoded (slurp body)
           message (if-not (string/blank? encoded) (:message (service/decode-json encoded)) "")]
       (service/success-response (teams/join-request current-user name message))))

   (POST "/teams/:name/join-request/:requester/deny" [name requester :as {:keys [body]}]
     (let [encoded (slurp body)
           message (if-not (string/blank? encoded) (:message (service/decode-json encoded)) "")]
       (service/success-response (teams/deny-join-request current-user name requester message))))

   (POST "/teams/:name/leave" [name]
     (service/success-response (teams/leave current-user name)))))

(defn community-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (GET "/communities" [:as {:keys [params]}]
     (service/success-response (communities/get-communities current-user params)))

   (POST "/communities" [:as {:keys [body]}]
     (service/success-response (communities/add-community current-user (service/decode-json body))))

   (GET "/communities/:name" [name]
     (service/success-response (communities/get-community current-user name)))

   (PATCH "/communities/:name" [name :as {:keys [params body]}]
     (service/success-response (communities/update-community current-user name params (service/decode-json body))))

   (DELETE "/communities/:name" [name]
     (service/success-response (communities/delete-community current-user name)))

   (GET "/communities/:name/admins" [name]
     (service/success-response (communities/get-community-admins current-user name)))

   (POST "/communities/:name/admins" [name :as {:keys [body]}]
     (service/success-response (communities/add-community-admins current-user name (service/decode-json body))))

   (POST "/communities/:name/admins/deleter" [name :as {:keys [body]}]
     (service/success-response (communities/remove-community-admins current-user name (service/decode-json body))))

   (GET "/communities/:name/members" [name]
     (service/success-response (communities/get-community-members current-user name)))

   (POST "/communities/:name/join" [name]
     (service/success-response (communities/join current-user name)))

   (POST "/communities/:name/leave" [name]
     (service/success-response (communities/leave current-user name)))))

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
