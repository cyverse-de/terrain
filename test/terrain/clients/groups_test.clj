(ns terrain.clients.groups-test
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.test :refer :all]
            [clojure-commons.exception :as cx]
            [slingshot.slingshot :refer [try+]]
            [terrain.clients.groups :as groups]
            [terrain.test-fixtures :as test-fixtures]
            [terrain.util.config :as config]))

(defmacro ^:private error-type
  "Returns the error type thrown by `body`, or nil if it threw nothing."
  [& body]
  `(try+ ~@body nil (catch map? e# (:type e#))))

(use-fixtures :once test-fixtures/with-test-config test-fixtures/with-test-user)

(defn- groups-url [& components]
  (str (apply curl/url (config/groups-base) components)))

(defn- json-response [body]
  (fn [_] {:status 200 :headers {"Content-Type" "application/json"} :body (json/encode body)}))

(defn- not-found []
  (fn [_] {:status 404 :headers {"Content-Type" "application/json"} :body "{}"}))

(defn- captured-body
  "Fake route that records the decoded request body in `capture` and responds with `body`."
  [capture body]
  (fn [req]
    (reset! capture (json/decode (slurp (:body req)) true))
    {:status 200 :headers {"Content-Type" "application/json"} :body (json/encode body)}))

(def ^:private alice
  {:id "alice" :name "alice" :first_name "Alice" :last_name "Anderson"
   :email "alice@example.org" :institution "CyVerse" :source_id "ldap"})

;; Groups as the service returns them: structured, with no hierarchy packed into the name.

(def ^:private cl-group
  {:id "g1" :group_type "collaborator_list" :owner "alice" :name "friends"
   :display_name "friends" :description "buddies"})

(def ^:private team-group
  {:id "t1" :group_type "team" :owner "alice" :name "t1" :display_name "t1" :description "d"})

(def ^:private community-group
  {:id "c1" :group_type "community" :name "biology" :display_name "biology" :description "d"})

;; Subject search and lookup.

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
     (not-found)}
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
     (not-found)}
    (testing "a missing subject yields an empty user-info block keyed by the username"
      (is (= {:id "ghost" :name "" :first_name "" :last_name "" :email "" :institution "" :source_id ""}
             (groups/lookup-subject-add-empty "de_grouper" "ghost"))))))

;; The external group contract. Every response passes through one formatter, so this is the
;; single place the shape terrain promises its callers is asserted.

(deftest format-group-contract-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "subjects" "bob" "groups") :query-params {:user "de_grouper"}}
     (json-response {:groups [team-group community-group]})}
    (let [{:keys [groups]} (groups/list-groups-for-user "bob" nil)
          [team community] groups]
      (testing "a team's external name carries its owner prefix"
        (is (= "alice:t1" (:name team))))
      (testing "other group types are named by their short name alone"
        (is (= "biology" (:name community))))
      (testing "the contract fields are exactly those the group schema defines"
        (is (= #{:id :name :type :id_index :description :display_extension :display_name}
               (set (keys team)))))
      (testing "the service's structured fields are not leaked to callers"
        (is (not-any? #(contains? team %) [:group_type :owner :created_at :updated_at])))
      (testing "type and id_index are synthesized, and display_extension is the short name"
        (is (= "group" (:type team)))
        (is (= "" (:id_index team)))
        (is (= "t1" (:display_extension team)))))))

;; Collaborator lists.

(defn- cl-lookup-route []
  {{:address (groups-url "groups" "lookup")
    :query-params {:user "alice" :group_type "collaborator_list" :owner "alice" :name "friends"}}
   (json-response cl-group)})

(deftest get-collaborator-lists-test
  (testing "listing asks the service for the caller's own lists"
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups") :query-params {:user "alice" :group_type "collaborator_list" :owner "alice"}}
       (json-response {:groups [cl-group]})}
      (let [{:keys [groups]} (groups/get-collaborator-lists "alice" nil)]
        (is (= ["friends"] (mapv :name groups)))
        (is (= "g1" (:id (first groups)))))))
  (testing "a search is delegated to the service rather than filtered in terrain"
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups")
        :query-params {:user "alice" :group_type "collaborator_list" :owner "alice" :search "fri"}}
       (json-response {:groups [cl-group]})}
      (is (= ["friends"] (mapv :name (:groups (groups/get-collaborator-lists "alice" nil "fri"))))))))

(deftest add-collaborator-list-test
  (let [captured (atom nil)]
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups") :query-params {:user "alice"}}
       (captured-body captured cl-group)}
      (let [result (groups/add-collaborator-list "alice" {:name "friends" :description "buddies"})]
        (testing "the group is created by structured identity, with no name packing"
          (is (= {:group_type "collaborator_list" :owner "alice" :name "friends"
                  :display_name "friends" :description "buddies"}
                 @captured)))
        (testing "the created list is returned in the external contract shape"
          (is (= "friends" (:name result)))
          (is (= "g1" (:id result))))))))

(deftest get-collaborator-list-test
  (with-fake-routes-in-isolation
    (cl-lookup-route)
    (testing "get resolves the list by identity in a single round trip"
      (let [result (groups/get-collaborator-list "alice" "friends")]
        (is (= "friends" (:name result)))
        (is (= "g1" (:id result)))))))

(deftest get-collaborator-list-not-found-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups" "lookup")
      :query-params {:user "alice" :group_type "collaborator_list" :owner "alice" :name "ghost"}}
     (not-found)}
    (testing "a missing list is reported as a 404 rather than an upstream error"
      (is (= ::cx/not-found (error-type (groups/get-collaborator-list "alice" "ghost")))))))

(deftest update-collaborator-list-test
  (let [captured (atom nil)]
    (with-fake-routes-in-isolation
      (merge (cl-lookup-route)
             {{:address (groups-url "groups" "g1") :query-params {:user "alice"}}
              (captured-body captured (assoc cl-group :name "pals" :display_name "pals"))})
      (let [result (groups/update-collaborator-list "alice" "friends" {:name "pals"})]
        (testing "only the fields being changed are sent"
          (is (= {:name "pals" :display_name "pals"} @captured)))
        (is (= "pals" (:name result)))))))

(deftest delete-collaborator-list-test
  (with-fake-routes-in-isolation
    (merge (cl-lookup-route)
           {{:address (groups-url "groups" "g1") :query-params {:user "alice"}}
            (json-response cl-group)})
    (testing "delete returns the removed group including its id"
      (let [result (groups/delete-collaborator-list "alice" "friends")]
        (is (= "g1" (:id result)))
        (is (= "friends" (:name result)))))))

(deftest get-collaborator-list-members-test
  (with-fake-routes-in-isolation
    (merge (cl-lookup-route)
           {{:address (groups-url "groups" "g1" "members") :query-params {:user "alice"}}
            (json-response {:members [alice {:id "de_grouper" :name "de_grouper"}]})})
    (let [{:keys [members]} (groups/get-collaborator-list-members "alice" "friends")]
      (testing "members exclude the admin user and get a display_name"
        (is (= 1 (count members)))
        (is (= "alice" (:id (first members))))
        (is (= "alice" (:display_name (first members))))))))

(deftest add-collaborator-list-members-test
  (with-fake-routes-in-isolation
    (merge (cl-lookup-route)
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

(deftest add-collaborator-list-members-creates-missing-list-test
  (let [created (atom nil)]
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups" "lookup")
        :query-params {:user "alice" :group_type "collaborator_list" :owner "alice" :name "friends"}}
       (not-found)
       {:address (groups-url "groups") :query-params {:user "alice"}}
       (captured-body created cl-group)
       {:address (groups-url "groups" "g1" "members") :query-params {:user "alice"}}
       (json-response {:results [{:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"}]})}
      (let [{:keys [results]} (groups/add-collaborator-list-members "alice" "friends" ["bob"])]
        (testing "a list that does not exist yet is created rather than 404ing"
          (is (= "friends" (:name @created)))
          (is (= "bob" (:subject_id (first results)))))))))

(deftest remove-collaborator-list-members-test
  (with-fake-routes-in-isolation
    (merge (cl-lookup-route)
           {{:address (groups-url "groups" "g1" "members" "deleter") :query-params {:user "alice"}}
            (json-response {:results [{:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"}]})})
    (is (= "bob" (:subject_id (first (:results (groups/remove-collaborator-list-members
                                                "alice" "friends" ["bob"]))))))))

;; Teams. The external name is `<owner>:<short>`; nothing else knows that format.

(defn- team-lookup-route [& {:keys [owner name group] :or {owner "alice" name "t1" group team-group}}]
  {{:address (groups-url "groups" "lookup")
    :query-params {:user "alice" :group_type "team" :owner owner :name name}}
   (json-response group)})

(deftest get-teams-test
  (testing "listing asks for teams by type"
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups") :query-params {:user "alice" :group_type "team"}}
       (json-response {:groups [team-group]})}
      (let [{:keys [groups]} (groups/get-teams "alice" {})]
        (is (= ["alice:t1"] (mapv :name groups)))
        (is (= "group" (:type (first groups)))))))
  (testing "a creator filter becomes an exact owner match, so `bob` cannot match `bobby`"
    ;; The old client filtered on the `<creator>:` string prefix, which matched any creator
    ;; whose name started with the requested one.
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups") :query-params {:user "alice" :group_type "team" :owner "bob"}}
       (json-response {:groups []})}
      (is (= [] (:groups (groups/get-teams "alice" {:creator "bob"}))))))
  (testing "a member filter is delegated to the service"
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups") :query-params {:user "alice" :group_type "team" :member "bob"}}
       (json-response {:groups [team-group]})}
      (is (= ["alice:t1"] (mapv :name (:groups (groups/get-teams "alice" {:member "bob"})))))))
  (testing "a search is delegated to the service"
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups") :query-params {:user "alice" :group_type "team" :search "t"}}
       (json-response {:groups [team-group]})}
      (is (= ["alice:t1"] (mapv :name (:groups (groups/get-teams "alice" {:search "t"}))))))))

(deftest add-team-test
  (testing "a public team grants the all-users subject read and returns the owner-scoped name"
    (let [captured (atom nil)]
      (with-fake-routes-in-isolation
        {{:address (groups-url "groups") :query-params {:user "alice"}}
         (captured-body captured team-group)
         {:address (groups-url "groups" "t1" "permissions" "group" "GrouperAll") :query-params {:user "alice"}}
         (json-response {:subject {:subject_id "GrouperAll" :subject_type "group"} :level "read"})}
        (let [result (groups/add-team "alice" {:name "t1" :description "d" :public_privileges ["view"]})]
          (is (= {:group_type "team" :owner "alice" :name "t1" :display_name "t1" :description "d"
                  :members_public false}
                 @captured))
          (testing "`view` makes the team discoverable without exposing its members"
            ;; This is the case the DE actually uses for public teams. Marking it
            ;; members_public would publish the membership of all 183 of them.
            (is (false? (:members_public @captured))))
          (is (= "alice:t1" (:name result)))
          (is (= "t1" (:id result)))))))
  (testing "a non-public team makes no permission grant"
    ;; Only the create route is registered; a permission PUT would fail the isolated routes.
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups") :query-params {:user "alice"}}
       (json-response team-group)}
      (is (= "alice:t1" (:name (groups/add-team "alice" {:name "t1" :description "d"})))))))

(deftest get-team-test
  (with-fake-routes-in-isolation
    (team-lookup-route)
    (let [result (groups/get-team "alice" "alice:t1")]
      (testing "get resolves the team by owner and short name"
        (is (= "alice:t1" (:name result)))
        (is (= "t1" (:id result)))))))

(deftest team-name-parsing-test
  (testing "a short name containing colons round-trips instead of being mangled"
    ;; The old client rebuilt the packed name as `de:teams:<creator>:<short>` and then split
    ;; the result on the first colon, so a colon in the short name moved the boundary.
    (let [captured (atom nil)
          weird    (assoc team-group :name "my:team" :display_name "my:team")]
      (with-fake-routes-in-isolation
        (merge (team-lookup-route :name "my:team" :group weird)
               {{:address (groups-url "groups" "t1") :query-params {:user "alice"}}
                (captured-body captured (assoc weird :name "other:name" :display_name "other:name"))})
        (let [result (groups/update-team "alice" "alice:my:team" {:name "other:name"})]
          (is (= {:name "other:name" :display_name "other:name"} @captured))
          (is (= "alice:other:name" (:name result)))))))
  (testing "a team name with no owner prefix is rejected rather than silently mis-resolved"
    (with-fake-routes-in-isolation {}
      (is (= ::cx/bad-request (error-type (groups/get-team "alice" "t1")))))))

(deftest update-team-test
  (let [captured (atom nil)]
    (with-fake-routes-in-isolation
      (merge (team-lookup-route)
             {{:address (groups-url "groups" "t1") :query-params {:user "alice"}}
              (captured-body captured (assoc team-group :description "new"))})
      (testing "an update that changes only the description leaves the name alone"
        (let [result (groups/update-team "alice" "alice:t1" {:description "new"})]
          (is (= {:description "new"} @captured))
          (is (= "alice:t1" (:name result))))))))

(deftest verify-team-exists-test
  (with-fake-routes-in-isolation
    (team-lookup-route)
    (is (nil? (groups/verify-team-exists "alice" "alice:t1")))))

(deftest delete-team-test
  (with-fake-routes-in-isolation
    (merge (team-lookup-route)
           {{:address (groups-url "groups" "t1") :query-params {:user "alice"}}
            (json-response team-group)})
    (is (= "t1" (:id (groups/delete-team "alice" "alice:t1"))))))

(deftest get-team-members-test
  (with-fake-routes-in-isolation
    (merge (team-lookup-route)
           {{:address (groups-url "groups" "t1" "members") :query-params {:user "alice"}}
            (json-response {:members [alice]})})
    (is (= ["alice"] (mapv :id (:members (groups/get-team-members "alice" "alice:t1")))))))

(deftest add-team-members-test
  (with-fake-routes-in-isolation
    (merge (team-lookup-route)
           {{:address (groups-url "groups" "t1" "members") :query-params {:user "alice"}}
            (json-response {:results [{:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"}]})})
    (let [{:keys [results]} (groups/add-team-members "alice" "alice:t1" ["bob"])]
      (testing "members are added and results pass through source_id/subject_name"
        (is (= {:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"} (first results)))))))

(deftest remove-team-members-test
  (with-fake-routes-in-isolation
    (merge (team-lookup-route)
           {{:address (groups-url "groups" "t1" "members" "deleter") :query-params {:user "alice"}}
            (json-response {:results [{:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"}]})})
    (is (= "bob" (:subject_id (first (:results (groups/remove-team-members "alice" "alice:t1" ["bob"]))))))))

(deftest join-team-test
  (with-fake-routes-in-isolation
    (merge (team-lookup-route)
           {{:address (groups-url "groups" "t1" "permissions") :query-params {:user "alice"}}
            (json-response {:permissions [{:subject {:subject_id "GrouperAll" :subject_type "group"} :level "read"}]})
            {:address (groups-url "groups" "t1" "members") :query-params {:user "de_grouper"}}
            (json-response {:results [{:subject_id "alice" :success true :source_id "ldap" :subject_name "Alice"}]})})
    (let [{:keys [results]} (groups/join-team "alice" "alice:t1")]
      (testing "joining a public team adds the caller as a member"
        (is (= "alice" (:subject_id (first results))))
        (is (true? (:success (first results))))))))

(deftest join-non-public-team-test
  (with-fake-routes-in-isolation
    (merge (team-lookup-route)
           {{:address (groups-url "groups" "t1" "permissions") :query-params {:user "alice"}}
            (json-response {:permissions [{:subject {:subject_id "alice" :subject_type "user"} :level "own"}]})})
    (testing "a team without the public grant cannot be joined"
      (is (= ::cx/forbidden (error-type (groups/join-team "alice" "alice:t1")))))))

(deftest leave-team-test
  (with-fake-routes-in-isolation
    (merge (team-lookup-route)
           {{:address (groups-url "groups" "t1" "members" "deleter") :query-params {:user "de_grouper"}}
            (json-response {:results [{:subject_id "alice" :success true :source_id "ldap" :subject_name "Alice"}]})})
    (let [{:keys [results]} (groups/leave-team "alice" "alice:t1")]
      (testing "leaving removes the caller from the team"
        (is (= "alice" (:subject_id (first results))))))))

(deftest list-team-privileges-test
  (with-fake-routes-in-isolation
    (merge (team-lookup-route)
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
    (merge (team-lookup-route)
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
    (merge (team-lookup-route)
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

;; Communities.

(defn- community-lookup-route []
  {{:address (groups-url "groups" "lookup")
    :query-params {:user "alice" :group_type "community" :name "biology"}}
   (json-response community-group)})

(deftest get-communities-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups") :query-params {:user "alice" :group_type "community"}}
     (json-response {:groups [community-group {:id "c2" :group_type "community" :name "physics"}]})
     {:address (groups-url "groups") :query-params {:user "alice" :group_type "community" :member "alice"}}
     (json-response {:groups [community-group]})}
    (let [{:keys [groups]} (groups/get-communities "alice" {})
          by-name (into {} (map (juxt :name identity)) groups)]
      (testing "listing reports per-user membership from one membership query"
        (is (= #{"biology" "physics"} (set (keys by-name))))
        (is (true? (:member (get by-name "biology"))))
        (is (false? (:member (get by-name "physics"))))
        (is (= [] (:privileges (get by-name "biology")))))
      (testing "contract group fields are synthesized"
        (is (= "group" (:type (get by-name "biology"))))))))

(deftest get-communities-for-member-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups") :query-params {:user "alice" :group_type "community" :member "alice"}}
     (json-response {:groups [community-group]})}
    (testing "listing a user's own communities needs no second membership query"
      (let [{:keys [groups]} (groups/get-communities "alice" {:member "alice"})]
        (is (= ["biology"] (mapv :name groups)))
        (is (true? (:member (first groups))))))))

(deftest admin-get-communities-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups") :query-params {:user "de_grouper" :group_type "community"}}
     (json-response {:groups [community-group]})}
    (let [{:keys [groups]} (groups/admin-get-communities "de_grouper" {})]
      (testing "admin listing omits member/privileges"
        (is (= ["biology"] (mapv :name groups)))
        (is (not (contains? (first groups) :member)))))))

(deftest add-community-test
  (let [captured (atom nil)]
    (with-fake-routes-in-isolation
      {{:address (groups-url "groups") :query-params {:user "alice"}}
       (captured-body captured community-group)
       {:address (groups-url "groups" "c1" "permissions" "group" "GrouperAll") :query-params {:user "alice"}}
       (json-response {:subject {:subject_id "GrouperAll" :subject_type "group"} :level "read"})}
      (let [result (groups/add-community "alice" {:name "biology" :description "d"
                                                  :public_privileges ["read" "optin"]})]
        (testing "a community is created with no owner, since it belongs to no user namespace"
          (is (= {:group_type "community" :name "biology" :display_name "biology" :description "d"
                  :members_public true}
                 @captured)))
        (testing "read among the public privileges makes the member list public"
          ;; Grouper gave public communities `read` and public teams `view`; the
          ;; permissions service cannot express the difference, so it rides on the
          ;; group. Dropping it would expose every public team's membership.
          (is (true? (:members_public @captured))))
        (is (= "biology" (:name result)))
        (is (= "c1" (:id result)))))))

(deftest get-community-test
  (with-fake-routes-in-isolation
    (community-lookup-route)
    (is (= "biology" (:name (groups/get-community "alice" "biology"))))))

(deftest update-community-test
  (let [captured (atom nil)]
    (with-fake-routes-in-isolation
      (merge (community-lookup-route)
             {{:address (groups-url "groups" "c1") :query-params {:user "alice"}}
              (captured-body captured (assoc community-group :name "bio" :display_name "bio"))})
      (testing "a rename is a single update with no app retagging"
        (let [result (groups/update-community "alice" "biology" {:name "bio"})]
          (is (= {:name "bio" :display_name "bio"} @captured))
          (is (= "bio" (:name result))))))))

(deftest delete-community-test
  (with-fake-routes-in-isolation
    (merge (community-lookup-route)
           {{:address (groups-url "groups" "c1") :query-params {:user "alice"}}
            (json-response community-group)})
    (is (= "c1" (:id (groups/delete-community "alice" "biology"))))))

(deftest get-community-members-test
  (with-fake-routes-in-isolation
    (merge (community-lookup-route)
           {{:address (groups-url "groups" "c1" "members") :query-params {:user "alice"}}
            (json-response {:members [alice]})})
    (is (= ["alice"] (mapv :id (:members (groups/get-community-members "alice" "biology")))))))

(deftest add-community-admins-test
  (with-fake-routes-in-isolation
    (merge (community-lookup-route)
           {{:address (groups-url "groups" "c1" "permissions" "user" "bob") :query-params {:user "alice"}}
            (fn [req]
              (is (= "admin" (:level (json/decode (slurp (:body req)) true))))
              {:status 200 :headers {"Content-Type" "application/json"} :body "{}"})
            {:address (groups-url "groups" "c1" "members") :query-params {:user "alice"}}
            (json-response {:results [{:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"}]})})
    (let [{:keys [results]} (groups/add-community-admins "alice" "biology" ["bob"])]
      (testing "an admin is granted the admin level and added as a member"
        (is (= "bob" (:subject_id (first results))))
        (is (true? (:success (first results))))))))

(deftest remove-community-admins-test
  (with-fake-routes-in-isolation
    (merge (community-lookup-route)
           {{:address (groups-url "groups" "c1" "permissions" "user" "bob") :query-params {:user "alice"}}
            (fn [_] {:status 200 :headers {"Content-Type" "application/json"} :body "{}"})
            {:address (groups-url "groups" "c1" "members" "deleter") :query-params {:user "alice"}}
            (json-response {:results [{:subject_id "bob" :success true :source_id "ldap" :subject_name "Bob"}]})})
    (is (= "bob" (:subject_id (first (:results (groups/remove-community-admins "alice" "biology" ["bob"]))))))))

(deftest get-community-admins-test
  (with-fake-routes-in-isolation
    (merge (community-lookup-route)
           {{:address (groups-url "groups" "c1" "permissions") :query-params {:user "alice"}}
            (json-response {:permissions [{:subject {:subject_id "alice" :subject_type "user"} :level "admin"}]})
            {:address (groups-url "subjects" "lookup") :query-params {:user "alice"}}
            (json-response {:subjects [{:id "alice" :name "Alice" :source_id "ldap"}]})})
    (is (= ["alice"] (mapv :id (:members (groups/get-community-admins "alice" "biology")))))))

(deftest join-community-test
  (with-fake-routes-in-isolation
    (merge (community-lookup-route)
           {{:address (groups-url "groups" "c1" "permissions") :query-params {:user "alice"}}
            (json-response {:permissions [{:subject {:subject_id "GrouperAll" :subject_type "group"} :level "read"}]})
            {:address (groups-url "groups" "c1" "members") :query-params {:user "de_grouper"}}
            (json-response {:results [{:subject_id "alice" :success true :source_id "ldap" :subject_name "Alice"}]})})
    (let [{:keys [results]} (groups/join-community "alice" "biology")]
      (testing "joining a public community adds the caller as a member"
        (is (= "alice" (:subject_id (first results))))))))

(deftest leave-community-test
  (with-fake-routes-in-isolation
    (merge (community-lookup-route)
           {{:address (groups-url "groups" "c1" "members" "deleter") :query-params {:user "de_grouper"}}
            (json-response {:results [{:subject_id "alice" :success true :source_id "ldap" :subject_name "Alice"}]})})
    (is (= "alice" (:subject_id (first (:results (groups/leave-community "alice" "biology"))))))))

;; DE user group administration.

(deftest remove-de-user-test
  (with-fake-routes-in-isolation
    {{:address (groups-url "groups" "lookup")
      :query-params {:user "de_grouper" :group_type "system" :name "de-users"}}
     (json-response {:id "du1" :group_type "system" :name "de-users"})
     {:address (groups-url "groups" "du1" "members" "bob") :query-params {:user "de_grouper"}}
     (fn [_] {:status 200 :headers {"Content-Type" "application/json"} :body "{}"})}
    (testing "removing a DE user deletes their membership in the de-users system group"
      (is (nil? (groups/remove-de-user "bob"))))))

(deftest admin-user-guard-test
  (testing "the administrative account may not be added to or removed from a group"
    ;; It is filtered out of every listing, so admitting it creates a member the
    ;; UI can neither show nor remove. The legacy client rejected this with a 400.
    (with-fake-routes-in-isolation
      (team-lookup-route)
      (doseq [[label f] [["add-team-members"      #(groups/add-team-members "alice" "alice:t1" ["de_grouper"])]
                         ["remove-team-members"   #(groups/remove-team-members "alice" "alice:t1" ["de_grouper"])]
                         ["add-community-admins"  #(groups/add-community-admins "alice" "biology" ["bob" "de_grouper"])]]]
        (is (= :clojure-commons.exception/bad-request
               (try+ (f) nil (catch [:type :clojure-commons.exception/bad-request] {:keys [type]} type)))
            (str label " must reject the administrative account"))))))
