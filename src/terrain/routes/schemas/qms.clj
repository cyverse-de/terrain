(ns terrain.routes.schemas.qms
  (:require [common-swagger-api.schema :refer [PagingParams describe]]
            [schema.core :refer [defschema Any optional-key maybe enum]])
  (:import [java.util UUID]))

(def GetAllPlansSummary "Returns a list of all plans registered in QMS")
(def GetAllPlansDescription "Returns a list of all plans registered in QMS. New plans may be registered at run-time through QMS itself")
(def GetPlanSummary "Returns details about a single plan in QMS")
(def GetPlanDescription "Returns details about a single plan in QMS. Plan is referenced by its UUID")
(def GetResourceTypesSummary "List Resource Types")
(def GetResourceTypesDescription "Returns a list of all resource types registered in QMS")
(def GetSubscriptionSummary "Returns details about the user's current plan")
(def GetSubscriptionDescription "Returns details about the user's current plan, including quota and usage information")
(def UpdateSubscriptionQuotaSummary "Updates a quota in the user's current subscription")
(def UpdateSubscriptionQuotaDescription "Updates the user's quota for the specified resource type")
(def UpdateSubscriptionSummary "Changes the user's plan to another one in QMS")
(def UpdateSubscriptionDescription "Changes the user's plan to another one in QMS. New plan is referenced by name")
(def GetUserUsagesSummary "Returns a list of resource usages generated by the user")
(def GetUserUsagesDescription "Returns a list of resource usages generated by the user")
(def UpdateUsageSummary "Updates resource usage totals for a user")
(def UpdateUsageDescription "Updates resource usage totals for a user")
(def CreateSubscriptionsSummary "Bulk Subscription Creation")
(def CreateSubscriptionsDescription "Creates multiple subscriptions in one request")
(def ListSubscriptionsSummary "List Subscriptions")
(def ListSubscriptionsDescription "Lists existing subscriptions")

(def PlanID (describe (maybe UUID) "The UUID assigned to a plan in QMS"))
(def PlanName (describe String "The name of the plan"))
(def PlanQuotaDefault (describe (maybe UUID) "The UUIID assigned to the plan quota default"))
(def QMSUserID (describe (maybe UUID) "The user's UUID assigned by QMS"))
(def QuotaID (describe (maybe UUID) "The UUID for the quota in QMS"))
(def ResourceID (describe (maybe UUID) "The UUID assigned to a resource type record"))
(def UsageID (describe (maybe UUID) "The UUID assigned to a user's usage record for a resource"))
(def Username (describe String "A user's username"))
(def ResourceTypeName (describe String "The name of the resource type"))

(defschema SuccessResponse
  {(optional-key :result) (describe (maybe Any) "The result of the response")
   (optional-key :error)  (describe (maybe String) "The error string")
   :status                (describe String "The status of the response")})

(defschema ResourceType
  {:id   ResourceID
   :name ResourceTypeName
   :unit (describe String "The unit of the resource type")})

(defschema ResourceTypesResponse
  {:result (describe [ResourceType] "The list of resource types")
   :status (describe String "The status of the response")})

(defschema Usage
  {:id                              UsageID
   :usage                           (describe Double "The usage value")
   :resource_type                   ResourceType
   (optional-key :last_modified_at) (describe (maybe String) "The time the usage record was last modified")})

(defschema UsagesResponse
  {(optional-key :result) (describe (maybe [Usage]) "The list of usages")
   (optional-key :error)  (describe (maybe String) "The error message")
   :status                (describe String "The status of the response")})

(defschema AddUsage
  {:username      Username
   :resource_name (describe String "The name of the resource that was used")
   :usage_value   (describe Double "The usage value")
   :update_type   (describe String "The update type")})

(defschema QMSUser
  {(optional-key :id)       QMSUserID
   (optional-key :username) (describe String "The user's username in QMS")})

