(ns terrain.services.qms
  (:require [clojure.set :refer [difference]]
            [terrain.clients.iplant-groups.subjects :as subjects]
            [terrain.clients.qms :as qms]))

(defn- invalid-username-response
  "Returns a JSON object that can serve as a response indicating that a username provided in a subscription request was
  invalid."
  [username]
  {:failure_reason   (str "user does not exist: " username)
   :new_subscription false})

(defn add-subscriptions
  "Validates usernames in the request body before forwarding the requests to QMS to create the subscriptions. Only
  requests with valid usernames will be forwarded to QMS to create the subscriptions. It may not be necessary, but
  this function strives to preserve the order of responses to incoming subscription requests in the response body."
  [params body]
  (let [request-for     (into {} (mapv (juxt :username identity) (:subscriptions body)))
        usernames       (set (keys request-for))
        valid-usernames (->> (subjects/lookup-subjects usernames) :subjects (map :id) set)
        qms-response    (qms/add-subscriptions params {:subscriptions (mapv request-for valid-usernames)})
        response-for    (->> (:result qms-response)
                             (mapv (juxt (comp :username :user) identity))
                             (into {}))]
    {:result (for [username (map :username (:subscriptions body))]
               (response-for username (invalid-username-response username)))
     :status (:status qms-response)}))
