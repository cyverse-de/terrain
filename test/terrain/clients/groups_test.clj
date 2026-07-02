(ns terrain.clients.groups-test
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.test :refer :all]
            [terrain.clients.groups :as groups]
            [terrain.test-fixtures :as test-fixtures]
            [terrain.util.config :as config]))

(use-fixtures :once test-fixtures/with-test-config test-fixtures/with-test-user)

(defn- groups-url [& components]
  (str (apply curl/url (config/groups-base) components)))

(defn- json-response [body]
  (fn [_] {:status 200 :headers {"Content-Type" "application/json"} :body (json/encode body)}))

(def ^:private alice
  {:id "alice" :name "alice" :first_name "Alice" :last_name "Anderson"
   :email "alice@example.org" :institution "CyVerse" :source_id "ldap"})

(deftest find-subjects-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "subjects") :query-params {:user "ipcdev" :search "ali"}}
     (json-response {:subjects [alice {:id "de_grouper" :name "de_grouper"}]})}
    (let [{:keys [subjects]} (groups/find-subjects "ipcdev" "ali")]
      (testing "the administrative user is filtered out of the results"
        (is (= 1 (count subjects)))
        (is (= "alice" (:id (first subjects)))))
      (testing "each subject gets a display_name equal to its name"
        (is (= "alice" (:display_name (first subjects))))))))

(deftest lookup-subject-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "subjects" "alice") :query-params {:user "de_grouper"}}
     (json-response alice)}
    (testing "a found subject is returned as-is"
      (is (= alice (groups/lookup-subject "de_grouper" "alice")))))
  (with-fake-routes-in-isolation
    {{:address (groups-url "subjects" "nobody") :query-params {:user "de_grouper"}}
     (fn [_] {:status 404 :headers {"Content-Type" "application/json"} :body "{}"})}
    (testing "a missing subject yields nil rather than an error"
      (is (nil? (groups/lookup-subject "de_grouper" "nobody"))))))

(deftest lookup-subjects-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "subjects" "lookup") :query-params {:user "de_grouper"}}
     (json-response {:subjects [alice]})}
    (testing "bulk lookup returns the subjects list"
      (is (= [alice] (:subjects (groups/lookup-subjects "de_grouper" ["alice" "ghost"])))))))

(deftest lookup-subject-add-empty-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "subjects" "ghost") :query-params {:user "de_grouper"}}
     (fn [_] {:status 404 :headers {"Content-Type" "application/json"} :body "{}"})}
    (testing "a missing subject yields an empty user-info block keyed by the username"
      (is (= {:id "ghost" :name "" :first_name "" :last_name "" :email "" :institution "" :source_id ""}
             (groups/lookup-subject-add-empty "de_grouper" "ghost"))))))

(deftest list-groups-for-user-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "subjects" "bob" "groups") :query-params {:user "de_grouper"}}
     (json-response {:groups [{:id "uuid-1" :name "de:teams:bob:my-team" :description "d"}]})}
    (let [{:keys [groups]} (groups/list-groups-for-user "bob" nil)]
      (testing "the subject's groups are returned with contract-required fields synthesized"
        (is (= 1 (count groups)))
        (is (= "group" (:type (first groups))))
        (is (contains? (first groups) :id_index))
        (is (= "uuid-1" (:id (first groups))))))))
