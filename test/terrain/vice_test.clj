(ns terrain.vice-test
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.test :refer :all]
            [medley.core :refer [map-vals]]
            [terrain.services.vice :as vice]
            [terrain.test-fixtures :as test-fixtures]
            [terrain.util.config :as config]
            [terrain.util.transformers :refer [add-current-user-to-map]])
  (:import [clojure.lang ExceptionInfo]))

(use-fixtures :once test-fixtures/with-test-config test-fixtures/with-test-user)

(def ^:private analysis-id "0123abcd-0000-4000-8000-00000000beef")

(defn- apps-url [& components]
  (str (apply curl/url (config/apps-base-url) components)))

(defn- app-exposer-url [& components]
  (str (apply curl/url (config/app-exposer-base-uri) components)))

(defn- json-response [body]
  (fn [_req]
    {:status  200
     :headers {"Content-Type" "application/json"}
     :body    (json/generate-string body)}))

(defn- permission-lister-route
  "Fakes the apps permission-lister, granting the test user the given permission level."
  [permission]
  {{:address      (apps-url "analyses" "permission-lister")
    :query-params (map-vals str (add-current-user-to-map {}))}
   {:post (json-response
           {:analyses [{:id          analysis-id
                        :name        "test-analysis"
                        :permissions [{:subject    {:source_id "ldap" :id "ipcdev"}
                                       :permission permission}]}]})}})

(deftest exit-with-write-permission
  (let [exited (atom false)]
    (with-fake-routes-in-isolation
      (merge
       (permission-lister-route "own")
       {(app-exposer-url "vice" "admin" "analyses" analysis-id "exit")
        {:post (fn [_req] (reset! exited true) {:status 200 :headers {} :body ""})}})
      (vice/exit analysis-id)
      (testing "exit reaches app-exposer when the user holds a write-level permission"
        (is @exited)))))

(deftest exit-forbidden-for-readers
  (with-fake-routes-in-isolation
    (permission-lister-route "read")
    (testing "read permission is not enough to terminate an analysis"
      (is (thrown? ExceptionInfo (vice/exit analysis-id))))))

(deftest external-id-reshapes-key
  (with-fake-routes-in-isolation
    (merge
     (permission-lister-route "read")
     {(app-exposer-url "vice" "admin" "analyses" analysis-id "external-id")
      {:get (json-response {:external_id "ext-1"})}})
    (testing "app-exposer's external_id is reshaped to the schema's externalID"
      (is (= {:externalID "ext-1"} (vice/external-id analysis-id))))))

(deftest async-data-checks-analysis-visibility
  (with-fake-routes-in-isolation
    (merge
     (permission-lister-route "read")
     {{:address      (app-exposer-url "vice" "async-data")
       :query-params {:external-id "ext-1" :user "ipcdev"}}
      {:get (json-response {:analysisID analysis-id :subdomain "a1b2c3" :ipAddr "127.0.0.1"})}})
    (testing "async data passes through once the analysis is readable"
      (is (= "a1b2c3" (:subdomain (vice/async-data {:external-id "ext-1"})))))))
