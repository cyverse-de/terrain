(ns terrain.clients.subject-info
  "Backend-neutral helpers for subject lookups. Both group clients reshape subject information
   and recover from failed lookups in exactly the same way, so the shared pieces live here
   rather than in either client."
  (:require [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [try+]]))

(defn format-like-trellis
  "Reformats a subject lookup response to look like a trellis response."
  [response]
  {:username    (:id response)
   :firstname   (:first_name response)
   :lastname    (:last_name response)
   :name        (:name response)
   :email       (:email response)
   :institution (:institution response)})

(defn empty-user-info
  "Returns an empty user-info record for the given username."
  [username]
  {:id          username
   :name        ""
   :first_name  ""
   :last_name   ""
   :email       ""
   :institution ""
   :source_id   ""})

(defn lookup-or-nil
  "Performs a subject lookup, logging and returning nil when the subject is missing or the
   lookup fails. Subject details decorate responses that are otherwise complete, so a lookup
   failure must not fail the request that triggered it."
  [short-username lookup-fn]
  (try+
   (lookup-fn)
   (catch [:status 404] _
     (log/warn (str "no user info found for username '" short-username "'"))
     nil)
   (catch Object _
     (log/error (:throwable &throw-context) "user lookup for '" short-username "' failed")
     nil)))
