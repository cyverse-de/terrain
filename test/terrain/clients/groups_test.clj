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

(deftest get-collaborator-list-test
  ;; Only the resolving search is registered: if get-collaborator-list also fetched the
  ;; group by UUID, with-fake-routes-in-isolation would throw on the unregistered route.
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups") :query-params {:user "alice" :search cl-full}}
     (json-response {:groups [{:id "g1" :name cl-full :description "buddies"}]})}
    (let [result (groups/get-collaborator-list "alice" "friends")]
      (testing "get resolves and returns the list in a single round trip"
        (is (= "friends" (:name result)))
        (is (= "g1" (:id result)))))))

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
  ;; No subjects/lookup route is registered: the members response now carries source_id and
  ;; subject_name directly, so any enrichment lookup would fail the isolated fake routes.
  (with-fake-routes-in-isolation
    (merge (resolve-route)
           {{:address (groups-url "groups" "g1" "members") :query-params {:user "alice"}}
            (json-response {:results [{:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"}
                                      {:subject_id "carol" :success true :source_id "" :subject_name ""}]})})
    (let [{:keys [results]} (groups/add-collaborator-list-members "alice" "friends" ["bob" "carol"])
          by-id             (into {} (map (juxt :subject_id identity)) results)]
      (testing "membership results pass through source_id and subject_name from the service"
        (is (= {:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"}
               (get by-id "bob"))))
      (testing "a blank source_id or subject_name is defaulted to satisfy the schema"
        (is (= {:subject_id "carol" :success true :source_id "unknown" :subject_name "carol"}
               (get by-id "carol")))))))

(deftest delete-collaborator-list-test
  (with-fake-routes-in-isolation
    (merge (resolve-route)
           {{:address (groups-url "groups" "g1") :query-params {:user "alice"}}
            (json-response {:id "g1" :name cl-full})})
    (let [result (groups/delete-collaborator-list "alice" "friends")]
      (testing "delete returns the removed group including its id"
        (is (= "g1" (:id result)))
        (is (= "friends" (:name result)))))))

;; Teams.

(def ^:private team-full "de:teams:alice:t1")

(defn- team-resolve-route []
  {{:address (groups-url "groups") :query-params {:user "alice" :search team-full}}
   (json-response {:groups [{:id "t1" :name team-full :description "d"}]})})

(deftest add-team-test
  (testing "a public team grants the all-users subject read and returns the creator-scoped name"
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups") :query-params {:user "alice"}}
       (fn [req]
         (let [body (json/decode (slurp (:body req)) true)]
           (is (= "de:teams:alice:t1" (:name body)))
           {:status 200 :headers {"Content-Type" "application/json"}
            :body (json/encode {:id "t1" :name "de:teams:alice:t1" :description "d" :display_extension "t1"})}))
       {:address (groups-url "groups" "t1" "permissions" "group" "GrouperAll") :query-params {:user "alice"}}
       (json-response {:subject {:subject_id "GrouperAll" :subject_type "group"} :level "read"})}
      (let [result (groups/add-team "alice" {:name "t1" :description "d" :public_privileges ["view"]})]
        (is (= "alice:t1" (:name result)))
        (is (= "t1" (:id result))))))
  (testing "a non-public team makes no permission grant"
    ;; Only the create route is registered; a permission PUT would fail the isolated routes.
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups") :query-params {:user "alice"}}
       (json-response {:id "t1" :name "de:teams:alice:t1" :display_extension "t1"})}
      (is (= "alice:t1" (:name (groups/add-team "alice" {:name "t1" :description "d"})))))))

(deftest get-team-test
  (with-fake-routes-in-isolation
    (team-resolve-route)
    (let [result (groups/get-team "alice" "alice:t1")]
      (testing "get resolves the team in a single round trip"
        (is (= "alice:t1" (:name result)))
        (is (= "t1" (:id result)))))))

(deftest get-teams-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups") :query-params {:user "alice" :search "de:teams"}}
     (json-response {:groups [{:id "t1" :name team-full :description "d"}]})}
    (let [{:keys [groups]} (groups/get-teams "alice" {})]
      (testing "listing strips the team folder prefix"
        (is (= ["alice:t1"] (mapv :name groups)))
        (is (= "group" (:type (first groups))))))))

(deftest delete-team-test
  (with-fake-routes-in-isolation
    (merge (team-resolve-route)
           {{:address (groups-url "groups" "t1") :query-params {:user "alice"}}
            (json-response {:id "t1" :name team-full})})
    (is (= "t1" (:id (groups/delete-team "alice" "alice:t1"))))))

