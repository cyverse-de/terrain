(ns terrain.clients.qms-test
  (:require [clojure.test :refer [deftest is testing]]
            [kameleon.uuids :refer [uuidify]]
            [terrain.clients.qms :as qms]))

(def ^:private normalize-addon #'qms/normalize-addon)
(def ^:private addon-update-flags #'qms/addon-update-flags)

(def ^:private addon-uuid (uuidify "45DD6319-219B-4EB1-A792-024AE323588E"))

(deftest normalize-addon-test
  (let [addon {:uuid           addon-uuid
               :name           "some.addon"
               :resource_type  {:uuid addon-uuid}
               :addon_rates    [{:uuid           addon-uuid
                                 :rate           2.5
                                 :effective_date "2024-05-15T12:34:56.789Z"}]}
        testcases [{:desc     "uuids become strings and dates become RFC 3339"
                    :in       addon
                    :expected {:uuid           (str addon-uuid)
                               :name           "some.addon"
                               :resource_type  {:uuid (str addon-uuid)}
                               :addon_rates    [{:uuid           (str addon-uuid)
                                                 :rate           2.5
                                                 :effective_date "2024-05-15T12:34:56.789Z"}]}}
                   {:desc     "offset timestamps are normalized to UTC"
                    :in       {:addon_rates [{:effective_date "2024-05-15T05:34:56.789-07:00"}]}
                    :expected {:addon_rates [{:effective_date "2024-05-15T12:34:56.789Z"}]}}
                   {:desc     "absent fields stay absent"
                    :in       {:name "some.addon"}
                    :expected {:name "some.addon"}}]]
    (doseq [{:keys [desc in expected]} testcases]
      (testing desc
        (is (= expected (normalize-addon in)))))))

(deftest addon-update-flags-test
  (let [testcases [{:desc     "all fields present"
                    :in       {:name           "n"
                               :description    "d"
                               :resource_type  {:uuid addon-uuid}
                               :default_amount 1.0
                               :default_paid   false
                               :addon_rates    []}
                    :expected {:updateName          true
                               :updateDescription   true
                               :updateResourceType  true
                               :updateDefaultAmount true
                               :updateDefaultPaid   true
                               :updateAddonRates    true}}
                   {:desc     "false and empty values still count as updates"
                    :in       {:default_paid false :addon_rates []}
                    :expected {:updateDefaultPaid true :updateAddonRates true}}
                   {:desc     "absent fields produce no flags"
                    :in       {:uuid addon-uuid}
                    :expected {}}]]
    (doseq [{:keys [desc in expected]} testcases]
      (testing desc
        (is (= expected (addon-update-flags in)))))))
