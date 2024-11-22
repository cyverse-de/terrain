(ns terrain.clients.qms.model
  (:require [terrain.util.time :as time])
  (:import [org.cyverse.de.protobufs
            AddAddonRequest
            Addon
            AddonRate
            AssociateByUuids
            ByUuid
            NoParamsRequest
            ResourceType
            SubscriptionAddon
            UpdateAddonRequest
            UpdateSubscriptionAddonRequest]))

(defn resource-type-from-map
  [m]
  (let [builder (ResourceType/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (:uuid m)))
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
      (.setUuid builder (:uuid m)))
    (when (contains? m :rate)
      (.setUuid builder (:rate m)))
    (when (contains? m :effective_date)
      (.setEffectiveDate builder (time/protobuf-timestamp (:effective_date m))))
    (.build builder)))

(defn addon-from-map
  [m]
  (let [builder (Addon/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (:uuid m)))
    (when (contains? m :name)
      (.setName builder (:name m)))
    (when (contains? m :description)
      (.setDescription builder (:description m)))
    (when (contains? m :resource_type)
      (.setResourceType builder (resource-type-from-map (:resource_type m))))
    (when (contains? m :default_amount)
      (.setDefaultAmount builder (:default_amount m)))
    (when (contains? m :default_paid)
      (.setDefaultPaaid builder (:default_paid m)))
    (when (contains? m :addon_rates)
      (.setAddonRates builder (into-array AddonRate (map addon-rate-from-map (:addon-rates m)))))
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
  (let [builder (ByUuid/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (:uuid m)))))

(defn associate-by-uuids-from-map
  [m]
  (let [builder (AssociateByUuids/newBuilder)]
    (when (contains? m :parent_uuid)
      (.setParentUuid builder (:parent_uuid m)))
    (when (contains? m :child_uuid)
      (.setChildUuid builder (:child_uuid m)))))

(defn subscription-addon-from-map
  [m]
  (let [builder (SubscriptionAddon/newBuilder)]
    (when (contains? m :uuid)
      (.setUuid builder (:uuid m)))
    (when (contains? m :addon)
      (.setAddon builder (addon-from-map (:addon m))))
    ;; TODO: finish implementing this function.
    ))

(defn update-subscription-addon-request-from-map
  [m]
  (let [builder (UpdateSubscriptionAddonRequest/newBuilder)]
    (when (contains? m :subscription_addon)
      (.setSubscriptionAddon builder (subscription-addon-from-map (:subscription_addon m))))
    ;; TODO: finish implementing this function
    ))
