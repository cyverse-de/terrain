(ns terrain.routes.user-info
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.user-info]
        [terrain.services.user-info]
        [terrain.util]
        [terrain.util.service :only [success-response]])
  (:require [terrain.clients.iplant-groups :as ipg]
            [terrain.util.config :as config]))

(defn secured-user-info-routes
  []
  (optional-routes
   [config/user-info-routes-enabled]

   (context "/user-info" []
     :tags ["user-info"]

     (GET "/" []
          :query [params UserInfoRequest]
          :return (doc-only UserInfoResponse UserInfoResponseDocs)
          :summary "Get user information"
          :description "Returns account information associated with each username or ID.  If the ID
          belongs to an individual user, information like first and last name, as well as institution and
          email will be returned.  If the ID belongs to a group, such as a team or collaborator list,
          then the name and description of the group will be returned."
          (ok (user-info (:username params)))))))

(defn admin-user-info-routes
  []
  (optional-routes
   [config/user-info-routes-enabled]

   (GET "/users/:username/groups" [username]
     (success-response (ipg/list-groups-for-user username)))))
