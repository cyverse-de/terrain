(ns terrain.qms-model
  (:require [clojure.test :refer [deftest is testing]]
            [terrain.clients.qms.model :as model]
            [terrain.util.time :as time]))

(defn gen-testcases
  [m]
  (concat
   [{:m m  :desc "fully specified"}
    {:m {} :desc "empty"}]
   (for [[f _] m] {:m (dissoc m f) :desc (str f " not specified")})))

(defmacro deftestcases
  [sym m]
  `(def ~sym (gen-testcases ~m)))

(defn dates-equal
  [m-date pb-date]
  (= (time/protobuf-timestamp m-date) pb-date))

(deftestcases resource-type-test-cases
  {:uuid       "45DD6319-219B-4EB1-A792-024AE323588E"
   :name       "some.resource.type"
   :unit       "some.unit"
   :consumable true})

(defn resource-type-equals-map
  [m rt]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid rt)))
        (and (contains? m :name) (not= (:name m) (.getName rt)))
        (and (contains? m :unit) (not= (:unit m) (.getUnit rt)))
        (and (contains? m :consumable) (not= (:consumable m) (.getConsumable rt)))]
       (every? false?)))

(deftest test-resource-type-from-map
  (doseq [{:keys [m desc]} resource-type-test-cases]
    (testing desc (is (resource-type-equals-map m (model/resource-type-from-map m))))))

(deftestcases addon-rate-test-cases
  {:uuid           "2D331892-992A-4895-9FA3-A5F4638099B2"
   :rate           123.45
   :effective_date "2024-11-22T14:33:00-07:00"})

(defn addon-rate-equals-map
  [m ar]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid ar)))
        (and (contains? m :rate) (not= (:rate m) (.getRate ar)))
        (and (contains? m :effective_date) (not (dates-equal (:effective_date m) (.getEffectiveDate ar))))]
       (every? false?)))

(defn addon-rates-equal-maps
  [ms ars]
  (every? true? (map addon-rate-equals-map ms ars)))

(deftest test-addon-rate-from-map
  (doseq [{:keys [m desc]} addon-rate-test-cases]
    (testing desc (is (addon-rate-equals-map m (model/addon-rate-from-map m))))))

(deftestcases addon-test-cases
  {:uuid           "C8EDAB3A-454B-4337-AD82-3AB5EF47B3B5"
   :name           "some-addon"
   :description    "Some Addon"
   :resource_type  (:m (first resource-type-test-cases))
   :default_amount 123.45
   :default_paid   true
   :addon_rates    (mapv :m addon-rate-test-cases)})

(defn addon-equals-map
  [m a]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid a)))
        (and (contains? m :name) (not= (:name m) (.getName a)))
        (and (contains? m :description)  (not= (:description m) (.getDescription a)))
        (and (contains? m :resource_type) (not= (model/resource-type-from-map (:resource_type m)) (.getResourceType a)))
        (and (contains? m :default_amount) (not= (:default_amount m) (.getDefaultAmount a)))
        (and (contains? m :default_paid) (not= (:default_paid m) (.getDefaultPaid a)))
        (and (contains? m :addon_rates) (not (addon-rates-equal-maps (:addon_rates m) (.getAddonRatesList a))))]
       (every? false?)))

(deftest test-addon-from-map
  (doseq [{:keys [m desc]} addon-test-cases]
    (testing desc (is (addon-equals-map m (model/addon-from-map m))))))

(deftestcases add-addon-request-test-cases
  {:addon (:m (first addon-test-cases))})

(defn add-addon-request-equals-map
  [m ar]
  (->> [(and (contains? m :addon) (not (addon-equals-map (:addon m) (.getAddon ar))))]
       (every? false?)))

(deftest test-add-addon-request-from-map
  (doseq [{:keys [m desc]} add-addon-request-test-cases]
    (testing desc (is (add-addon-request-equals-map m (model/add-addon-request-from-map m))))))

(deftestcases update-addon-request-test-cases
  {:addon                 (:m (first addon-test-cases))
   :update_name           true
   :update_description    true
   :update_resource_type  true
   :update_default_amount true
   :update_default_paid   true
   :update_addon_rates    true})

(defn update-addon-request-equals-map
  [m uar]
  (->> [(and (contains? m :addon) (not (addon-equals-map (:addon m) (.getAddon uar))))
        (and (contains? m :update_name) (not= (:update_name m) (.getUpdateName uar)))
        (and (contains? m :update_description) (not= (:update_description m) (.getUpdateDescription uar)))
        (and (contains? m :update_resource_type) (not= (:update_resource_type m) (.getUpdateResourceType uar)))
        (and (contains? m :update_default_amount) (not= (:update_default_amount m) (.getUpdateDefaultAmount uar)))
        (and (contains? m :update_default_paid) (not= (:update_default_paid m) (.getUpdateDefaultPaid uar)))
        (and (contains? m :update_addon_rates) (not= (:update_addon_rates m) (.getUpdateAddonRates uar)))]
       (every? false?)))

(deftest test-update-addon-request-from-map
  (doseq [{:keys [m desc]} update-addon-request-test-cases]
    (testing desc (is (update-addon-request-equals-map m (model/update-addon-request-from-map m))))))

(deftestcases by-uuid-request-test-cases
  {:uuid "262EF59B-08DF-4794-B81C-6F8602C54392"})

(defn by-uuid-request-equals-map
  [m bur]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid bur)))]
       (every? false?)))

(deftest test-by-uuid-request-from-map
  (doseq [{:keys [m desc]} by-uuid-request-test-cases]
    (testing desc (is (by-uuid-request-equals-map m (model/by-uuid-request-from-map m))))))

(deftestcases associate-by-uuids-test-cases
  {:parent_uuid "0C956469-70E3-4ECF-8B62-469FFC518A9E"
   :child_uuid  "328B5F0B-C65B-4173-AE19-E31A7586A733"})

(defn associate-by-uuids-request-equals-map
  [m abur]
  (->> [(and (contains? m :parent_uuid) (not= (:parent_uuid m) (.getParentUuid abur)))
        (and (contains? m :child_uuid) (not= (:child_uuid m) (.getChildUuid abur)))]
       (every? false?)))

(deftest test-associate-by-uuids-request-from-map
  (doseq [{:keys [m desc]} associate-by-uuids-test-cases]
    (testing desc (is (associate-by-uuids-request-equals-map m (model/associate-by-uuids-from-map m))))))

(deftestcases user-test-cases
  {:uuid     "E90FC296-EE37-406D-BB1A-BACEBAA71679"
   :username "someuser"})

(defn user-equals-map
  [m u]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid u)))
        (and (contains? m :username) (not= (:username m) (.getUsername u)))]))

(deftest test-user-from-map
  (doseq [{:keys [m desc]} user-test-cases]
    (testing desc (is (user-equals-map m (model/user-from-map m))))))

(deftestcases quota-default-test-cases
  {:uuid           "3E6004AA-6637-4D99-A069-145258D5A136"
   :quota_value    123.45
   :resource_type  (:m (first resource-type-test-cases))
   :effective_date "2024-11-27T15:20:00-07:00"})

(defn quota-default-equals-map
  [m qd]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid qd)))
        (and (contains? m :quota_value) (not= (:quota_value m) (.getQuotaValue qd)))
        (and (contains? m :resource_type) (not (resource-type-equals-map (:resource_type m) (.getResourceType qd))))
        (and (contains? m :effective_date) (not (dates-equal (:effective_date m) (.getEffectiveDate qd))))]
       (every? false?)))

(defn quota-defaults-equal-maps
  [ms qds]
  (every? true? (map quota-default-equals-map ms qds)))

(deftest test-quota-default-from-map
  (doseq [{:keys [m desc]} quota-default-test-cases]
    (testing desc (is (quota-default-equals-map m (model/quota-default-from-map m))))))

(deftestcases plan-rate-test-cases
  {:uuid           "36F7C33A-2175-4C3E-812E-3CA5AC984988"
   :rate           123.45
   :effective_date "2024-11-27T15:37:00-07:00"})

(defn plan-rate-equals-map
  [m pr]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid pr)))
        (and (contains? m :quota_value) (not= (:quota_value m) (.getQuotaValue pr)))
        (and (contains? m :resource_type) (not (resource-type-equals-map (:resource_type m) (.getResourceType pr))))
        (and (contains? m :effective_date) (not (dates-equal (:effective_date m) (.getEffectiveDate pr))))]
       (every? false?)))

(defn plan-rates-equal-maps
  [ms prs]
  (every? true? (mapv plan-rate-equals-map ms prs)))

(deftest test-plan-rate-from-map
  (doseq [{:keys [m desc]} plan-rate-test-cases]
    (testing desc (is (plan-rate-equals-map m (model/plan-rate-from-map m))))))

(deftestcases plan-test-cases
  {:uuid                "60D18DE6-796E-4F7A-932D-017986D1DF25"
   :name                "Plan"
   :description         "The most generic plan imaginable"
   :plan_quota_defaults (map :m quota-default-test-cases)
   :plan_rates          (map :m plan-rate-test-cases)})

(defn plan-equals-map
  [m p]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid p)))
        (and (contains? m :name) (not= (:name m) (.getName p)))
        (and (contains? m :description) (not= (:description m) (.getDescription p)))
        (and (contains? m :plan_quota_defaults)
             (not (quota-defaults-equal-maps (:plan_quota_defaults m) (.getPlanQuotaDefaultsList p))))
        (and (contains? m :plan_rates) (not (plan-rates-equal-maps (:plan_rates m) (.getPlanRatesList p))))]
       (every? false?)))

(deftest test-plan-from-map
  (doseq [{:keys [m desc]} plan-test-cases]
    (testing desc (is (plan-equals-map m (model/plan-from-map m))))))

(deftestcases usage-test-cases
  {:uuid             "DCF04FA0-C615-4C26-99ED-65D98AA37CC7"
   :usage            123.45
   :subscription_id  "D693F7B6-E0B9-4444-AA36-E2B62699F9B2"
   :resource_type    (:m (first resource-type-test-cases))
   :created_by       "someuser"
   :created_at       "2024-11-27T16:46:00-07:00"
   :last_modified_by "someuser"
   :last_modified_at "2024-11-27T16:46:00-07:00"})

(defn usage-equals-map
  [m u]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid u)))
        (and (contains? m :usage) (not= (:usage m) (.getUsage u)))
        (and (contains? m :subscription_id) (not= (:subscription_id m) (.getSubscriptionId u)))
        (and (contains? m :resource_type) (not (resource-type-equals-map (:resource_type m) (.getResourceType u))))
        (and (contains? m :created_by) (not= (:created_by m) (.getCreatedBy u)))
        (and (contains? m :created_at) (not (dates-equal (:created_at m) (.getCreatedAt u))))
        (and (contains? m :last_modified_by) (not= (:last_modified_by m) (.getLastModifiedBy u)))
        (and (contains? m :last_modified_at) (not (dates-equal (:last_modified_at m) (.getLastModifiedAt u))))]
       (every? false?)))

(defn usages-equal-maps
  [ms us]
  (every? true? (map usage-equals-map ms us)))

(deftest test-usage-from-map
  (doseq [{:keys [m desc]} usage-test-cases]
    (testing desc (is (usage-equals-map m (model/usage-from-map m))))))

(deftestcases subscription-test-cases
  {:uuid                 "BC7CAE34-D955-486E-910F-339BD3FB6A42"
   :effective_start_date "2024-11-27T17:08:00-07:00"
   :effective_end_date   "2025-11-27T17:08:00-07:00"
   :user                 (:m (first user-test-cases))
   :plan                 (:m (first plan-test-cases))
   :usages               (map :m usage-test-cases)
   :paid                 true
   :plan_rate            (:m (first plan-rate-test-cases))})

(defn subscription-equals-map
  [m s]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid s)))
        (and (contains? m :effective_start_date) (not (dates-equal (:effective_start_date m) (.getEffectiveStartDate s))))
        (and (contains? m :effective_end_date) (not (dates-equal (:effective_end_date m) (.getEffectiveStartDate s))))
        (and (contains? m :user) (not (user-equals-map (:user m) (.getUser s))))
        (and (contains? m :plan) (not (plan-equals-map (:plan m) (.getPlan s))))
        (and (contains? m :usages) (not (usages-equal-maps (:usages m) (.getUsagesList s))))
        (and (contains? m :paid) (not= (:paid m) (.getPaid s)))
        (and (contains? m :plan_rate) (not (plan-rate-equals-map (:plan_rate m) (.getPlanRate s))))]
       (some false?)))

(deftest test-subscription-from-map
  (doseq [{:keys [m desc]} subscription-test-cases]
    (testing desc (is (subscription-equals-map m (model/subscription-from-map m))))))

(deftestcases subscription-addon-test-cases
  {:uuid         "6D8EAC2D-B6F8-4851-8AC8-6C24A6939381"
   :addon        (:m (first addon-test-cases))
   :subscription (:m (first subscription-test-cases))
   :amount       123.45
   :paid         true
   :addon_rate   (:m (first addon-rate-test-cases))})

(defn subscription-addon-equals-map
  [m sa]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid sa)))
        (and (contains? m :addon) (not (addon-equals-map (:addon m) (.getAddon sa))))
        (and (contains? m :subscription) (not (subscription-equals-map (:subscription m) (.getSubscription sa))))
        (and (contains? m :amount) (not= (:amount m) (.getAmount sa)))
        (and (contains? m :paid) (not= (:paid m) (.getPaid sa)))
        (and (contains? m :addon_rate) (not (addon-rate-equals-map (:addon_rate m) (.getAddonRate sa))))]
       (every? false?)))

(deftest test-subscription-addon-from-map
  (doseq [{:keys [m desc]} subscription-addon-test-cases]
    (testing desc (is (subscription-addon-equals-map m (model/subscription-addon-from-map m))))))

(deftestcases update-subscription-addon-update-request-test-cases
  {:subscription_addon     (:m (first subscription-addon-test-cases))
   :update_addon_id        true
   :update_subscription_id true
   :update_amount          true
   :update_paid            true})

(defn update-subscription-addon-update-request-equals-map
  [m saur]
  (->> [(and (contains? m :subscription_addon)
             (not (subscription-addon-equals-map (:subscription_addon m) (.getSubscriptionAddon saur))))
        (and (contains? m :update_addon_id) (not= (:update_addon_id m) (.getUpdateAddonId saur)))
        (and (contains? m :update_subscription_id) (not= (:update_subscription_id m) (.getUpdateSubscriptionId saur)))
        (and (contains? m :update_amount) (not= (:update_amount m) (.getUpdateAmount saur)))
        (and (contains? m :update_paid) (not= (:update_paid m) (.getUpdatePaid saur)))]
       (every? false?)))

(deftest test-subscription-addon-update-request-from-map
  (doseq [{:keys [m desc]} update-subscription-addon-update-request-test-cases]
    (let [saur (model/update-subscription-addon-request-from-map m)]
      (testing desc (is (update-subscription-addon-update-request-equals-map m saur))))))
