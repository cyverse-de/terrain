(ns terrain.notifications-test
  (:require [cemerick.url :as curl]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.java.io :refer [resource]]
            [clojure.test :refer :all]
            [medley.core :refer [map-vals]]
            [terrain.clients.notifications :as nc]
            [terrain.test-fixtures :as test-fixtures]
            [terrain.util.config :as config]
            [terrain.util.transformers :refer [add-current-user-to-map]]))

(use-fixtures :once test-fixtures/with-test-config test-fixtures/with-test-user)

(defn- notification-url [& components]
  (str (apply curl/url (config/notificationagent-base-url) components)))

(deftest last-ten-messages
  (with-fake-routes-in-isolation
    {{:address      (notification-url "messages")
      :query-params (map-vals str (add-current-user-to-map nc/last-ten-messages-params))}
     (fn [req]
       {:status 200
        :headers {"Content-Type" "application/json"}
        :body (slurp (resource "notifications.json"))})}

    (let [response (nc/last-ten-messages)]
      (testing "unaltered fields in response"
        (is (= (:total response) "77"))
        (is (= (:unseen_total response) "27"))
        (is (= (count (:messages response)) 10)))
      (testing "reversed sort order for notification listing"
        (is (apply < (map (comp #(Long/parseLong %) :timestamp :message) (:messages response))))))))
