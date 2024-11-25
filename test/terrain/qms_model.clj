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
