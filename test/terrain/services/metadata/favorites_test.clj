(ns terrain.services.metadata.favorites-test
  (:require [clojure.test :refer [deftest testing are is]]
            [terrain.services.metadata.favorites :as favorites]))

(def ^:private user-col->api-col #'favorites/user-col->api-col)
(def ^:private user-order->api-order #'favorites/user-order->api-order)

;; Both of these become query parameters on the call to data-info's stat-lister. clj-http renders a
;; keyword with its leading colon, so a keyword here reaches data-info as ":name" and fails schema
;; coercion — the whole favorites listing 400s. They have to be strings.

(deftest sort-params-are-strings-not-keywords
  (testing "every sort column maps onto a string data-info's sort-field enum accepts"
    (are [col expected] (= expected (user-col->api-col col))
      :name         "name"
      :id           "path"
      :lastmodified "datemodified"
      :datecreated  "datecreated"
      :size         "size"
      nil           "name"
      :bogus        "name"))
  (testing "sort direction is a string in the casing data-info expects"
    (are [dir expected] (= expected (user-order->api-order dir))
      :asc  "ASC"
      :desc "DESC"
      nil   "ASC"))
  (testing "nothing leaks a keyword"
    (is (every? string? (map user-col->api-col [:name :id :lastmodified :datecreated :size nil])))
    (is (every? string? (map user-order->api-order [:asc :desc nil])))))
