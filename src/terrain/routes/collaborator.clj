(ns terrain.routes.collaborator
  (:use [compojure.core]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.util :only [optional-routes]])
  (:require [cheshire.core :as json]
            [terrain.clients.apps.raw :as apps]
            [terrain.services.collaborator-lists :as cl]
            [terrain.services.subjects :as subjects]
            [terrain.services.teams :as teams]
            [terrain.util.config :as config]
            [terrain.util.service :as service]))

(defn collaborator-list-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (GET "/collaborator-lists" [:as {:keys [params]}]
     (service/success-response (cl/get-collaborator-lists current-user params)))

   (POST "/collaborator-lists" [:as {:keys [body]}]
     (service/success-response (cl/add-collaborator-list current-user (service/decode-json body))))

   (GET "/collaborator-lists/:name" [name]
     (service/success-response (cl/get-collaborator-list current-user name)))

   (PATCH "/collaborator-lists/:name" [name :as {:keys [body]}]
     (service/success-response (cl/update-collaborator-list current-user name (service/decode-json body))))

   (DELETE "/collaborator-lists/:name" [name :as {:keys [params]}]
     (service/success-response (cl/delete-collaborator-list current-user name params)))

   (GET "/collaborator-lists/:name/members" [name]
     (service/success-response (cl/get-collaborator-list-members current-user name)))

   (POST "/collaborator-lists/:name/members" [name :as {:keys [body]}]
     (service/success-response (cl/add-collaborator-list-members current-user name (service/decode-json body))))

   (POST "/collaborator-lists/:name/members/deleter" [name :as {:keys [body params]}]
     (service/success-response
      (cl/remove-collaborator-list-members current-user name (service/decode-json body) params)))))

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

   (POST "/teams/:name/leave" [name]
     (service/success-response (teams/leave current-user name)))))

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
