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
       (some true?)
       not))

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
        (and (contains? m :effective_date) (not= (time/protobuf-timestamp (:effective_date m)) (.getEffectiveDate ar)))]
       (some true?)
       not))

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
        (and (contains? m :addon_rates) (some false? (map addon-rate-equals-map (:addon_rates m) (.getAddonRatesList a))))]
       (some true?)
       not))

(deftest test-addon-from-map
  (doseq [{:keys [m desc]} addon-test-cases]
    (testing desc (is (addon-equals-map m (model/addon-from-map m))))))

(deftestcases add-addon-request-test-cases
  {:addon (:m (first addon-test-cases))})

(defn add-addon-request-equals-map
  [m ar]
  (->> [(and (contains? m :addon) (not (addon-equals-map (:addon m) (.getAddon ar))))]
       (some true?)
       not))

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
       (some true?)
       not))

(deftest test-update-addon-request-from-map
  (doseq [{:keys [m desc]} update-addon-request-test-cases]
    (testing desc (is (update-addon-request-equals-map m (model/update-addon-request-from-map m))))))

(deftestcases by-uuid-request-test-cases
  {:uuid "262EF59B-08DF-4794-B81C-6F8602C54392"})

(defn by-uuid-request-equals-map
  [m bur]
  (->> [(and (contains? m :uuid) (not= (:uuid m) (.getUuid bur)))]
       (some true?)
       not))

(deftest test-by-uuid-request-from-map
  (doseq [{:keys [m desc]} by-uuid-request-test-cases]
    (testing desc (is (by-uuid-request-equals-map m (model/by-uuid-request-from-map m))))))
