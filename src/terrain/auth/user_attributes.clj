(ns terrain.auth.user-attributes
  (:use [slingshot.slingshot :only [try+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.response :as resp]
            [clojure-commons.exception-util :as cxu]
            [terrain.clients.iplant-groups.subjects :as subjects]
            [terrain.util.config :as cfg]
            [terrain.util.jwt :as jwt]
            [terrain.util.keycloak-oidc :as keycloak-oidc-util]))

(def
  ^{:doc "The username to use when we're using fake authentication."
    :dynamic true}
  fake-user nil)

(def
  ^{:doc "The authenticated user or nil if the service is unsecured or we have a service account instead."
    :dynamic true}
  current-user nil)

(def
  ^{:doc "The authenticated service account or nil if the service is unsecured or we have a real user."
    :dynamic true}
  service-account nil)

(defn no-auth-info
  "Returns a response indicating that no authentication information was found."
  []
  (resp/unauthorized "No user authentication information found in request."))

(defn not-permitted
  "Returns a response indicating that the authenticated user account is not permitted to call the endpoint."
  []
  (resp/forbidden "This account is not permitted to call this endpoint."))

;; TODO: fix common name retrieval when we add it as an attribute.
(defn user-from-attributes
  "Creates a map of values from user attributes obtained during the authentication process."
  [{:keys [user-attributes]}]
  (log/trace user-attributes)
  (let [first-name (get user-attributes "firstName")
        last-name  (get user-attributes "lastName")]
    {:username      (str (get user-attributes "uid") "@" (cfg/uid-domain)),
     :password      (get user-attributes "password"),
     :email         (get user-attributes "email"),
     :shortUsername (get user-attributes "uid")
     :firstName     first-name
     :lastName      last-name
     :commonName    (str first-name " " last-name)
     :principal     (get user-attributes "principal")}))

(defn user-from-de-jwt-claims
  "Creates a map of values from JWT claims stored in the request by the DE."
  [{:keys [jwt-claims]}]
  (jwt/terrain-user-from-jwt-claims jwt-claims))

(defn user-from-wso2-jwt-claims
  "Creates a map of values from JWT claims stored int he request by WSO2."
  [{:keys [jwt-claims]}]
  (jwt/terrain-user-from-jwt-claims jwt-claims jwt/user-from-wso2-assertion))

(defn lookup-user
  "Looks up the user with the given username."
  [username]
  (try+
   (let [subject (subjects/lookup-subject (cfg/grouper-user) username)]
     {:username      (str (:id subject) "@" (cfg/uid-domain))
      :password      nil
      :email         (:email subject)
      :shortUsername (:id subject)
      :firstName     (:first_name subject)
      :lastName      (:last_name subject)
      :commonName    (:description subject)})
   (catch [:status 404] _
     (cxu/internal-system-error (str "fake user " username " not found")))
   (catch Object o
     (cxu/internal-system-error (str "fake user lookup for username " username " failed")))))

(defn fake-user-from-attributes
  "Uses the username bound to `fake-user` to obtain user attributes. The subject lookup happens with every request
   so that terrain doesn't have to be restarted if the subject lookup fails when terrain is starting up. This adds
   a little bit of overhead to each request when fake authentication is enabled, but it can avoid problems if, for
   example, iplant-groups isn't available when terrain is started."
  [& _]
  (if fake-user
    (lookup-user fake-user)
    (cxu/internal-system-error (str "no fake user specified on command line"))))

(defn- user-info-from-current-user
  "Converts the current-user to the user info structure expected in the request."
  [user]
  (when-not (nil? user)
    {:user       (:shortUsername user)
     :email      (:email user)
     :first-name (:firstName user)
     :last-name  (:lastName user)}))

(defn wrap-current-user
  "Generates a Ring handler function that stores user information in current-user."
  [handler user-info-fn]
  (fn [request]
    (binding [current-user (user-info-fn request)]
      (handler (assoc request :user-info (user-info-from-current-user current-user))))))

(defn wrap-service-account
  "Generates a Ring handler function that stores service account information in service-account."
  [handler service-account-info-fn]
  (fn [request]
    (binding [service-account (service-account-info-fn request)]
      (handler (assoc request :service-account service-account)))))

(defn- find-auth-handler
  "Finds an authentication handler for a request."
  [request phs]
  (->> (remove (fn [[token-fn _]] (nil? (token-fn request))) phs)
       (first)
       (second)))

(defn- wrap-auth-selection
  "Generates a ring handler function that selects the authentication method based on predicates."
  [phs]
  (fn [request]
    (log/log 'AccessLogger :trace nil "entering terrain.auth.user-attributes/wrap-auth-selection")
    (if-let [auth-handler (find-auth-handler request phs)]
      (auth-handler request)
      (no-auth-info))))

(defn- get-fake-auth
  "Returns a non-nil value if we're using fake authentication."
  [_]
  fake-user)

(defn- get-de-jwt-assertion
  "Extracts a JWT assertion from the request header used by the DE, returning nil if none is
   found."
  [request]
  (get (:headers request) "x-iplant-de-jwt"))

(defn- get-wso2-jwt-assertion
  "Extracts a JWT assertion from the request header used by WSO2, returning nil if none is
   found."
  [request]
  (when-let [header-name (cfg/wso2-jwt-header)]
    (get (:headers request) (string/lower-case header-name))))

(defn- get-authorization-header
  "Extracts the authorization header from the reqeust if present and splits it into its components."
  [request]
  (when-let [header (get (:headers request) "authorization")]
    (string/split header #"\s+" 2)))

(defn- is-bearer?
  "Returns a truthy value if a token type indicates that the token is a bearer token. This function
   exists only to reduce code duplication."
  [[token-type _]]
  (= (string/lower-case token-type) "bearer"))

(defn- is-jwt?
  "Returns a truthy value if a token appears to be a JWT."
  [[_ token]]
  (re-find #"^(?:[\p{Alnum}_-]+=*[.]){1,2}(?:[\p{Alnum}_-]+=*)$" token))

(defn- get-keycloak-oidc-token
  "Returns a non-nil value if we appear to have received a Keycloak bearer token."
  [request]
  (when-let [header (get-authorization-header request)]
    (when (and (is-bearer? header) (is-jwt? header))
      (second header))))

(defn- wrap-fake-auth
  [handler]
  (wrap-current-user handler fake-user-from-attributes))

(defn- wrap-de-jwt-auth
  [handler]
  (-> (wrap-current-user handler user-from-de-jwt-claims)
      (jwt/validate-jwt-assertion get-de-jwt-assertion)))

(defn- wrap-wso2-jwt-auth
  [handler]
  (-> (wrap-current-user handler user-from-wso2-jwt-claims)
      (jwt/validate-jwt-assertion get-wso2-jwt-assertion jwt/user-from-wso2-assertion)))

(defn- wrap-keycloak-oidc
  [handler]
  (-> handler
      (wrap-current-user keycloak-oidc-util/user-from-token)
      (wrap-service-account keycloak-oidc-util/service-account-from-token)
      (keycloak-oidc-util/validate-token get-keycloak-oidc-token)))

(defn authenticate-current-user
  "Authenticates the user and binds current-user to a map that is built from the user attributes retrieved
   during the authentication process. This middleware does not require authentication information to be
   present in the request. If authentication information is present then authentication must succeed for the
   request to be processed. Requests without credentials will be passed to the handler without authentication.
   Routes that require authentication can use the require-authentication middleware."
  [handler]
  (wrap-auth-selection [[get-fake-auth           (wrap-fake-auth handler)]
                        [get-de-jwt-assertion    (wrap-de-jwt-auth handler)]
                        [get-wso2-jwt-assertion  (wrap-wso2-jwt-auth handler)]
                        [get-keycloak-oidc-token (wrap-keycloak-oidc handler)]
                        [(constantly true)       handler]]))

(defn validate-current-user
  "Verifies that the user belongs to one of the groups that are permitted to access the resource."
  [handler]
  (wrap-auth-selection
   [[get-fake-auth           handler]
    [get-de-jwt-assertion    (jwt/validate-group-membership handler cfg/allowed-groups)]
    [get-wso2-jwt-assertion  (constantly (resp/forbidden "Admin not supported for WSO2."))]
    [get-keycloak-oidc-token (keycloak-oidc-util/validate-group-membership handler cfg/allowed-groups)]
    [(constantly true)       (constantly (resp/unauthorized "Admin endpoints require authentication."))]]))

(defn require-authentication
  "Middleware that checks for user information in an incoming request and returns a 401 if it's not found.
   Should be placed in the middleware chain after either authenticate-current-user or wrap-current-user."
  [handler]
  (fn [req]
    (if (:user-info req)
      (handler req)
      (no-auth-info))))

(defn require-service-account
  "Middleware that checks for a service account attached to an incoming requests and returns a 401 if it's not found."
  [handler & [authorized-roles]]
  (fn [{:keys [service-account user-info] :as req}]
    (let [authorized-roles (set authorized-roles)
          authorized-role? (fn [role] (contains? authorized-roles role))]
      (cond
        (and service-account (empty? authorized-roles))
        (handler req)

        (and service-account (some authorized-role? (:roles service-account)))
        (handler req)

        (or service-account user-info)
        (not-permitted)

        :else
        (no-auth-info)))))

(defn resolve-test-user
  "Attempts to resolve a test user from either a username or a map of user attributes."
  [user]
  (cond (string? user) (lookup-user user)
        (map? user)    (user-from-attributes {:user-attribues user})
        :else          (cxu/internal-system-error "user must be a string or a map of attributes")))

(defmacro with-user
  "Performs a task with the given user information bound to current-user. This macro is used
   for debugging in the REPL."
  [[user] & body]
  `(binding [current-user (resolve-test-user ~user)]
     (do ~@body)))
