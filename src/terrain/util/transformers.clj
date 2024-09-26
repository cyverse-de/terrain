(ns terrain.util.transformers
  (:require [clojure.string :as string]
            [medley.core :refer [remove-vals]]
            [terrain.auth.user-attributes :refer [current-user]]))

(defn- invalid-query-param-value?
  "Determines whether the argument represents a valid query parameter value."
  [v]
  (cond
    (string? v)     (string/blank? v)
    (sequential? v) (empty? v)
    :else           (nil? v)))

(defn add-current-user-to-map
  "Adds the name and e-mail address of the currently authenticated user to a
   map that can be used to generate a query string. If no user is authenticated,
   the `user` parameter is set to `anonymous` and the rest of the user attribute
   parameters are omitted."
  ([query]
    (add-current-user-to-map query "anonymous"))
  ([query default-username]
    (->> (assoc query
           :user       (or (:shortUsername current-user) default-username)
           :email      (or (:email current-user) default-username)
           :first-name (:firstName current-user)
           :last-name  (:lastName current-user))
         (remove-vals invalid-query-param-value?))))

(defn secured-params
  "Generates a set of query parameters to pass to a remote service that requires
   information about the authenticated user."
  ([]
     (secured-params {}))
  ([existing-params]
     (add-current-user-to-map existing-params))
  ([existing-params param-keys]
     (secured-params (select-keys existing-params param-keys))))

(defn user-params
  "Generates a set of query parameters to pass to a remote service that requires
   the username of the authenticated user."
  ([]
   (user-params {}))
  ([existing-params]
   (as-> (add-current-user-to-map {}) m
         (select-keys m [:user])
         (merge existing-params m)))
  ([existing-params param-keys]
   (user-params (select-keys existing-params param-keys))))
