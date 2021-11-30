(ns terrain.routes.schemas.data-usage-api
  (:require [common-swagger-api.schema :refer [describe]]
            [schema.core :refer [defschema optional-key maybe]])
  (:import [java.util UUID]))

(def UserCurrentDataSummary "Get the most recent summary of a user's data usage")

(def UserCurrentDataDescription "Uses the current date to get the most recent record of data usage for the currently logged-in user")

(defschema UserCurrentDataTotal
  {:id            (describe UUID "The UUID assigned to the current reading")
   :user_id       (describe UUID "The UUID assigned to the user the reading applies to")
   :username      (describe String "The username of the user the reading applies to")
   :total         (describe Integer "The total number of bytes the user is consuming in the Data Store")
   :time          (describe String "The date of the reading")
   :last_modified (describe String "The date the reading in the database was last modified")})
