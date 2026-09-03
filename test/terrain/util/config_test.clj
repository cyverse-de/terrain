(ns terrain.util.config-test
  (:require [clojure.test :refer [deftest is testing]]
            [terrain.util.config :as config]))

(deftest valid-groups-backend-test
  (doseq [[backend expected] [["iplant-groups" true]
                              ["groups"        true]
                              ["Groups"        false]
                              ["groups "       false]
                              [""              false]
                              [nil             false]]]
    (testing (str "the backend selector " (pr-str backend) " is recognized: " expected)
      (is (= expected (config/valid-groups-backend? backend))))))
