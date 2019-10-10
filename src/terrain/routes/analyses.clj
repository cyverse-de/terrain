(ns terrain.routes.analyses
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.apps :only [AppIdParam AppJobView]]
        [common-swagger-api.schema.quicklaunches
         :only [QuickLaunch
                NewQuickLaunch
                UpdateQuickLaunch
                QuickLaunchFavorite
                NewQuickLaunchFavorite
                QuickLaunchUserDefault
                UpdateQuickLaunchUserDefault
                NewQuickLaunchUserDefault
                QuickLaunchGlobalDefault
                UpdateQuickLaunchGlobalDefault
                NewQuickLaunchGlobalDefault]]
        [ring.util.http-response :only [ok]]
        [terrain.util :only [optional-routes]])
  (:require [common-swagger-api.schema.analyses :as schema]
            [common-swagger-api.schema.analyses.listing :as listing-schema]
            [common-swagger-api.schema.apps.permission :as perms-schema]
            [schema.core :as s]
            [schema-tools.core :as st]
            [terrain.clients.analyses :as analyses]
            [terrain.clients.app-exposer :as app-exposer]
            [terrain.clients.apps.raw :as apps]
            [terrain.util.config :as config])
  (:import [java.util UUID]))

(defn analysis-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (context "/analyses" []
     :tags ["analyses"]

     (GET "/" []
       :query [params listing-schema/AnalysisListingParams]
       :return listing-schema/AnalysisList
       :summary listing-schema/AnalysesListingSummary
       :description listing-schema/AnalysesListingDocs
       (ok (apps/list-jobs params)))

     (POST "/" []
       :body [body schema/AnalysisSubmission]
       :return schema/AnalysisResponse
       :summary listing-schema/AnalysisSubmitSummary
       :description listing-schema/AnalysisSubmitDocs
       (ok (apps/submit-job body)))

     (POST "/permission-lister" []
       :query [params perms-schema/PermissionListerQueryParams]
       :body [body perms-schema/AnalysisIdList]
       :return perms-schema/AnalysisPermissionListing
       :summary perms-schema/AnalysisPermissionListingSummary
       :description perms-schema/AnalysisPermissionListingDocs
       (ok (apps/list-job-permissions body params)))

     (POST "/sharing" []
       :body [body perms-schema/AnalysisSharingRequest]
       :return perms-schema/AnalysisSharingResponse
       :summary perms-schema/AnalysisSharingSummary
       :description perms-schema/AnalysisSharingDocs
       (ok (apps/share-jobs body)))

     (POST "/unsharing" []
       :body [body perms-schema/AnalysisUnsharingRequest]
       :return perms-schema/AnalysisUnsharingResponse
       :summary perms-schema/AnalysisUnsharingSummary
       :description perms-schema/AnalysisUnsharingDocs
       (ok (apps/unshare-jobs body)))

     (POST "/shredder" []
       :body [body schema/AnalysisShredderRequest]
       :summary listing-schema/AnalysesDeleteSummary
       :description listing-schema/AnalysesDeleteDocs
       (ok (apps/delete-jobs body)))

     (context "/:analysis-id" []
       :path-params [analysis-id :- schema/AnalysisIdPathParam]

       (PATCH "/" []
         :body [body listing-schema/AnalysisUpdate]
         :return listing-schema/AnalysisUpdateResponse
         :summary listing-schema/AnalysisUpdateSummary
         :description listing-schema/AnalysisUpdateDocs
         (ok (apps/update-job analysis-id body)))

       (DELETE "/" []
         :summary listing-schema/AnalysisDeleteSummary
         :description listing-schema/AnalysisDeleteDocs
         (ok (apps/delete-job analysis-id)))

       (GET "/history" []
         :return listing-schema/AnalysisHistory
         :summary listing-schema/AnalysisHistorySummary
         :description listing-schema/AnalysisHistoryDocs
         (ok (apps/get-job-history analysis-id)))

       (GET "/parameters" []
         :return schema/AnalysisParameters
         :summary schema/AnalysisParametersSummary
         :description schema/AnalysisParametersDocs
         (ok (apps/get-job-params analysis-id)))

       (GET "/relaunch-info" []
         :return AppJobView
         :summary schema/AnalysisRelaunchSummary
         :description schema/AnalysisRelaunchDocs
         (ok (apps/get-job-relaunch-info analysis-id)))

       (GET "/steps" []
         :return listing-schema/AnalysisStepList
         :summary listing-schema/AnalysisStepsSummary
         :description listing-schema/AnalysisStepsDocs
         (ok (apps/list-job-steps analysis-id)))

       (POST "/stop" []
         :query [params schema/StopAnalysisRequest]
         :return schema/StopAnalysisResponse
         :summary schema/AnalysisStopSummary
         :description schema/AnalysisStopDocs
         (ok (apps/stop-job analysis-id params)))

       (GET "/logs" []
         :query [params schema/AnalysisPodLogParameters]
         :return schema/AnalysisPodLogEntry
         :summary schema/AnalysisPodLogSummary
         :description schema/AnalysisPodLogDescription
         (ok (app-exposer/get-pod-logs analysis-id params)))

       (GET "/time-limit" []
         :return schema/AnalysisTimeLimit
         :summary "Get the time limit for an analysis."
         :description "Get the time limit for an analysis. Returns seconds
             since the epoch for the time limit as a string or returns the
             string null if the time limit has not been set."
         (ok (app-exposer/get-time-limit analysis-id)))

       (POST "/time-limit" []
         :return schema/AnalysisTimeLimit
         :summary "Extend the time limit for an analysis."
         :description "Adds three days to the time limit for an analysis.
              Returns seconds since the epoch for the time limit as a string.
              Returns an error if the time limit is null."
         (ok (app-exposer/set-time-limit analysis-id)))))))

