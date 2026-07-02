(ns terrain.clients.qms
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [java-time.api :as jt]
            [slingshot.slingshot :refer  [throw+ try+]]
            [terrain.clients.util :refer [with-trap]]
            [terrain.util.config :as config]))

(defn- extract-qms-error
  [body]
  (let [body (if (string? body) body (slurp body))]
    (try+
     (:error (json/parse-string body true) body)
     (catch Object _
       body))))

(defn- default-error-handler
  [error-code {:keys [body] :as response}]
  (log/error "QMS request failed:" response)
  (let [error (extract-qms-error body)]
    (throw+ {:error_code error-code
             :reason     error})))

(defn- qms-api
  ([components]
   (qms-api components {}))
  ([components query]
   (-> (apply curl/url (config/qms-api-uri) components)
       (assoc :query query)
       str)))

;;; Admin
(defn get-usages
  [username]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "usages" username])
        (http/get {:as :json})
        (:body))))

(defn add-usage
  [usage]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "usages"])
        (http/post {:form-params  usage
                    :as           :json
                    :content-type :json})
        (:body))))

(defn add-subscriptions
  [params body]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "subscriptions"] params)
        (http/post {:form-params  body
                    :as           :json
                    :content-type :json})
        (:body))))

(defn list-subscriptions
  [params]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "subscriptions"] params)
        (http/get {:as :json})
        (:body))))

(defn list-user-subscriptions
  [username params]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "users" username "subscriptions"] params)
        (http/get {:as :json})
        (:body))))

(defn update-subscription-quota
  [username resource-type body]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "users" username "plan" resource-type "quota"])
        (http/post {:form-params  body
                    :as           :json
                    :content-type :json})
        (:body))))

(defn update-subscription
  [username plan-name params]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "users" username plan-name] params)
        (http/put {:as :json})
        (:body))))

;;; Non-admin
(defn subscription
  [username]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "users" username "plan"])
        (http/get {:as :json})
        (:body))))

(defn list-all-plans
  []
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "plans"])
        (http/get {:as :json})
        (:body))))

(defn single-plan
  [plan-id]
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "plans" plan-id])
        (http/get {:as :json})
        (:body))))

(defn list-resource-types
  []
  (with-trap [default-error-handler]
    (-> (qms-api ["v1" "resource-types"])
        (http/get {:as :json})
        (:body))))

;;; Add-on and subscription add-on operations, served by the subscriptions
;;; service. Its responses embed error information in the body rather than
;;; relying solely on the HTTP status.

(defn- subscriptions-api
  [components]
  (str (apply curl/url (config/subscriptions-base-uri) components)))

(defn- handle-embedded-error
  [{:keys [status body] :as response}]
  (cond
    (some? (:error body))
    (throw+ {:error_code (get-in body [:error :error_code])
             :reason     (get-in body [:error :message])})

    (>= status 400)
    (do (log/error "subscriptions request failed:" response)
        (throw+ {:error_code "ERR_REQUEST_FAILED"
                 :reason     (or (:message body) body)}))

    :else body))

(defn- subscriptions-request
  ([method components]
   (subscriptions-request method components nil))
  ([method components body]
   (-> (http/request (cond-> {:method           method
                              :url              (subscriptions-api components)
                              :as               :json
                              :coerce           :always
                              :throw-exceptions false}
                       body (assoc :form-params body :content-type :json)))
       (handle-embedded-error))))

(defn- rfc3339
  [timestamp-str]
  (str (jt/instant timestamp-str)))

(defn- normalize-addon
  "Normalizes an add-on for the subscriptions service: UUIDs as strings and
   rate effective dates as RFC 3339 timestamps."
  [addon]
  (cond-> addon
    (get-in addon [:resource_type :uuid]) (update-in [:resource_type :uuid] str)
    (:uuid addon)                         (update :uuid str)
    (:addon_rates addon)                  (update :addon_rates
                                                  (partial mapv #(cond-> %
                                                                   (:uuid %)           (update :uuid str)
                                                                   (:effective_date %) (update :effective_date rfc3339))))))

(defn- addon-update-flags
  "Builds the updateX flags the subscriptions service uses to decide which
   add-on fields to modify; a field is updated when it's present. Each flag is
   sent under both its current camelCase key and the snake_case key the
   pre-migration service bound, so a mis-ordered rolling deploy fails loudly
   instead of silently ignoring the update; each service ignores the key it
   doesn't know."
  [addon]
  (cond-> {}
    (some? (:name addon))                         (assoc :updateName true :update_name true)
    (some? (:description addon))                  (assoc :updateDescription true :update_description true)
    (some? (get-in addon [:resource_type :uuid])) (assoc :updateResourceType true :update_resource_type true)
    (some? (:default_amount addon))               (assoc :updateDefaultAmount true :update_default_amount true)
    (some? (:default_paid addon))                 (assoc :updateDefaultPaid true :update_default_paid true)
    (some? (:addon_rates addon))                  (assoc :updateAddonRates true :update_addon_rates true)))

(defn add-addon
  [addon]
  (-> (subscriptions-request :put ["addons"] {:addon (normalize-addon addon)})
      (select-keys [:addon])))

(defn list-addons
  []
  (-> (subscriptions-request :get ["addons"])
      (select-keys [:addons])))

(defn update-addon
  [addon]
  (-> (subscriptions-request :post ["addons" (str (:uuid addon))]
                             (assoc (addon-update-flags addon) :addon (normalize-addon addon)))
      (select-keys [:addon])))

(defn delete-addon
  [uuid]
  (-> (subscriptions-request :delete ["addons" (str uuid)])
      (select-keys [:addon])))

(defn add-subscription-addon
  [subscription-uuid addon-uuid]
  (-> (subscriptions-request :put ["subscriptions" (str subscription-uuid) "addons" (str addon-uuid)])
      (select-keys [:subscription_addon])))

(defn list-subscription-addons
  [subscription-uuid]
  (-> (subscriptions-request :get ["subscriptions" (str subscription-uuid) "addons"])
      (select-keys [:subscription_addons])))

(defn update-subscription-addon
  [subscription-uuid sub-addon]
  (let [sub-addon (update sub-addon :uuid str)]
    (-> (subscriptions-request :post ["subscriptions" (str subscription-uuid) "addons" (:uuid sub-addon)]
                               (cond-> {:subscription_addon sub-addon}
                                 (some? (:amount sub-addon)) (assoc :update_amount true)
                                 (some? (:paid sub-addon))   (assoc :update_paid true)))
        (select-keys [:subscription_addon]))))

(defn delete-subscription-addon
  [subscription-uuid sub-addon-uuid]
  (-> (subscriptions-request :delete ["subscriptions" (str subscription-uuid) "addons" (str sub-addon-uuid)])
      (select-keys [:subscription_addon])))

(defn get-subscription-addon
  [subscription-uuid sub-addon-uuid]
  (-> (subscriptions-request :get ["subscriptions" (str subscription-uuid) "addons" (str sub-addon-uuid)])
      (select-keys [:subscription_addon])))
