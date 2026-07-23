(ns terrain.test-fixtures
  (:require [cheshire.core :as json]
            [terrain.auth.user-attributes :as user-attributes]
            [terrain.util.config :as config]))

(defn json-response
  "Builds a clj-http.fake handler that responds with the given body serialized as JSON."
  [body]
  (fn [_req]
    {:status  200
     :headers {"Content-Type" "application/json"}
     :body    (json/generate-string body)}))

(def test-config
  {:terrain.uid.domain                   "iplantcollaborative.org"
   :terrain.email.tool-request-dest      "tool-request-dest@example.org"
   :terrain.email.tool-request-src       "tool-request-src@example.org"
   :terrain.email.perm-id-req.dest       "perm-id-request-dest@example.org"
   :terrain.email.perm-id-req.src        "perm-id-request-src@example.org"
   :terrain.email.support-email-dest     "support-email-dest@example.org"
   :terrain.email.support-email-src      "support-email-src@example.org"
   :terrain.permanent-id.target-base-url "http://perm-id-target-base-url"
   :terrain.keycloak.client-id           "keycloak-client-id"
   :terrain.keycloak.client-secret       "keycloak-client-secret"
   :terrain.keycloak.admin-client-id     "keycloak-admin-client-id"
   :terrain.keycloak.admin-client-secret "keycloak-admin-client-secret"})

(defn with-test-config
  "Runs a series of tests after loading the test configuration defined by `test-config` above. This fixture should
   be used any time the code used by a set of tests requires one or more configuration settings."
  [f]
  (config/load-config-from-map test-config)
  (f))

(def test-user
  {"uid"      "ipcdev"
   "email"    "ipcdev@cyverse.org"
   "firstName" "Ipc"
   "lastName" "Dev"})

(defn with-test-user
  "Runs a series of tests as if the user defined by `test-user` above has authenticated to an endpoint. This fixture
   should be used whenever an endpoint that requires user authentication is being tested."
  [f]
  (user-attributes/with-user [test-user] (f)))