(s/defschema DeletionResponse
  {:id (describe UUID "The UUID of the resource that was deleted")})

(def QuickLaunchID (describe UUID "The UUID of the Quick Launch"))

(def QuickLaunchFavoriteID
  (describe UUID "The UUID of a Quick Launch favorite"))

(def QuickLaunchUserDefaultID
  (describe UUID "The UUID of the a user-defined Quick Launch default"))

(def QuickLaunchGlobalDefaultID
  (describe UUID "The UUID of a global Quick Launch default"))

(def NewQuickLaunchForTerrain
  (st/dissoc NewQuickLaunch :creator))

(defn quicklaunch-routes
  "The routes for accessing analyses information. Currently limited to Quick
   Launches."
  []
  (optional-routes [config/app-routes-enabled])

  (context "/quicklaunches" []
    :tags ["analyses-quicklaunches"]

    (context "/apps" []
      (context "/:app-id" []
        :path-params [app-id :- AppIdParam]

        (GET "/" []
          :return [QuickLaunch]
          :summary "Get all of the Quick Launches for a user by app UUID"
          :description "Get all of the Quick Launches for a user by app UUID.
          The user must have access to the Quick Launch"
          (ok (analyses/get-quicklaunches-by-app app-id)))))

    ;; Quick Launch Favorites
    (context "/favorites" []
      (GET "/" []
        :return      [QuickLaunchFavorite]
        :summary     "Get information about all favorited Quick Launches"
        :description "Get in information about all favorited Quick Launches by the
        logged in user, including their UUIDs and Quick Launch UUIDS"
        (ok (analyses/get-all-quicklaunch-favorites)))

      (POST "/" []
        :body        [fave NewQuickLaunchFavorite]
        :return      QuickLaunchFavorite
        :summary     "Favorite a Quick Launch"
        :description "Favorite a Quick Launch for the logged in user"
        (ok (analyses/add-quicklaunch-favorite fave)))

      (context "/:fave-id" []
        :path-params [fave-id :- QuickLaunchFavoriteID]

        (GET "/" []
          :return      QuickLaunchFavorite
          :summary     "Get information for a favorited Quick Launch"
          :description "Get information for a favorited Quick Launch, including the
          UUID, the user that favorited it, and the Quick Launch UUID"
          (ok (analyses/get-quicklaunch-favorite fave-id)))

        (DELETE "/" []
          :return      DeletionResponse
          :summary     "Un-favorite a Quick Launch"
          :description "Un-favorite a Quick Launch for the logged in user. Does not
          delete the Quick Launch, just the entry that indicated that it was a
          favorite of the user"
          (ok (analyses/delete-quicklaunch-favorite fave-id)))))


    ;; Quick Launch Defaults


    (context "/defaults" []

      ;; User Defaults
      (context "/user" []
        (GET "/" []
          :return      [QuickLaunchUserDefault]
          :summary     "Get information for all of the user-defined Quick Launch defaults
          for the logged in user"
          :description "Get information for all of the user-defined Quick Launch
          defaults for the logged-in user. Includes all of the info returned by
          the endpoint that lists individual user defaults"
          (ok (analyses/get-all-quicklaunch-user-defaults)))

        (POST "/" []
          :body        [new NewQuickLaunchUserDefault]
          :return      QuickLaunchUserDefault
          :summary     "Set a user-defined Quick Launch default"
          :description "Set a user-defined Quick Launch default for the logged-in
          user"
          (ok (analyses/add-quicklaunch-user-default new)))

        (context "/:user-default-id" []
          :path-params [user-default-id :- QuickLaunchUserDefaultID]

          (GET "/" []
            :return      QuickLaunchUserDefault
            :summary     "Get information for a user-defined Quick Launch default"
            :description "Get information for a user-defined Quick Launch default.
            These are not the ones defined by the app creator, they're the user
            overrides"
            (ok (analyses/get-quicklaunch-user-default user-default-id)))

          (PATCH "/" []
            :body        [update UpdateQuickLaunchUserDefault]
            :return      QuickLaunchUserDefault
            :summary     "Edit a user-defined Quick Launch default"
            :description "Edit a user-defined Quick Launch default. Note that most or
            all of the fields in the JSON body are optional"
            (ok (analyses/update-quicklaunch-user-default user-default-id update)))

          (DELETE "/" []
            :return      DeletionResponse
            :summary     "Delete a user-defined Quick Launch default"
            :description "Delete a user-defined Quick Launch default. Does not delete
            the Quick Launch or the global Quick Launch default"
            (ok (analyses/delete-quicklaunch-user-default user-default-id)))))

      ;; Global Defaults
      (context "/global" []

        (GET "/" []
          :return      [QuickLaunchGlobalDefault]
          :summary     "Get information for all of the globally-defined Quick Launch
          defaults"
          :description "Get information for all of the globally-defined Quick Launch
          defaults. Includes all of the info returned by the endpoint that lists
          individual user defaults"
          (ok (analyses/get-all-quicklaunch-global-defaults)))

        (POST "/" []
          :body        [new NewQuickLaunchGlobalDefault]
          :return      QuickLaunchGlobalDefault
          :summary     "Set a globally-defined Quick Launch default"
          :description "Set a globally-defined Quick Launch default"
          (ok (analyses/add-quicklaunch-global-default new)))

        (context "/:global-default-id" []
          :path-params [global-default-id :- QuickLaunchGlobalDefaultID]

          (GET "/" []
            :return      QuickLaunchGlobalDefault
            :summary     "Get information for a globally-defined Quick Launch default"
            :description "Get information for a globally-defined Quick Launch default.
            These are the ones defined by the app creator"
            (ok (analyses/get-quicklaunch-global-default global-default-id)))

          (PATCH "/" []
            :body        [update UpdateQuickLaunchGlobalDefault]
            :return      QuickLaunchGlobalDefault
            :summary     "Edit a globally-defined Quick Launch default"
            :description "Edit a globally-defined Quick Launch default. Note that most
            or all of the fields in the JSON body are optional"
            (ok (analyses/update-quicklaunch-global-default global-default-id update)))

          (DELETE "/" []
            :return      DeletionResponse
            :summary     "Delete a globally-defined Quick Launch default"
            :description "Delete a globally-defined Quick Launch default"
            (ok (analyses/delete-quicklaunch-global-default global-default-id))))))

    (GET "/" []
      :return [QuickLaunch]
      :summary "List Quick Launches created by the user"
      :description "List Quick Launches created by the user"
      (ok (analyses/get-all-quicklaunches)))

    (POST "/" []
      :body        [quicklaunch NewQuickLaunchForTerrain]
      :return      QuickLaunch
      :summary     "Adds a Quick Launch to the database"
      :description "Adds a Quick Launch and corresponding submission information to the
      database. The username passed in should already exist. A new UUID will be
      assigned and returned."
      (ok (analyses/add-quicklaunch quicklaunch)))

    (context "/:ql-id" []
      :path-params [ql-id :- QuickLaunchID]

      (GET "/app-info" []
        :return      AppJobView
        :summary     "Returns app launch info based on the Quick Launch"
        :description "Returns app launch info based on the Quick Launch. Uses
        the Quick Launch submission along with the app definition to form JSON
        that the UI can use to create and populate an app launch window with
        parameter values filled in"
        (ok (analyses/get-quicklaunch-app-info ql-id)))

      (GET "/" []
        :summary     "Get Quick Launch information by its UUID."
        :description "Gets Quick Launch information, including the UUID, the name of
        the user that owns it, and the submission JSON"
        :return  QuickLaunch
        (ok (analyses/get-quicklaunch ql-id)))

      (PATCH "/" []
        :body        [quicklaunch UpdateQuickLaunch]
        :return      QuickLaunch
        :summary     "Modifies an existing Quick Launch"
        :description "Modifies an existing Quick Launch, allowing the caller to change
        owners and the contents of the submission JSON"
        (ok (analyses/update-quicklaunch ql-id quicklaunch)))

      (DELETE "/" []
        :return      DeletionResponse
        :summary     "Deletes a Quick Launch"
        :description "Deletes a Quick Launch from the database. Will returns a success
        even if called on a Quick Launch that has either already been deleted or never
        existed in the first place"
        (ok (analyses/delete-quicklaunch ql-id))))))
