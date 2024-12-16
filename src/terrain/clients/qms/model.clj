(ns terrain.clients.qms.model
  (:require [terrain.util.time :as time])
  (:import [org.cyverse.de.protobufs
            AddAddonRequest
            Addon
            AddonRate
            AssociateByUUIDs
            ByUUID
            NoParamsRequest
            ResourceType
            Plan
            PlanRate
            QMSUser
            QuotaDefault
            Subscription
            SubscriptionAddon
            UpdateAddonRequest
            UpdateSubscriptionAddonRequest
            Usage]))

(defn resource-type-from-map
  [m]
  (let [builder (ResourceType/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (when (contains? m :name)
      (.setName builder (:name m)))
    (when (contains? m :unit)
      (.setUnit builder (:unit m)))
    (when (contains? m :consumable)
      (.setConsumable builder (:consumable m)))
    (.build builder)))

(defn addon-rate-from-map
  [m]
  (let [builder (AddonRate/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (when (contains? m :rate)
      (.setRate builder (:rate m)))
    (when (contains? m :effective_date)
      (.setEffectiveDate builder (time/protobuf-timestamp (:effective_date m))))
    (.build builder)))

(defn addon-rates-from-maps
  [ms]
  (map addon-rate-from-map ms))

(defn addon-from-map
  [m]
  (let [builder (Addon/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (when (contains? m :name)
      (.setName builder (:name m)))
    (when (contains? m :description)
      (.setDescription builder (:description m)))
    (when (contains? m :resource_type)
      (.setResourceType builder (resource-type-from-map (:resource_type m))))
    (when (contains? m :default_amount)
      (.setDefaultAmount builder (:default_amount m)))
    (when (contains? m :default_paid)
      (.setDefaultPaid builder (:default_paid m)))
    (when (contains? m :addon_rates)
      (.addAllAddonRates builder (addon-rates-from-maps (:addon_rates m))))
    (.build builder)))

(defn add-addon-request-from-map
  [m]
  (let [builder (AddAddonRequest/newBuilder)]
    (when (contains? m :addon)
      (.setAddon builder (addon-from-map (:addon m))))
    (.build builder)))

(defn new-no-params-request
  []
  (.build (NoParamsRequest/newBuilder)))

(defn update-addon-request-from-map
  [m]
  (let [builder (UpdateAddonRequest/newBuilder)]
    (when (contains? m :addon)
      (.setAddon builder (addon-from-map (:addon m))))
    (when (contains? m :update_name)
      (.setUpdateName builder (:update_name m)))
    (when (contains? m :update_description)
      (.setUpdateDescription builder (:update_description m)))
    (when (contains? m :update_resource_type)
      (.setUpdateResourceType builder (:update_resource_type m)))
    (when (contains? m :update_default_amount)
      (.setUpdateDefaultAmount builder (:update_default_amount m)))
    (when (contains? m :update_default_paid)
      (.setUpdateDefaultPaid builder (:update_default_paid m)))
    (when (contains? m :update_addon_rates)
      (.setUpdateAddonRates builder (:update_addon_rates m)))
    (.build builder)))

(defn by-uuid-request-from-map
  [m]
  (let [builder (ByUUID/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (.build builder)))

(defn associate-by-uuids-from-map
  [m]
  (let [builder (AssociateByUUIDs/newBuilder)]
    (when (contains? m :parent_uuid)
      (.setParentUuid builder (:parent_uuid m)))
    (when (contains? m :child_uuid)
      (.setChildUuid builder (:child_uuid m)))
    (.build builder)))

(defn user-from-map
  [m]
  (let [builder (QMSUser/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (when (contains? m :username)
      (.setUsername builder (:username m)))
    (.build builder)))

(defn quota-default-from-map
  [m]
  (let [builder (QuotaDefault/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (when (contains? m :quota_value)
      (.setQuotaValue builder (:quota_value m)))
    (when (contains? m :resource_type)
      (.setResourceType builder (resource-type-from-map (:resource_type m))))
    (when (contains? m :effective_date)
      (.setEffectiveDate builder (time/protobuf-timestamp (:effective_date m))))
    (.build builder)))

(defn quota-defaults-from-maps
  [ms]
  (map quota-default-from-map ms))

(defn plan-rate-from-map
  [m]
  (let [builder (PlanRate/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (when (contains? m :rate)
      (.setRate builder (:rate m)))
    (when (contains? m :effective_date)
      (.setEffectiveDate builder (time/protobuf-timestamp (:effective_date m))))
    (.build builder)))

(defn plan-rates-from-maps
  [ms]
  (map plan-rate-from-map ms))

(defn plan-from-map
  [m]
  (let [builder (Plan/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (when (contains? m :name)
      (.setName builder (:name m)))
    (when (contains? m :description)
      (.setDescription builder (:description m)))
    (when (contains? m  :plan_quota_defaults)
      (.addAllPlanQuotaDefaults builder (quota-defaults-from-maps (:plan_quota_defaults m))))
    (when (contains? m :plan_rates)
      (.addAllPlanRates builder (plan-rates-from-maps (:plan_rates m))))
    (.build builder)))

(defn usage-from-map
  [m]
  (let [builder (Usage/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (when (contains? m :usage)
      (.setUsage builder (:usage m)))
    (when (contains? m :subscription_id)
      (.setSubscriptionId builder (:subscription_id m)))
    (when (contains? m :resource_type)
      (.setResourceType builder (resource-type-from-map (:resource_type m))))
    (when (contains? m :created_by)
      (.setCreatedBy builder (:created_by m)))
    (when (contains? m :created_at)
      (.setCreatedAt builder (time/protobuf-timestamp (:created_at m))))
    (when (contains? m :last_modified_by)
      (.setLastModifiedBy builder (:last_modified_by m)))
    (when (contains? m :last_modified_at)
      (.setLastModifiedAt builder (time/protobuf-timestamp (:last_modified_at m))))
    (.build builder)))

(defn usages-from-maps
  [m]
  (map usage-from-map (:usages m)))

(defn subscription-from-map
  [m]
  (let [builder (Subscription/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (when (contains? m :effective_start_date)
      (.setEffectiveStartDate builder (time/protobuf-timestamp (:effective_start_date m))))
    (when (contains? m :effective_end_date)
      (.setEffectiveEndDate builder (time/protobuf-timestamp (:effective_end_date m))))
    (when (contains? m :user)
      (.setUser builder (user-from-map (:user m))))
    (when (contains? m :plan)
      (.setPlan builder (plan-from-map (:plan m))))
    (when (contains? m :usages)
      (.addAllUsages builder (usages-from-maps (:usages m))))
    (when (contains? m :paid)
      (.setPaid builder (:paid m)))
    (when (contains? m :plan_rate)
      (.setPlanRate builder (plan-rate-from-map (:plan_rate m))))
    (.build builder)))

(defn subscription-addon-from-map
  [m]
  (let [builder (SubscriptionAddon/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (str (:uuid m))))
    (when (contains? m :addon)
      (.setAddon builder (addon-from-map (:addon m))))
    (when (contains? m :subscription)
      (.setSubscription builder (subscription-from-map (:subscription m))))
    (when (contains? m :amount)
      (.setAmount builder (:amount m)))
    (when (contains? m :paid)
      (.setPaid builder (:paid m)))
    (when (contains? m :addon_rate)
      (.setAddonRate builder (addon-rate-from-map (:addon_rate m))))
    (.build builder)))

(defn update-subscription-addon-request-from-map
  [m]
  (let [builder (UpdateSubscriptionAddonRequest/newBuilder)]
    (when (contains? m :subscription_addon)
      (.setSubscriptionAddon builder (subscription-addon-from-map (:subscription_addon m))))
    (when (contains? m :update_addon_id)
      (.setUpdateAddonId builder (:update_addon_id m)))
    (when (contains? m :update_subscription_id)
      (.setUpdateSubscriptionId builder (:update_subscription_id m)))
    (when (contains? m :update_amount)
      (.setUpdateAmount builder (:update_amount m)))
    (when (contains? m :update_paid)
      (.setUpdatePaid builder (:update_paid m)))
    (.build builder)))
