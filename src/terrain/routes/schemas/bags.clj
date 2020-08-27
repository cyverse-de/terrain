(ns terrain.routes.schemas.bags
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema Any Keyword optional-key]])
  (:import [java.util UUID]))

(def BagIDPathParam (describe String "The ID of the bag located in the path of the URL"))

(defschema Bag
  {:id       (describe UUID "The bag id")
   :user_id  (describe UUID "The user's id")
   :contents (describe String "JSON-encoded bag")})

(defschema AddBagResponse
  {:id (describe UUID "The UUID assigned to the bag")})

(def AddBagDescription "Adds a new bag to the list for the user")
(def AddBagSummary "Adds a new bag")

(def UpdateBagSummary "Update a bag")
(def UpdateBagDescription "Updates a bag for a user based on its UUID")

(defschema BagList
  {:bags (describe [Bag] "The list of bags associated with the user")})

(def HasBagsSummary "Tells whether a user has a bag")
(def HasBagsDescription "Tells whether a user has one or more bags in the database")

(def BagListSummary "List user bags")
(def BagListDescription "List all of the bags for the user")

(def GetBagSummary "Gets a bag")
(def GetBagDescription "Gets a single bag for a user by its UUID")

(def DeleteAllBagsSummary "Deletes all of a user's bags")
(def DeleteAllBagsDescription "Deletes all of a user's bags")

(def DeleteBagSummary "Delete a bag")
(def DeleteBagDescription "Delete's a bag for a user based on its UUID")