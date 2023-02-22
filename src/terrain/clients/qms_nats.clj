(ns terrain.clients.qms-nats
  (:require [terrain.util.nats :as nats]
            [terrain.util.config :as cfg]
            [slingshot.slingshot :refer [throw+]])
  (:import [org.cyverse.de.protobufs 
            AddAddonRequest 
            NoParamsRequest 
            UpdateAddonRequest 
            ByUUID
            AssociateByUUIDs
            UpdateSubscriptionAddonRequest]))

(def not-nil? (complement nil?))

(defn- select-assoc
  [m a selector assoc-key assoc-val]
  (if (not-nil? (get-in m selector))
    (assoc a assoc-key assoc-val)
    a))

(defn- handle-error
  [m]
  (if (not-nil? (:error m)) 
    (throw+ {:error_code  (get-in m [:error :error_code])
             :reason      (get-in m [:error :message])})
    m))

(defn- return-keys
  [m v]
  (-> m (handle-error) (select-keys v)))

(defn- update-request
  [m]
  (let [assocer (partial select-assoc m)]
    (merge {:addon (assoc m :uuid (str (:uuid m)))} (-> {}
                 (assocer [:name]                :update_name           true)
                 (assocer [:description]         :update_description    true)
                 (assocer [:resource_type :uuid] :update_resource_type  true)
                 (assocer [:default_amount]      :update_default_amount true)
                 (assocer [:default_paid]        :update_default_paid   true)))))

(defn- update-sub-addon-request
  [m]
  (let [assocer (partial select-assoc m)]
    (merge {:subscription_addon (assoc m :uuid (str (:uuid m)))}
           (-> {}
               (assocer [:amount] :update_amount true)
               (assocer [:paid] :update_paid true)))))

(defn add-addon
  [addon]
  (as-> {:addon addon} r
    (nats/request (cfg/add-addon-subject) AddAddonRequest r)
    (return-keys r [:addon])))

(defn list-addons
  []
  (as-> {} r
    (nats/request (cfg/list-addons-subject) NoParamsRequest r) 
    (return-keys r [:addons])))

(defn update-addon
  [addon]
  (as-> (update-request addon) r
    (nats/request (cfg/update-addon-subject) UpdateAddonRequest r)
    (return-keys r [:addon])))

(defn delete-addon
  [uuid]
  (as-> {:uuid (str uuid)} r
    (nats/request (cfg/delete-addon-subject) ByUUID r)
    (return-keys r [:addon])))

(defn add-subscription-addon
  [parent-uuid child-uuid]
  (as-> {:parent_uuid (str parent-uuid)
         :child_uuid  (str child-uuid)} r
    (nats/request (cfg/add-subscription-addon-subject) AssociateByUUIDs r)
    (return-keys r [:subscription_addon])))

(defn get-subscription-addon
  [addon-uuid]
  (as-> {:uuid (str addon-uuid)} r
    (nats/request (cfg/get-subscription-addon-subject) ByUUID r)
    (return-keys r [:subscription_addons])))

(defn list-subscription-addons
  [uuid]
  (as-> {:uuid (str uuid)} r
        (nats/request (cfg/list-subscription-addons-subject) ByUUID r)
        (return-keys r [:subscription_addons])))

(defn update-subscription-addon
  [sub-addon]
  (as-> (update-sub-addon-request sub-addon) r
    (nats/request (cfg/update-subscription-addon-subject) UpdateSubscriptionAddonRequest r)
    (return-keys r [:subscription_addon])))

(defn delete-subscription-addon
  [uuid]
  (as-> {:uuid (str uuid)} r
    (nats/request (cfg/delete-subscription-addon-subject) ByUUID r)
    (return-keys r [:subscription_addon])))

(defn get-subscription-addon
  [uuid]
  (as-> {:uuid (str uuid)} r
    (nats/request (cfg/get-subscription-addon-subject) ByUUID r)
    (return-keys r [:subscription_addon])))