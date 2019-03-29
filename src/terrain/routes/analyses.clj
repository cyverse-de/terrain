(ns terrain.routes.analyses
  (:require [compojure.api.sweet :refer [describe]]
            [common-swagger-api.schema :refer [context GET PATCH POST DELETE]]
            [common-swagger-api.schema.quicklaunches :refer [QuickLaunch NewQuickLaunch UpdateQuickLaunch]]
            [common-swagger-api.schema.apps :refer [AnalysisSubmission]]
            [ring.util.http-response :refer [ok]]
            [terrain.util :refer [optional-routes]]
            [schema.core :as s]
            [terrain.util.config :as config]
            [terrain.clients.analyses :as analyses])
  (:import [java.util UUID]))

(s/defschema DeletionResponse
  {:id (describe UUID "The UUID of the resource that was deleted")})

(defn secured-analyses-routes
  "The routes for accessing analyses information. Currently limited to Quick
   Launches."
  []
  (optional-routes [config/app-routes-enabled])

  (context "/quicklaunches" []
    :tags ["analyses"]

    (GET "/:id" [id]
      :summary "Get Quick Launch information by its UUID."
      :description "Gets Quick Launch information, including the UUID, the name of
      the user that owns it, and the submission JSON"
      :return  QuickLaunch
      (ok (analyses/get-quicklaunch id)))

    (PATCH "/:id" [id]
      :body         [quicklaunch UpdateQuickLaunch]
      :return       QuickLaunch
      :summary      "Modifies an existing Quick Launch"
      :description  "Modifies an existing Quick Launch, allowing the caller to change
      owners and the contents of the submission JSON"
      (ok (analyses/update-quicklaunch id quicklaunch)))

    (POST "/" []
      :body         [quicklaunch NewQuickLaunch]
      :return       QuickLaunch
      :summary      "Adds a Quick Launch to the database"
      :description  "Adds a Quick Launch and corresponding submission information to the
      database. The username passed in should already exist. A new UUID will be
      assigned and returned."
      (ok (analyses/add-quicklaunch quicklaunch)))

    (DELETE "/:id" [id]
      :return        DeletionResponse
      :summary      "Deletes a Quick Launch"
      :description  "Deletes a Quick Launch from the database. Will returns a success
      even if called on a Quick Launch that has either already been deleted or never
      existed in the first place"
      (ok (analyses/delete-quicklaunch id)))))
