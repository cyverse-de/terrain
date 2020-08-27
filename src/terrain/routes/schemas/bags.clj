(ns terrain.routes.schemas.bags
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema Any Keyword optional-key]]))

(defschema Bag
  {:id       (describe String "The bag id")
   :user_id  (describe String "The user's id")
   :contents (describe String "JSON-encoded bag")})

(defschema BagList
  {:bags (describe [Bag] "The list of bags associated with the user")})