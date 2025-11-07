(ns terrain.routes.user-info
  (:require
   [common-swagger-api.schema :refer [context doc-only GET]]
   [ring.util.http-response :refer [ok]]
   [schema-tools.core :as st]
   [terrain.auth.user-attributes :refer [require-service-account]]
   [terrain.clients.iplant-groups :as ipg]
   [terrain.clients.portal-conductor :as pc]
   [terrain.routes.schemas.user-info :as user-info-schema]
   [terrain.services.user-info :refer [user-info]]
   [terrain.util :refer [optional-routes]]
   [terrain.util.config :as config]))

;; Declarations to eliminate lint warnings for path and query parameter bindings.
(declare params username details)

(defn secured-user-info-routes
  []
  (optional-routes
   [config/user-info-routes-enabled]

   (context "/user-info" []
     :tags ["user-info"]

     (GET "/" []
          :query [params user-info-schema/UserInfoRequest]
          :return (doc-only user-info-schema/UserInfoResponse user-info-schema/UserInfoResponseDocs)
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

   (context "/users" []
     :tags ["admin-user-info"]
     (GET "/:username/groups" []
          :path-params [username :- user-info-schema/UsernameParam]
          :query [{:keys [details]} user-info-schema/DetailsParam]
          :summary "Get a user's groups"
          :description "Lists all groups to which a user belongs"
          :return user-info-schema/GroupListing
          (ok (ipg/list-groups-for-user username details))))))

(defn service-account-user-info-routes
  []
  (optional-routes
   [config/user-info-routes-enabled]

   (context "/users" []
     :tags ["service-account-user-info"]

     (context "/:username" []
       :path-params [username :- user-info-schema/UsernameParam]

       (GET "/" []
            :middleware [[require-service-account ["cyverse-ldap-reader"]]]
            :summary "Get user information"
            :description "Looks up information for a single username in LDAP"
            :return user-info-schema/UserDetails
            (ok (st/select-schema (pc/get-user-details username) user-info-schema/UserDetails)))))))
