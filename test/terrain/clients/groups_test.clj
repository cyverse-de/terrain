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

;; Collaborator lists.

(def ^:private cl-folder "de:users:alice:collaborator-lists")
(def ^:private cl-full (str cl-folder ":friends"))

(defn- resolve-route
  "Fake route that resolves the collaborator-list group `friends` to id g1."
  []
  {{:address (groups-url "groups") :query-params {:user "alice" :search cl-full}}
   (json-response {:groups [{:id "g1" :name cl-full :description "buddies"}]})})

(deftest get-collaborator-lists-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups") :query-params {:user "alice" :search cl-folder}}
     (json-response {:groups [{:id "g1" :name cl-full :description "buddies"}]})}
    (let [{:keys [groups]} (groups/get-collaborator-lists "alice" nil)]
      (testing "listing strips the folder prefix and synthesizes contract fields"
        (is (= 1 (count groups)))
        (is (= "friends" (:name (first groups))))
        (is (= "group" (:type (first groups))))
        (is (= "g1" (:id (first groups))))))))

(deftest add-collaborator-list-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups") :query-params {:user "alice"}}
     (fn [req]
       (let [body (json/decode (slurp (:body req)) true)]
         (is (= cl-full (:name body)))
         (is (= "friends" (:display_extension body)))
         {:status 200 :headers {"Content-Type" "application/json"}
          :body (json/encode {:id "g1" :name cl-full :description "buddies" :display_extension "friends"})}))}
    (let [result (groups/add-collaborator-list "alice" {:name "friends" :description "buddies"})]
      (testing "the created list is returned with its short name"
        (is (= "friends" (:name result)))
        (is (= "g1" (:id result)))
        (is (= "group" (:type result)))))))

(deftest get-collaborator-list-members-test
  (with-fake-routes-in-isolation
    (merge (resolve-route)
           {{:address (groups-url "groups" "g1" "members") :query-params {:user "alice"}}
            (json-response {:members [alice {:id "de_grouper" :name "de_grouper"}]})})
    (let [{:keys [members]} (groups/get-collaborator-list-members "alice" "friends")]
      (testing "members exclude the admin user and get a display_name"
        (is (= 1 (count members)))
        (is (= "alice" (:id (first members))))
        (is (= "alice" (:display_name (first members))))))))

(deftest add-collaborator-list-members-test
  (with-fake-routes-in-isolation
    (merge (resolve-route)
           {{:address (groups-url "groups" "g1" "members") :query-params {:user "alice"}}
            (json-response {:results [{:subject_id "bob" :success true}]})
            {:address (groups-url "subjects" "lookup") :query-params {:user "alice"}}
            (json-response {:subjects [{:id "bob" :name "Bob" :source_id "ldap"}]})})
    (let [{:keys [results]} (groups/add-collaborator-list-members "alice" "friends" ["bob"])]
      (testing "membership results are enriched with source_id and subject_name"
        (is (= 1 (count results)))
        (is (= {:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"}
               (first results)))))))

(deftest delete-collaborator-list-test
  (with-fake-routes-in-isolation
    (merge (resolve-route)
           {{:address (groups-url "groups" "g1") :query-params {:user "alice"}}
            (json-response {:id "g1" :name cl-full})})
    (let [result (groups/delete-collaborator-list "alice" "friends")]
      (testing "delete returns the removed group including its id"
        (is (= "g1" (:id result)))
        (is (= "friends" (:name result)))))))

(deftest remove-de-user-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups") :query-params {:user "de_grouper" :search "de:users:de-users"}}
     (json-response {:groups [{:id "du1" :name "de:users:de-users"}]})
     {:address (groups-url "groups" "du1" "members" "bob") :query-params {:user "de_grouper"}}
     (fn [_] {:status 200 :headers {"Content-Type" "application/json"} :body "{}"})}
    (testing "removing a DE user deletes their membership in the de-users group"
      (is (nil? (groups/remove-de-user "bob"))))))
