(ns terrain.vice-test
  (:require [cemerick.url :as curl]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.string :as string]
            [clojure.test :refer :all]
            [terrain.services.vice :as vice]
            [terrain.test-fixtures :as test-fixtures]
            [terrain.util.config :as config])
  (:import [clojure.lang ExceptionInfo]))

(use-fixtures :once test-fixtures/with-test-config test-fixtures/with-test-user)

(def ^:private analysis-id "0123abcd-0000-4000-8000-00000000beef")

(defn- app-exposer-url [& components]
  (str (apply curl/url (config/app-exposer-base-uri) components)))

(defn- perms-url [& components]
  (str (apply curl/url (config/permissions-base) components)))

(defn- permissions-route
  "Fakes the permissions service lookup for the current user's most privileged
   permission on the analysis; a nil level fakes an analysis the user has no
   access to."
  [level]
  {{:address      (perms-url "permissions" "subjects" "user" "ipcdev" "analysis" analysis-id)
    :query-params {:lookup "true"}}
   {:get (test-fixtures/json-response
          {:permissions
           (if level
             [{:id               "78b8ba84-3f23-11f1-bc05-008cfa5ae621"
               :subject          {:id           "9e496e8e-3f23-11f1-bb2f-008cfa5ae621"
                                  :subject_id   "ipcdev"
                                  :subject_type "user"}
               :resource         {:id            "a55f1b9a-3f23-11f1-b8b8-008cfa5ae621"
                                  :name          analysis-id
                                  :resource_type "analysis"}
               :permission_level level}]
             [])})}})

(defn- exit-route
  [exited]
  {(app-exposer-url "vice" "admin" "analyses" analysis-id "exit")
   {:post (fn [_req] (reset! exited true) {:status 200 :headers {} :body ""})}})

(defn- async-data-route
  [body]
  {{:address      (app-exposer-url "vice" "async-data")
    :query-params {:external-id "ext-1" :user "ipcdev"}}
   {:get (test-fixtures/json-response body)}})

(deftest exit-permission-enforcement
  (doseq [{:keys [desc level allowed?]}
          [{:desc "an own-level permission allows exit"              :level "own"   :allowed? true}
           {:desc "a write-level permission allows exit"             :level "write" :allowed? true}
           {:desc "a read-level permission is not enough"            :level "read"  :allowed? false}
           {:desc "an admin-level permission is not enough"          :level "admin" :allowed? false}
           {:desc "an analysis the user cannot see reads as missing" :level nil     :allowed? false}]]
    (testing desc
      (let [exited (atom false)]
        (with-fake-routes-in-isolation
          (merge (permissions-route level)
                 (exit-route exited))
          (if allowed?
            (do (vice/exit analysis-id)
                (is @exited))
            (do (is (thrown? ExceptionInfo (vice/exit analysis-id)))
                (is (not @exited)))))))))

(deftest external-id-reshapes-key
  (with-fake-routes-in-isolation
    (merge
     (permissions-route "read")
     {(app-exposer-url "vice" "admin" "analyses" analysis-id "external-id")
      {:get (test-fixtures/json-response {:external_id "ext-1"})}})
    (testing "app-exposer's external_id is reshaped to the schema's externalID"
      (is (= {:externalID "ext-1"} (vice/external-id analysis-id))))))

(deftest external-id-not-found-when-invisible
  (with-fake-routes-in-isolation
    (permissions-route nil)
    (testing "analyses the user cannot read are reported as not found"
      (is (thrown? ExceptionInfo (vice/external-id analysis-id))))))

(deftest async-data-checks-analysis-visibility
  (with-fake-routes-in-isolation
    (merge
     (permissions-route "read")
     (async-data-route {:analysisID analysis-id :subdomain "a1b2c3" :ipAddr "127.0.0.1"}))
    (testing "async data passes through once the analysis is readable"
      (is (= "a1b2c3" (:subdomain (vice/async-data {:external-id "ext-1"})))))))

(deftest async-data-fails-closed-without-analysis-id
  (with-fake-routes-in-isolation
    (async-data-route {:analysisID nil :subdomain "a1b2c3" :ipAddr "127.0.0.1"})
    (testing "a response without an analysis id is rejected instead of leaked"
      (is (thrown? ExceptionInfo (vice/async-data {:external-id "ext-1"}))))))

(deftest async-data-does-not-leak-analysis-ids
  (with-fake-routes-in-isolation
    (merge
     (permissions-route nil)
     (async-data-route {:analysisID analysis-id :subdomain "a1b2c3" :ipAddr "127.0.0.1"}))
    (testing "the not-found error for an unreadable analysis omits the analysis id"
      (let [e (is (thrown? ExceptionInfo (vice/async-data {:external-id "ext-1"})))]
        (is (not (string/includes? (str e) analysis-id)))))))
