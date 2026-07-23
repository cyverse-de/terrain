(ns terrain.clients.qms-addons-test
  (:require [clojure.test :refer [deftest is testing]]
            [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [slingshot.slingshot :refer [try+ throw+]]
            [terrain.clients.qms-addons :as addons]
            [terrain.util.config :as config]))

(def ^:private base "http://subscriptions")

(defn- addons-url [& components]
  (str (apply curl/url base components)))

(deftest add-addon-test
  (let [captured (atom nil)
        addon    {:name "a" :resource_type {:uuid "rt"}}]
    (with-redefs [config/subscriptions-base-uri (constantly base)
                  http/put (fn [url opts]
                             (reset! captured {:url url :opts opts})
                             {:body {:addon (assoc addon :uuid "new")}})]
      (let [result (addons/add-addon addon)]
        (testing "PUTs to /addons"
          (is (= (str base "/addons") (:url @captured))))
        (testing "sends the addon in the request body"
          (is (= {:addon addon} (get-in @captured [:opts :form-params]))))
        (testing "returns the addon payload"
          (is (= {:addon (assoc addon :uuid "new")} result)))))))

(deftest update-addon-masks-test
  (let [captured (atom nil)
        ;; default_paid is present but false: the mask must still be set, so the
        ;; presence check distinguishes "unset" from "false".
        addon    {:uuid "abc" :name "n" :default_paid false}]
    (with-redefs [config/subscriptions-base-uri (constantly base)
                  http/post (fn [url opts]
                              (reset! captured {:url url :opts opts})
                              {:body {:addon addon}})]
      (addons/update-addon addon)
      (let [body (get-in @captured [:opts :form-params])]
        (testing "POSTs to /addons/{uuid}"
          (is (= (str base "/addons/abc") (:url @captured))))
        (testing "sets camelCase update masks only for present fields"
          (is (true? (:updateName body)))
          (is (true? (:updateDefaultPaid body)))
          (is (nil? (:updateDescription body)))
          (is (nil? (:updateDefaultAmount body))))
        (testing "carries the addon object"
          (is (= addon (:addon body))))))))

(deftest update-subscription-addon-masks-test
  (let [captured  (atom nil)
        sub-addon {:uuid "sa" :amount 5.0}]
    (with-redefs [config/subscriptions-base-uri (constantly base)
                  http/post (fn [url opts]
                              (reset! captured {:url url :opts opts})
                              {:body {:subscription_addon sub-addon}})]
      (addons/update-subscription-addon sub-addon)
      (let [body (get-in @captured [:opts :form-params])]
        (testing "reuses the add-on uuid for both path segments"
          (is (= (str base "/subscriptions/sa/addons/sa") (:url @captured))))
        (testing "sets snake_case update masks only for present fields"
          (is (true? (:update_amount body)))
          (is (nil? (:update_paid body))))
        (testing "carries the subscription_addon object"
          (is (= sub-addon (:subscription_addon body))))))))

(deftest add-subscription-addon-test
  (let [captured (atom nil)]
    (with-redefs [config/subscriptions-base-uri (constantly base)
                  http/put (fn [url opts]
                             (reset! captured {:url url :opts opts})
                             {:body {:subscription_addon {:uuid "x"}}})]
      (let [result (addons/add-subscription-addon "sub1" "addon1")]
        (testing "PUTs to /subscriptions/{sub}/addons/{addon} with no body"
          (is (= (str base "/subscriptions/sub1/addons/addon1") (:url @captured)))
          (is (nil? (get-in @captured [:opts :form-params]))))
        (testing "returns the subscription_addon payload"
          (is (= {:subscription_addon {:uuid "x"}} result)))))))

(deftest url-and-payload-test
  (testing "list-addons"
    (let [captured (atom nil)]
      (with-redefs [config/subscriptions-base-uri (constantly base)
                    http/get (fn [url _] (reset! captured url) {:body {:addons [{:uuid "1"}]}})]
        (is (= {:addons [{:uuid "1"}]} (addons/list-addons)))
        (is (= (str base "/addons") @captured)))))
  (testing "delete-addon"
    (let [captured (atom nil)]
      (with-redefs [config/subscriptions-base-uri (constantly base)
                    http/delete (fn [url _] (reset! captured url) {:body {:addon {:uuid "d"}}})]
        (is (= {:addon {:uuid "d"}} (addons/delete-addon "d")))
        (is (= (str base "/addons/d") @captured)))))
  (testing "list-subscription-addons"
    (let [captured (atom nil)]
      (with-redefs [config/subscriptions-base-uri (constantly base)
                    http/get (fn [url _] (reset! captured url) {:body {:subscription_addons []}})]
        (is (= {:subscription_addons []} (addons/list-subscription-addons "sub9")))
        (is (= (str base "/subscriptions/sub9/addons") @captured)))))
  (testing "get-subscription-addon reuses the add-on uuid for both segments"
    (let [captured (atom nil)]
      (with-redefs [config/subscriptions-base-uri (constantly base)
                    http/get (fn [url _] (reset! captured url) {:body {:subscription_addon {:uuid "g"}}})]
        (is (= {:subscription_addon {:uuid "g"}} (addons/get-subscription-addon "g")))
        (is (= (str base "/subscriptions/g/addons/g") @captured)))))
  (testing "delete-subscription-addon reuses the add-on uuid for both segments"
    (let [captured (atom nil)]
      (with-redefs [config/subscriptions-base-uri (constantly base)
                    http/delete (fn [url _] (reset! captured url) {:body {:subscription_addon {:uuid "z"}}})]
        (is (= {:subscription_addon {:uuid "z"}} (addons/delete-subscription-addon "z")))
        (is (= (str base "/subscriptions/z/addons/z") @captured))))))

;; The tests above capture the pre-serialization map via with-redefs. These two
;; exercise the real clj-http request pipeline so the on-the-wire JSON is
;; checked — java.util.UUID encoding and the update-mask key casing.
(deftest add-addon-serializes-request-over-the-wire
  (let [captured (atom nil)
        rt-uuid  (java.util.UUID/fromString "45DD6319-219B-4EB1-A792-024AE323588E")]
    (with-redefs [config/subscriptions-base-uri (constantly base)]
      (with-fake-routes-in-isolation
        {(addons-url "addons")
         (fn [req]
           (reset! captured req)
           {:status  200
            :headers {"Content-Type" "application/json"}
            :body    (json/generate-string {:addon {:name "a"}})})}
        (addons/add-addon {:name "a" :resource_type {:uuid rt-uuid}})))
    (let [sent (json/parse-string (slurp (:body @captured)) true)]
      (testing "issues a PUT"
        (is (= :put (:request-method @captured))))
      (testing "serializes a java.util.UUID as its string form"
        (is (= (str rt-uuid) (get-in sent [:addon :resource_type :uuid])))))))

(deftest update-addon-sends-camelcase-masks-over-the-wire
  (let [captured (atom nil)
        uuid     (java.util.UUID/fromString "262EF59B-08DF-4794-B81C-6F8602C54392")]
    (with-redefs [config/subscriptions-base-uri (constantly base)]
      (with-fake-routes-in-isolation
        {(addons-url "addons" (str uuid))
         (fn [req]
           (reset! captured req)
           {:status  200
            :headers {"Content-Type" "application/json"}
            :body    (json/generate-string {:addon {}})})}
        (addons/update-addon {:uuid uuid :name "new name"})))
    (let [sent (json/parse-string (slurp (:body @captured)) true)]
      (testing "posts to /addons/{uuid}"
        (is (= :post (:request-method @captured))))
      (testing "sets only the camelCase mask for the changed field"
        (is (true? (:updateName sent)))
        (is (not (contains? sent :updateDescription)))))))

(deftest error-handling-test
  (testing "maps a non-2xx response to a thrown error_code/reason from the body"
    (with-redefs [config/subscriptions-base-uri (constantly base)
                  http/get (fn [_ _]
                             (throw+ {:status 404
                                      :body   (json/generate-string
                                               {:error {:error_code 3 :message "add-on not found"}})}))]
      (try+
       (addons/list-addons)
       (is false "expected the client to throw")
       (catch map? e
         (is (= 3 (:error_code e)))
         (is (= "add-on not found" (:reason e))))))))
