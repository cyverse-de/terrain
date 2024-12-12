(ns terrain.services.bootstrap
  (:require
    [clojure.tools.logging :as log]
    [clojure-commons.assertions :as assertions]
    [slingshot.slingshot :refer [try+]]
    [terrain.auth.user-attributes :refer [current-user]]
    [terrain.clients.apps.raw :as apps-client]
    [terrain.clients.keycloak.admin :as kc-client]
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
  [req & {:keys [extra-log-info]}]
  (try+
    (req)
    (catch #(not (nil? (:status %))) {:keys [status body] :as e}
      (log/error (:throwable &throw-context) e extra-log-info)
      {:status status
       :error  (decode-error-response body)})
    (catch map? e
      (log/error (:throwable &throw-context) e extra-log-info)
      {:error e})
    (catch Object _
      (log/error (:throwable &throw-context) "bootstrap request failed" extra-log-info)
      {:error (str (:throwable &throw-context))})))

(defn- get-login-session
  [username]
  (trap-bootstrap-request
   #(let [kc-resp (kc-client/get-user-session-by-username username)
          current-session (first kc-resp) ;; TODO: choose most recent start/access using our known client ID
          ]
      (select-keys (apps-client/record-login (:ipAddress current-session nil) 
                                             (:id current-session nil)
                                             (:start current-session nil)) [:login_time :auth_redirect]))
   {:extra-log-info "login session request"}))

(defn- get-apps-info
  []
  (trap-bootstrap-request
    #(apps-client/bootstrap)
    {:extra-log-info "apps bootstrap request"}))

(defn- get-user-data-info
  [user]
  (trap-bootstrap-request
   #(data-info-client/user-base-paths user)
   {:extra-log-info "base paths request"}))

(defn- get-user-prefs
  [username]
  (trap-bootstrap-request
   #(let [prefs (prefs/user-prefs username)]
      (if (and (:error prefs) (:default_output_folder prefs)) ;; if we've got both, there's an error stored in preferences. remove it so schema validation works right
        (dissoc prefs :error)
        prefs))
   {:extra-log-info "user prefs request"}))

(defn bootstrap
  "This service obtains information about and initializes the workspace for the authenticated user.
   It also records the fact that the user logged in."
  [ip-address user-agent]
  (assertions/assert-valid user-agent "Missing or empty request parameter: user-agent")
  (let [{user :shortUsername :keys [email firstName lastName username]} current-user
        login-session (future (get-login-session username))
        apps-info     (future (get-apps-info))
        data-info     (future (get-user-data-info user))
        preferences   (future (get-user-prefs username))]
    {:user_info   {:username      user
                   :full_username username
                   :email         email
                   :first_name    firstName
                   :last_name     lastName}
     :session     @login-session
     :apps_info   @apps-info
     :data_info   @data-info
     :preferences @preferences}))
