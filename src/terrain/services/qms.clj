(ns terrain.services.qms
  (:require [clojure-commons.core :refer [remove-nil-values]]
            [clojure-commons.exception-util :as cxu]
            [terrain.clients.iplant-groups.subjects :as subjects]
            [terrain.util.nats :as nats]
            [protobuf.core :as protobuf]
            [terrain.util.config :as cfg]
            [terrain.clients.qms :as qms]
            [slingshot.slingshot :refer [throw+ try+]])
  (:import [org.cyverse.de.protobufs 
            AddAddonRequest 
            NoParamsRequest 
            UpdateAddonRequest 
            ByUUID
            AssociateByUUIDs
            UpdateSubscriptionAddonRequest]))

(defn- validate-username
  "Throws an error if a user with the given username doesn't exist."
  [username]
  (when (empty? (:subjects (subjects/lookup-subjects [username])))
    (cxu/bad-request (str "user '" username "' does not exist"))))

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

(defn update-subscription-quota
  "Validates the username before forwarding the request to QMS to update a user's resource usage limit."
  [username resource-type body]
  (validate-username username)
  (qms/update-subscription-quota username resource-type body))

(defn- select-assoc
  [m a selector assoc-key assoc-val]
  (if (get-in m selector)
    (assoc a assoc-key assoc-val)
    a))

(def not-nil? (complement nil?))

(defn- handle-error
  [m]
  (if (not-nil? (:error m)) 
    (throw+ {:error_code (or (get-in m [:error :error_code]) 
                             (get-in m [:error :status_code]))
             :reason     (get-in m [:error :message])})
    m))

(defn- return-keys
  [m v]
  (-> m (handle-error) (select-keys v)))

(defn add-addon
  [addon]
  (as-> {:addon addon} r
    (protobuf/create AddAddonRequest r)
    (nats/request-json (cfg/add-addon-subject) r)
    (return-keys r [:addon])))

(defn list-addons
  []
  (as-> {} r
    (protobuf/create NoParamsRequest r)
    (nats/request-json (cfg/list-addons-subject) r) 
    (select-keys r [:addons])))

(defn- update-request
  [m]
  (let [assocer (partial select-assoc m)]
    (merge {:addon (assoc m :uuid (str (:uuid m)))} (-> {}
                 (assocer [:name]                :update_name           true)
                 (assocer [:description]         :update_description    true)
                 (assocer [:resource_type :uuid] :update_resource_type  true)
                 (assocer [:default_amount]      :update_default_amount true)
                 (assocer [:default_paid]        :update_default_paid   true)))))

(defn update-addon
  [addon]
  (as-> (update-request addon) r
    (protobuf/create UpdateAddonRequest r)
    (nats/request-json (cfg/update-addon-subject) r)
    (return-keys r [:addon])))

(defn delete-addon
  [uuid]
  (as-> {:uuid (str uuid)} r
    (protobuf/create ByUUID r)
    (nats/request-json (cfg/delete-addon-subject) r)
    (return-keys r [:addon])))

(defn add-subscription-addon
  [parent-uuid child-uuid]
  (as-> {:parent_uuid (str parent-uuid)
         :child_uuid  (str child-uuid)} r
    (protobuf/create AssociateByUUIDs r)
    (nats/request-json (cfg/add-subscription-addon-subject) r)
    (return-keys r [:subscription-addon])))

(defn get-subscription-addon
  [addon-uuid]
  (as-> {:uuid (str addon-uuid)} r
    (protobuf/create ByUUID r)
    (nats/request-json (cfg/get-subscription-addon-subject) r)
    (return-keys r [:subscription_addons])))

(defn list-subscription-addons
  [uuid]
  (as-> {:uuid (str uuid)} r
        (protobuf/create ByUUID r)
        (nats/request-json (cfg/list-subscription-addons-subject) r)
        (return-keys r [:subscription_addons])))

(defn update-sub-addon-request
  [m]
  (let [assocer (partial select-assoc m)]
    (merge {:subscription_addon (assoc m :uuid (str (:uuid m)))}
           (-> {}
               (assocer [:amount] :update_amount true)
               (assocer [:paid] :update_paid true)))))

(defn update-subscription-addon
  [sub-addon]
  (as-> (update-sub-addon-request sub-addon) r
    (protobuf/create UpdateSubscriptionAddonRequest r)
    (nats/request-json (cfg/update-subscription-addon-subject) r)
    (return-keys r [:subscription_addon])))

(defn delete-subscription-addon
  [uuid]
  (as-> {:uuid (str uuid)} r
    (protobuf/create ByUUID r)
    (nats/request-json (cfg/delete-subscription-addon-subject) r)
    (return-keys r [:subscription_addon])))

(defn get-subscription-addon
  [uuid]
  (as-> {:uuid (str uuid)} r
    (protobuf/create ByUUID r)
    (nats/request-json (cfg/get-subscription-addon-subject) r)
    (return-keys r [:subscription_addon])))
