(ns terrain.routes.bags
  (:require [common-swagger-api.schema :refer [context HEAD GET PUT DELETE POST]]
            [ring.util.http-response :refer [ok]]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.routes.schemas.bags :as bags-schema]
            [terrain.services.bags :as bags]
            [terrain.util :refer [optional-routes]]
            [terrain.util.config :as config]))

(declare body bag-id)

(defn bag-routes
  []
  (optional-routes
   [config/bag-routes-enabled]

   (context "/bags" []
     :tags ["bags"]

     (HEAD "/" []
       :summary     bags-schema/HasBagsSummary
       :description bags-schema/HasBagsDescription
       (bags/has-bags (:username current-user))
       (ok))

     (GET "/" []
       :summary     bags-schema/BagListSummary
       :description bags-schema/BagListDescription
       :return      bags-schema/BagList
       (ok (bags/get-bags (:username current-user))))

     (PUT "/" []
       :summary     bags-schema/AddBagSummary
       :description bags-schema/AddBagDescription
       :body        [body bags-schema/BagContents]
       :return      bags-schema/AddBagResponse
       (ok (bags/add-bag (:username current-user) body)))

     (DELETE "/" []
       :summary     bags-schema/DeleteAllBagsSummary
       :description bags-schema/DeleteAllBagsDescription
       (bags/delete-all-bags (:username current-user))
       (ok))

     (context "/default" []

       (GET "/" []
         :summary     bags-schema/GetDefaultBagSummary
         :description bags-schema/GetDefaultBagDescription
         :return      bags-schema/Bag
         (ok (bags/get-default-bag (:username current-user))))

       (POST "/" []
         :summary     bags-schema/UpdateDefaultBagSummary
         :description bags-schema/UpdateDefaultBagDescription
         :body        [body bags-schema/BagContents]
         :return      bags-schema/Bag
         (ok (bags/update-default-bag (:username current-user) body)))

       (DELETE "/" []
         :summary     bags-schema/DeleteDefaultBagSummary
         :description bags-schema/DeleteDefaultBagDescription
         :return      bags-schema/Bag
         (ok (bags/delete-default-bag (:username current-user)))))

     (context "/:bag-id" []
       :path-params [bag-id :- bags-schema/BagIDPathParam]

       (GET "/" []
         :summary     bags-schema/GetBagSummary
         :description bags-schema/GetBagDescription
         (ok (bags/get-bag (:username current-user) bag-id)))

       (POST "/" []
         :summary     bags-schema/UpdateBagSummary
         :description bags-schema/UpdateBagDescription
         :body        [body bags-schema/BagContents]
         (ok (bags/update-bag (:username current-user) bag-id body)))

       (DELETE "/" []
         :summary     bags-schema/DeleteBagSummary
         :description bags-schema/DeleteBagDescription
         (bags/delete-bag (:username current-user) bag-id)
         (ok))))))