(defschema PlanQuotaDefault
  {(optional-key :id)            PlanQuotaDefault
   (optional-key :quota_value)   (describe Double "The quota's default value")
   (optional-key :resource_type) ResourceType})

(defschema Plan
  {(optional-key :id)                  PlanID
   (optional-key :name)                (describe String "The name of the plan in QMS")
   (optional-key :description)         (describe String "The description of the plan")
   (optional-key :plan_quota_defaults) (describe [PlanQuotaDefault] "The list of default values for the quotas")})

(defschema PlanListResponse
  {(optional-key :result) (describe (maybe [Plan]) "The list of plans")
   (optional-key :error)  (describe (maybe String) "The error message")
   :status                (describe String "The status of the response")})

(defschema PlanResponse
  {(optional-key :result) (describe (maybe Plan) "The plan")
   (optional-key :error)  (describe (maybe String) "The error message")
   :status                (describe String "The status of the response")})

(defschema Quota
  {:id                              QuotaID
   :quota                           (describe Double "The value associated with the quota")
   :resource_type                   ResourceType
   (optional-key :last_modified_at) (describe (maybe String) "The time the quota was last modified.")})

(defschema QuotaValue
  {:quota (describe Double "The resource usage limit")})

(defschema Subscription
  {(optional-key :id)                   (describe (maybe UUID) "The UUID assigned to a user's plan")
   (optional-key :effective_start_date) (describe (maybe String) "The date the user's plan takes effect")
   (optional-key :effective_end_date)   (describe (maybe String) "The date the user's plan ends")
   (optional-key :user)                 QMSUser
   (optional-key :plan)                 Plan
   (optional-key :quotas)               (describe (maybe [Quota]) "The list of quotas associated with the user's plan")
   (optional-key :usages)               (describe (maybe [Usage]) "The list of usages associated with the user's plan")})

(defschema SubscriptionResponse
  {(optional-key :result) (describe (maybe Subscription) "The user's plan")
   (optional-key :error)  (describe (maybe String) "The error message")
   :status                (describe String "The status of the response")})

(defschema SubscriptionResponse
  (merge
   Subscription
   {(optional-key :failure_reason)
    (describe (maybe String) "The reason for the failure if the subscription couldn't be created")

    (optional-key :new_subscription)
    (describe (maybe Boolean) "True if the subscription was created as part of this request")}))

(defschema BulkSubscriptionResponse
  {(optional-key :result) (describe (maybe [SubscriptionResponse]) "The response for each of the subscription requests")
   (optional-key :error)  (describe (maybe String) "The error message if the request could not be completed")
   :status                (describe String "The status of the request")})

(defschema SubscriptionUpdateResponse
  {(optional-key :result) (describe (maybe SubscriptionResponse) "The response for the subscription update request")
   (optional-key :error)  (describe (maybe String) "The error message if the request could not be completed")
   (optional-key :status) (describe (maybe String) "The status of the request")})

(defschema SubscriptionRequest
  {(optional-key :username)  (describe String "The username to associate with the subscription")
   (optional-key :plan_name) (describe String "The name of the plan to associate with the subscription")})

(defschema SubscriptionRequests
  {(optional-key :subscriptions) (describe (maybe [SubscriptionRequest]) "The list of subscription requests")})

(defschema BulkSubscriptionParams
  {(optional-key :force)
   (describe String "True if the subscription should be created even if the user already has a higher level plan")})

(defschema ListSubscriptionsParams
  (merge PagingParams
         {(optional-key :search)
          (describe String "The username substring to search for in the listing")}))

(defschema SubscriptionListing
  {:subscriptions (describe [Subscription] "The subscription listing")
   :total         (describe Integer "The total number of matching subscriptions")})

(defschema SubscriptionListingResponse
  {(optional-key :result) (describe (maybe SubscriptionListing) "The subscription listing")
   (optional-key :error)  (describe (maybe String) "The error message if the request could not be completed")
   :status                (describe String "The status of the request")})
