(ns terrain.user-attributes-test
  (:require [clojure.test :refer :all]
            [terrain.auth.user-attributes :as ua]))

(defn- dummy-success-response []
  {:status 200})

(deftest test-require-service-account
  (let [test-cases [{:request     {}
                     :description "unauthenticated request"
                     :expected    (ua/no-auth-info)}

                    {:request          {}
                     :description      "unauthenticated request with authorized roles"
                     :expected         (ua/no-auth-info)
                     :authorized-roles #{"foo-barrer"}}

                    {:request          {:user-info {:some :user}}
                     :description      "called by regular user"
                     :expected         (ua/not-permitted)
                     :authorized-roles #{"foo-barrer"}}

                    {:request          {:service-account {:roles []}}
                     :description      "service account with no authorized roles"
                     :expected         (dummy-success-response)
                     :authorized-roles #{}}

                    {:request          {:service-account {:roles []}}
                     :description      "service account without an authorized role"
                     :expected         (ua/not-permitted)
                     :authorized-roles #{"foo-barrer"}}

                    {:request          {:service-account {:roles ["foo-barrer"]}}
                     :description      "service account with the only authorized role"
                     :expected         (dummy-success-response)
                     :authorized-roles #{"foo-barrer"}}

                    {:request          {:service-account {:roles ["blarg-blrfler" "foo-barrer"]}}
                     :description      "service account with one authorized role"
                     :expected         (dummy-success-response)
                     :authorized-roles #{"foo-barrer"}}

                    {:request          {:service-account {:roles ["foo-barrer"]}}
                     :description      "valid request with authorized roles passed as vector"
                     :expected         (dummy-success-response)
                     :authorized-roles ["foo-barrer"]}

                    {:request          {:service-account {:roles []}}
                     :description      "forbidden request with authorized roles passed as vector"
                     :expected         (ua/not-permitted)
                     :authorized-roles ["foo-barrer"]}]]

    (doseq [{:keys [request description expected authorized-roles]} test-cases]
      (testing description
        (let [handler (ua/require-service-account (constantly (dummy-success-response)) authorized-roles)]
          (is (= expected (handler request))))))))
