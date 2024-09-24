(ns terrain.routes.instantlaunches
  (:require [common-swagger-api.schema :refer [context GET PUT POST DELETE]]
            [common-swagger-api.schema.metadata :refer [AvuList AvuListRequest SetAvuRequest]]
            [common-swagger-api.schema.quicklaunches :refer [QuickLaunch]]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [current-user require-authentication]]
            [terrain.clients.app-exposer :as app-exposer]
            [terrain.routes.schemas.instantlaunches :as instantlaunch-schema]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare query id body data)

(defn instant-launch-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (context "/instantlaunches" []
     :tags ["instant-launches"]

     (context "/mappings" []
       (context "/defaults" []

         (GET "/latest" []
           :middleware  [require-authentication]
           :summary instantlaunch-schema/LatestILMappingsDefaultsSummary
           :description instantlaunch-schema/LatestILMappingsDefaultsDescription
           :return instantlaunch-schema/DefaultInstantLaunchMapping
           (ok (app-exposer/latest-instant-launch-mappings-defaults)))))

     (GET "/" []
       :middleware  [require-authentication]
       :summary instantlaunch-schema/ListInstantLaunchesSummary
       :description instantlaunch-schema/ListInstantLaunchesDescription
       :return instantlaunch-schema/InstantLaunchList
       (ok (app-exposer/get-instant-launch-list)))

     (GET "/full" []
       :middleware  [require-authentication]
       :summary instantlaunch-schema/ListFullInstantLaunchesSummary
       :description instantlaunch-schema/ListFullInstantLaunchesDescription
       :return instantlaunch-schema/FullInstantLaunchList
       (ok (app-exposer/get-full-instant-launch-list)))

     (GET "/metadata/full" []
       :summary instantlaunch-schema/ListFullMetadataSummary
       :description instantlaunch-schema/ListFullMetadataDescription
       :query [query instantlaunch-schema/MetadataListingQueryMap]
       :return instantlaunch-schema/FullInstantLaunchList
       (ok (app-exposer/list-full-metadata query (or (:username current-user) "anonymous"))))

     (context "/quicklaunches" []
       (context "/public" []
         (GET "/" []
           :middleware  [require-authentication]
           :summary instantlaunch-schema/ListQuickLaunchesForPublicAppsSummary
           :description instantlaunch-schema/ListQuickLaunchesForPublicAppsDescription
           :return [QuickLaunch]
           (ok (app-exposer/list-quicklaunches-for-public-apps)))))

     (context "/:id" []
       :path-params [id :- instantlaunch-schema/InstantLaunchIDParam]

       (GET "/" []
         :middleware  [require-authentication]
         :summary instantlaunch-schema/GetInstantLaunchSummary
         :description instantlaunch-schema/GetInstantLaunchDescription
         :return instantlaunch-schema/InstantLaunch
         (ok (app-exposer/get-instant-launch id)))

       (GET "/full" []
         :middleware  [require-authentication]
         :summary instantlaunch-schema/GetFullInstantLaunchSummary
         :description instantlaunch-schema/GetFullInstantLaunchDescription
         :return instantlaunch-schema/FullInstantLaunch
         (ok (app-exposer/get-full-instant-launch id)))))))

(defn admin-instant-launch-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (context "/instant-launches" []
     :tags ["admin-instant-launches"]

     (context "/mappings" []
       (context "/defaults" []
         (PUT "/latest" []
           :summary instantlaunch-schema/AddLatestILMappingsDefaultsSummary
           :description instantlaunch-schema/AddLatestILMappingsDefaultsDescription
           :body [body instantlaunch-schema/InstantLaunchMapping]
           :return instantlaunch-schema/InstantLaunchMapping
           (ok (app-exposer/add-latest-instant-launch-mappings-defaults (:username current-user) body)))

         (POST "/latest" []
           :summary instantlaunch-schema/UpdateLatestILMappingsDefaultsSummary
           :description instantlaunch-schema/UpdateLatestILMappingsDefaultsDescription
           :body [body instantlaunch-schema/InstantLaunchMapping]
           :return instantlaunch-schema/InstantLaunchMapping
           (ok (app-exposer/update-latest-instant-launch-mappings-defaults (:username current-user) body)))

         (DELETE "/latest" []
           :summary instantlaunch-schema/DeleteLatestILMappingsDefaultsSummary
           :description instantlaunch-schema/DeleteLatestILMappingsDefaultsDescription
           (ok (app-exposer/delete-latest-instant-launch-mappings-defaults)))))

     (context "/metadata" []
       (GET "/" []
         :summary instantlaunch-schema/ListMetadataSummary
         :description instantlaunch-schema/ListMetadataDescription
         :query [query instantlaunch-schema/MetadataListingQueryMap]
         :return AvuList
         (ok (app-exposer/list-metadata query)))

       (GET "/full" []
         :summary instantlaunch-schema/ListFullMetadataSummary
         :description instantlaunch-schema/ListFullMetadataDescription
         :query [query instantlaunch-schema/MetadataListingQueryMap]
         :return instantlaunch-schema/FullInstantLaunchList
         (ok (app-exposer/list-full-metadata query (:username current-user)))))

     (context "/:id" []
       :path-params [id :- instantlaunch-schema/InstantLaunchIDParam]

       (POST "/" []
         :summary instantlaunch-schema/UpdateInstantLaunchSummary
         :description instantlaunch-schema/UpdateInstantLaunchDescription
         :body [body instantlaunch-schema/InstantLaunch]
         :return instantlaunch-schema/InstantLaunch
         (ok (app-exposer/update-instant-launch id body)))

       (DELETE "/" []
         :summary instantlaunch-schema/DeleteInstantLaunchSummary
         :description instantlaunch-schema/DeleteInstantLaunchDescription
         (ok (app-exposer/delete-instant-launch id)))

       (context "/metadata" []
         (GET "/" []
           :summary instantlaunch-schema/GetMetadataSummary
           :description instantlaunch-schema/GetMetadataDescription
           :return AvuList
           (ok (app-exposer/get-metadata id)))

         (POST "/" []
           :summary instantlaunch-schema/UpsertMetadataSummary
           :description instantlaunch-schema/UpsertMetadataDescription
           :body [data AvuListRequest]
           :return AvuList
           (ok (app-exposer/upsert-metadata id data)))

         (PUT "/" []
           :summary instantlaunch-schema/ResetMetadataSummary
           :description instantlaunch-schema/ResetMetadataDescription
           :body [data SetAvuRequest]
           :return AvuList
           (ok (app-exposer/reset-metadata id data)))))

     (PUT "/" []
       :summary instantlaunch-schema/AddInstantLaunchSummary
       :description instantlaunch-schema/AddInstantLaunchDescription
       :body [body instantlaunch-schema/InstantLaunch]
       :return instantlaunch-schema/InstantLaunch
       (ok (app-exposer/add-instant-launch (:username current-user) body))))))