(deftest add-team-members-test
  (with-fake-routes-in-isolation
    (merge (team-resolve-route)
           {{:address (groups-url "groups" "t1" "members") :query-params {:user "alice"}}
            (json-response {:results [{:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"}]})})
    (let [{:keys [results]} (groups/add-team-members "alice" "alice:t1" ["bob"])]
      (testing "members are added and results pass through source_id/subject_name"
        (is (= {:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"} (first results)))))))

(deftest join-team-test
  (with-fake-routes-in-isolation
    (merge (team-resolve-route)
           {{:address (groups-url "groups" "t1" "permissions") :query-params {:user "alice"}}
            (json-response {:permissions [{:subject {:subject_id "GrouperAll" :subject_type "group"} :level "read"}]})
            {:address (groups-url "groups" "t1" "members") :query-params {:user "de_grouper"}}
            (json-response {:results [{:subject_id "alice" :success true :source_id "ldap" :subject_name "Alice"}]})})
    (let [{:keys [results]} (groups/join-team "alice" "alice:t1")]
      (testing "joining a public team adds the caller as a member"
        (is (= "alice" (:subject_id (first results))))
        (is (true? (:success (first results))))))))

(deftest leave-team-test
  (with-fake-routes-in-isolation
    (merge (team-resolve-route)
           {{:address (groups-url "groups" "t1" "members" "deleter") :query-params {:user "de_grouper"}}
            (json-response {:results [{:subject_id "alice" :success true :source_id "ldap" :subject_name "Alice"}]})})
    (let [{:keys [results]} (groups/leave-team "alice" "alice:t1")]
      (testing "leaving removes the caller from the team"
        (is (= "alice" (:subject_id (first results))))))))

(deftest list-team-privileges-test
  (with-fake-routes-in-isolation
    (merge (team-resolve-route)
           {{:address (groups-url "groups" "t1" "permissions") :query-params {:user "alice"}}
            (json-response {:permissions [{:subject {:subject_id "alice" :subject_type "user"} :level "own"}
                                          {:subject {:subject_id "GrouperAll" :subject_type "group"} :level "read"}
                                          {:subject {:subject_id "bob" :subject_type "user"} :level "read"}]})
            {:address (groups-url "subjects" "lookup") :query-params {:user "alice"}}
            (json-response {:subjects [{:id "alice" :name "Alice" :source_id "ldap"}
                                       {:id "bob" :name "Bob" :source_id "ldap"}]})})
    (let [privs (:privileges (groups/list-team-privileges "alice" "alice:t1"))
          by-subject (into {} (map (juxt (comp :id :subject) identity)) privs)]
      (testing "own/admin levels map to admin, write/read to read, and the public subject to view"
        (is (= "admin" (:name (get by-subject "alice"))))
        (is (= "read" (:name (get by-subject "bob"))))
        (is (= "view" (:name (get by-subject "GrouperAll")))))
      (testing "the public subject is surfaced as a group subject"
        (is (= "g:gsa" (:source_id (:subject (get by-subject "GrouperAll")))))))))

(deftest update-team-privileges-test
  (with-fake-routes-in-isolation
    (merge (team-resolve-route)
           {{:address (groups-url "groups" "t1" "permissions" "user" "bob") :query-params {:user "alice"}}
            (fn [req]
              (is (= "admin" (:level (json/decode (slurp (:body req)) true))))
              {:status 200 :headers {"Content-Type" "application/json"} :body "{}"})
            {:address (groups-url "groups" "t1" "permissions" "group" "GrouperAll") :query-params {:user "alice"}}
            (fn [_] {:status 200 :headers {"Content-Type" "application/json"} :body "{}"})
            {:address (groups-url "groups" "t1" "permissions") :query-params {:user "alice"}}
            (json-response {:permissions [{:subject {:subject_id "bob" :subject_type "user"} :level "admin"}]})
            {:address (groups-url "subjects" "lookup") :query-params {:user "alice"}}
            (json-response {:subjects [{:id "bob" :name "Bob" :source_id "ldap"}]})})
    (let [result (groups/update-team-privileges
                  "alice" "alice:t1"
                  {:updates [{:subject_id "bob" :privileges ["admin"]}
                             {:subject_id "GrouperAll" :privileges []}]})]
      (testing "privilege names are translated to levels (admin grant) and empty privileges revoke"
        (is (= "admin" (:name (first (:privileges result)))))))))

(deftest get-team-admins-test
  (with-fake-routes-in-isolation
    (merge (team-resolve-route)
           {{:address (groups-url "groups" "t1" "permissions") :query-params {:user "alice"}}
            (json-response {:permissions [{:subject {:subject_id "alice" :subject_type "user"} :level "own"}
                                          {:subject {:subject_id "GrouperAll" :subject_type "group"} :level "read"}
                                          {:subject {:subject_id "de_grouper" :subject_type "user"} :level "admin"}]})
            {:address (groups-url "subjects" "lookup") :query-params {:user "alice"}}
            (json-response {:subjects [{:id "alice" :name "Alice" :email "a@x" :source_id "ldap"}]})})
    (let [{:keys [members]} (groups/get-team-admins "alice" "alice:t1")]
      (testing "admins are the own/admin user subjects, excluding the service user and public subject"
        (is (= 1 (count members)))
        (is (= "alice" (:id (first members))))
        (is (= "a@x" (:email (first members))))))))

(deftest remove-de-user-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups") :query-params {:user "de_grouper" :search "de:users:de-users"}}
     (json-response {:groups [{:id "du1" :name "de:users:de-users"}]})
     {:address (groups-url "groups" "du1" "members" "bob") :query-params {:user "de_grouper"}}
     (fn [_] {:status 200 :headers {"Content-Type" "application/json"} :body "{}"})}
    (testing "removing a DE user deletes their membership in the de-users group"
      (is (nil? (groups/remove-de-user "bob"))))))
