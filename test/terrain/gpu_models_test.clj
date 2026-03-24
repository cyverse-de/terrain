(ns terrain.gpu-models-test
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.test :refer :all]
            [medley.core :refer [map-vals]]
            [terrain.clients.apps.raw :as apps]
            [terrain.test-fixtures :as test-fixtures]
            [terrain.util.config :as config]
            [terrain.util.transformers :refer [add-current-user-to-map]]))

(use-fixtures :once test-fixtures/with-test-config test-fixtures/with-test-user)

(defn- apps-url [& components]
  (str (apply curl/url (config/apps-base-url) components)))

(deftest list-gpu-models-passthrough
  (let [expected-body {:gpu_models ["Tesla V100" "A100" "RTX 3090"]}]
    (with-fake-routes-in-isolation
      {{:address      (apps-url "tools" "gpu-models")
        :query-params (map-vals str (add-current-user-to-map {}))}
       (fn [_req]
         {:status  200
          :headers {"Content-Type" "application/json"}
          :body    (json/generate-string expected-body)})}

      (let [response (apps/list-gpu-models)]
        (testing "passes through the response body from the apps service"
          (is (= (:gpu_models response) ["Tesla V100" "A100" "RTX 3090"])))))))
