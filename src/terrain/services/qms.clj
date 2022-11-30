(ns terrain.services.qms
  (:require [clojure.set :refer [difference]]
            [clojure-commons.core :refer [remove-nil-values]]
            [terrain.clients.iplant-groups.subjects :as subjects]
            [terrain.clients.qms :as qms]))

(defn- subscription-error-response
  "Returns a JSON object indicating that a subscription couldn't be created."
  [username reason]
  (remove-nil-values
   {:user             (when-not (nil? username) {:username username})
    :failure_reason   reason
    :new_subscription false}))

(defn- get-subscription-response
  "Returns the subscription response to use for a username."
  [response-for username]
  (if (empty? username)
    (subscription-error-response nil "no username provided in request")
    (response-for username (subscription-error-response username (str "user does not exist: " username)))))

(defn add-subscriptions
  "Validates usernames in the request body before forwarding the requests to QMS to create the subscriptions. Only
  requests with valid usernames will be forwarded to QMS to create the subscriptions. It may not be necessary, but
  this function strives to preserve the order of responses to incoming subscription requests in the response body."
  [params body]
  (let [request-for     (into {} (mapv (juxt :username identity) (:subscriptions body)))
        usernames       (remove empty? (set (keys request-for)))
        valid-usernames (->> (subjects/lookup-subjects usernames) :subjects (map :id) set)
        qms-response    (qms/add-subscriptions params {:subscriptions (mapv request-for valid-usernames)})
        response-for    (->> (:result qms-response)
                             (mapv (juxt (comp :username :user) identity))
                             (into {}))]
    {:result (for [username (map :username (:subscriptions body))]
               (get-subscription-response response-for username))
     :status (:status qms-response)}))
