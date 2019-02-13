(ns terrain.routes.token
  (:use [common-swagger-api.schema :only [context routes GET]]
        [terrain.routes.schemas.token]
        [terrain.services.oauth :only [get-token]])
  (:require [clojure.string :as string]
            [terrain.util]))

(defn token-routes
  []
  (routes
   (context "/token" []
     :tags ["token"]

     (GET "/" [:as {{:strs [authorization]} :headers}]
       :summary "Obtain OAuth Tokens"
       :return AccessTokenResponse
       :description-file "docs/get-token.md"
       (get-token authorization)))))
