(ns terrain.routes.schemas.qms
  (:require [common-swagger-api.schema :refer [describe]]
            [schema.core :refer [defschema]])
  (:import [java.util UUID]))

(def PlanID (describe UUID "The UUID assigned to a plan in QMS"))
(def QMSUserID (describe UUID "The user's UUID assigned by QMS"))
(def QuotaID (describe UUID "The UUID for the quota in QMS"))
(def ResourceID (describe UUID "The UUID assigned to a resource type record"))
(def UsageID (describe UUID "The UUID assigned to a user's usage record for a resource"))

(defschema ResourceType
  {:id   ResourceID
   :name (describe String "The name of the resource type")
   :unit (describe String "The unit of the resource type")})

(defschema Usage
  {:id            UsageID
   :usage         (describe Double "The usage value")
   :resource_type ResourceType})

(defschema QMSUser
  {:id        QMSUserID
   :username (describe String "The user's username in QMS")})

(defschema Plan
  {:id   PlanID
   :name (describe String "The name of the plan in QMS")})

(defschema Quota
  {:id            QuotaID
   :quota         (describe Double "The value associated with the quota")
   :resource_type ResourceType})

(defschema UserPlan
  {:id                   (describe String "The UUID assigned to a user's plan")
   :effective_start_date (describe String "The date the user's plan takes effect")
   :effective_end_date   (describe String "The date the user's plan ends")
   :user                 QMSUser
   :plan                 Plan
   :quotas               (describe [Quota] "The list of quotas associated with the user's plan")
   :usages               (describe [Usage] "The list of usages associated with the user's plan")})