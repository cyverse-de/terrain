(ns terrain.routes.collaborator
  (:use [compojure.core]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.util :only [optional-routes]])
  (:require [cheshire.core :as json]
            [terrain.clients.apps.raw :as apps]
            [terrain.services.collaborator-lists :as cl]
            [terrain.util.config :as config]
            [terrain.util.service :as service]))

(defn collaborator-list-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (GET "/collaborator-lists" [:as {:keys [params]}]
     (service/success-response (cl/get-collaborator-lists current-user params)))

   (POST "/collaborator-lists" [:as {:keys [body]}]
     (service/success-response (cl/add-collaborator-list current-user (json/decode (slurp body) true))))))

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
