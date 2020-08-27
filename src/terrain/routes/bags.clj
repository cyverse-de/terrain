(ns terrain.routes.bags
  (:use [common-swagger-api.schema]
        [terrain.routes.schemas.bags]
        [terrain.auth.user-attributes :only [current-user]]
        [terrain.services.bags]
        [terrain.util :only [optional-routes]])
  (:require [terrain.util.config :as config]))

(defn bag-routes
  []
  (optional-routes
   [config/bag-routes-enabled]

   (context "/bags" []
     :tags ["bags"]

     (HEAD "/" []
       :summary     HasBagsSummary
       :description HasBagsDescription
       (has-bags (:username current-user)))

     (GET "/" []
       :summary     BagListSummary
       :description BagListDescription
       :return      BagList
       (get-bags (:username current-user)))

     (PUT "/" [:as {body :body}]
       :summary     AddBagSummary
       :description AddBagDescription
       :body        [body Bag]
       :return      AddBagResponse
       (add-bag (:username current-user) (slurp body)))

     (DELETE "/" []
       :summary     DeleteAllBagsSummary
       :description DeleteAllBagsDescription
       (delete-all-bags (:username current-user)))

     (context "/:bag-id" []
       :path-params [bag-id :- BagIDPathParam]

       (GET "/" []
         :summary     GetBagSummary
         :description GetBagDescription
         (get-bag (:username current-user) bag-id))

       (POST "/" []
         :summary     UpdateBagSummary
         :description UpdateBagDescription
         :body        [body Bag]
         (update-bag (:username current-user) bag-id body))

       (DELETE "/" []
         :summary     DeleteBagSummary
         :description DeleteBagDescription
         (delete-bag (:username current-user) bag-id))))))