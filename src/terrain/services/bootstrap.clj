(ns terrain.services.bootstrap
  (:use
    [slingshot.slingshot :only [try+]]
    [terrain.auth.user-attributes :only [current-user]])
  (:require
    [clojure.tools.logging :as log]
    [terrain.clients.apps :as apps-client]
    [terrain.clients.data-info :as data-info-client]
    [terrain.services.user-prefs :as prefs]
    [terrain.util.service :as service]))

(defn- decode-error-response
  [body]
  (let [response (if (string? body) body (slurp body))]
    (try+
      (service/decode-json response)
    (catch Object _
      response))))

(defn- trap-bootstrap-request
  [req]
  (try+
    (req)
    (catch #(not (nil? (:status %))) {:keys [status body] :as e}
      (log/error e)
      {:status status
       :error  (decode-error-response body)})
    (catch map? e
      (log/error e)
      {:error e})
    (catch Object _
      (log/error (:throwable &throw-context) "bootstrap request failed")
      {:error (str (:throwable &throw-context))})))

(defn- get-login-session
  [ip-address user-agent]
  (trap-bootstrap-request
    #(select-keys (apps-client/record-login ip-address user-agent) [:login_time :auth_redirect])))

(defn- get-apps-info
  []
  (trap-bootstrap-request #(apps-client/bootstrap)))

(defn- get-user-data-info
  [user]
  (trap-bootstrap-request
    #(data-info-client/user-base-paths user)))

(defn- get-user-prefs
  [username]
  (trap-bootstrap-request
    #(prefs/user-prefs username)))

(defn bootstrap
  "This service obtains information about and initializes the workspace for the authenticated user.
   It also records the fact that the user logged in."
  [{{:keys [ip-address]} :params {user-agent "user-agent"} :headers}]
  (service/assert-valid ip-address "Missing or empty query string parameter: ip-address")
  (service/assert-valid user-agent "Missing or empty request parameter: user-agent")
  (let [{user :shortUsername :keys [email firstName lastName username]} current-user
        login-session (future (get-login-session ip-address user-agent))
        apps-info     (future (get-apps-info))
        data-info     (future (get-user-data-info user))
        preferences   (future (get-user-prefs username))]
    (service/success-response
      {:user_info   {:username      user
                     :full_username username
                     :email         email
                     :first_name    firstName
                     :last_name     lastName}
       :session     @login-session
       :apps_info   @apps-info
       :data_info   @data-info
       :preferences @preferences})))
