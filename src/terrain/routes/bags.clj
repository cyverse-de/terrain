(ns terrain.routes.bags
  (:use [common-swagger-api.schema]
        [ring.util.http-response :only [ok]]
        [terrain.routes.schemas.bags]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.services.bags]
        [terrain.util :only [optional-routes]])
  (:require [terrain.util.config :as config]
            [clojure.tools.logging :as log]))

(defn bag-routes
  []
  (optional-routes
   [config/bag-routes-enabled]

   (context "/bags" []
     :tags ["bags"]

     (HEAD "/" []
       :summary     HasBagsSummary
       :description HasBagsDescription
       (ok (has-bags (:username current-user))))

     (GET "/" []
       :summary     BagListSummary
       :description BagListDescription
       :return      BagList
       (ok (get-bags (:username current-user))))

     (PUT "/" []
       :summary     AddBagSummary
       :description AddBagDescription
       :body        [body BagContents]
       :return      AddBagResponse
       (ok (add-bag (:username current-user) body)))

     (DELETE "/" []
       :summary     DeleteAllBagsSummary
       :description DeleteAllBagsDescription
       (ok (delete-all-bags (:username current-user))))

     (context "/:bag-id" []
       :path-params [bag-id :- BagIDPathParam]

       (GET "/" []
         :summary     GetBagSummary
         :description GetBagDescription
         (ok (get-bag (:username current-user) bag-id)))

       (POST "/" []
         :summary     UpdateBagSummary
         :description UpdateBagDescription
         :body        [body BagContents]
         (ok (update-bag (:username current-user) bag-id body)))

       (DELETE "/" []
         :summary     DeleteBagSummary
         :description DeleteBagDescription
         (ok (delete-bag (:username current-user) bag-id)))))))