(ns terrain.routes.schemas.bags
  (:require [common-swagger-api.schema :refer [describe]]
            [schema.core :refer [defschema Any Keyword]])
  (:import [java.util UUID]))

(def BagIDPathParam (describe String "The ID of the bag located in the path of the URL"))

(defschema BagContents
  {(describe Keyword "Bag key") (describe Any "Bag value")})

(defschema Bag
  {:id       (describe UUID "The bag id")
   :user_id  (describe UUID "The user's id")
   :contents (describe BagContents "JSON-encoded bag")})

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
(def DeleteBagDescription "Deletes a bag for a user based on its UUID")

(def GetDefaultBagSummary "Get the user's default bag")
(def GetDefaultBagDescription "Get the user's default bag. Most interactions should go through the default bag. Creates the bag with default contents if it doesn't already exist")

(def UpdateDefaultBagSummary "Updates the contents of the user's default bag")
(def UpdateDefaultBagDescription "Updates the contents of the user's default bag. Must be JSON. Will create the bag if it doesn't yet exist")

(def DeleteDefaultBagSummary "Delete the default bag for the user")
(def DeleteDefaultBagDescription "Deletes the default bag for the user. If you try to retrieve the bag after this, it will return a new, empty bag")
