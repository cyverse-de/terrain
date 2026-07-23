(ns terrain.clients.qms-addons
  "HTTP client for the QMS add-on and subscription-add-on operations served by
  the subscriptions service. These were previously reached over NATS."
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+ try+]]
            [terrain.clients.util :refer [with-trap]]
            [terrain.util.config :as config]))

(defn- addons-url
  [& components]
  (str (apply curl/url (config/subscriptions-base-uri) components)))

(defn- response-error
  "Pulls the error object out of a subscriptions response body, tolerating a
  body that isn't the expected JSON envelope."
  [body]
  (try+
   (:error (json/parse-string (if (string? body) body (slurp body)) true))
   (catch Object _ nil)))

(defn- default-error-handler
  [error-code {:keys [body] :as response}]
  (log/error "subscriptions add-on request failed:" response)
  (let [error (response-error body)]
    (throw+ {:error_code (or (:error_code error) error-code)
             :reason     (or (:message error) body)})))

(defn add-addon
  [addon]
  (with-trap [default-error-handler]
    (-> (addons-url "addons")
        (http/put {:form-params {:addon addon} :content-type :json :as :json})
        :body
        (select-keys [:addon]))))

(defn list-addons
  []
  (with-trap [default-error-handler]
    (-> (addons-url "addons")
        (http/get {:as :json})
        :body
        (select-keys [:addons]))))

;; The update-mask flags on UpdateAddonRequest are camelCase in the wire
;; contract; a flag is sent only when the corresponding field is present.
(defn- update-addon-body
  [addon]
  (let [set? (fn [ks] (some? (get-in addon ks)))]
    (cond-> {:addon addon}
      (set? [:name])                (assoc :updateName true)
      (set? [:description])         (assoc :updateDescription true)
      (set? [:resource_type :uuid]) (assoc :updateResourceType true)
      (set? [:default_amount])      (assoc :updateDefaultAmount true)
      (set? [:default_paid])        (assoc :updateDefaultPaid true)
      (set? [:addon_rates])         (assoc :updateAddonRates true))))

(defn update-addon
  [addon]
  (with-trap [default-error-handler]
    (-> (addons-url "addons" (str (:uuid addon)))
        (http/post {:form-params (update-addon-body addon) :content-type :json :as :json})
        :body
        (select-keys [:addon]))))

(defn delete-addon
  [uuid]
  (with-trap [default-error-handler]
    (-> (addons-url "addons" (str uuid))
        (http/delete {:as :json})
        :body
        (select-keys [:addon]))))

(defn add-subscription-addon
  [subscription-uuid addon-uuid]
  (with-trap [default-error-handler]
    (-> (addons-url "subscriptions" (str subscription-uuid) "addons" (str addon-uuid))
        (http/put {:as :json})
        :body
        (select-keys [:subscription_addon]))))

(defn list-subscription-addons
  [subscription-uuid]
  (with-trap [default-error-handler]
    (-> (addons-url "subscriptions" (str subscription-uuid) "addons")
        (http/get {:as :json})
        :body
        (select-keys [:subscription_addons]))))

;; UpdateSubscriptionAddonRequest's mask flags are snake_case in the wire
;; contract, unlike the add-on update flags above.
(defn- update-sub-addon-body
  [sub-addon]
  (let [set? (fn [k] (some? (get sub-addon k)))]
    (cond-> {:subscription_addon sub-addon}
      (set? :amount) (assoc :update_amount true)
      (set? :paid)   (assoc :update_paid true))))

(defn update-subscription-addon
  [sub-addon]
  (with-trap [default-error-handler]
    ;; The service ignores the path UUIDs on this route and keys off
    ;; subscription_addon.uuid in the body, so reuse the add-on UUID for both
    ;; path segments.
    (let [uuid (str (:uuid sub-addon))]
      (-> (addons-url "subscriptions" uuid "addons" uuid)
          (http/post {:form-params (update-sub-addon-body sub-addon) :content-type :json :as :json})
          :body
          (select-keys [:subscription_addon])))))

(defn delete-subscription-addon
  [uuid]
  (with-trap [default-error-handler]
    ;; Only the add-on UUID is read from this route; the subscription path
    ;; segment is ignored, so reuse the add-on UUID for both.
    (-> (addons-url "subscriptions" (str uuid) "addons" (str uuid))
        (http/delete {:as :json})
        :body
        (select-keys [:subscription_addon]))))

(defn get-subscription-addon
  [uuid]
  (with-trap [default-error-handler]
    ;; As with delete, only the add-on UUID is read from the path.
    (-> (addons-url "subscriptions" (str uuid) "addons" (str uuid))
        (http/get {:as :json})
        :body
        (select-keys [:subscription_addon]))))
