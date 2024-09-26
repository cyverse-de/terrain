(ns terrain.services.user-sessions
  (:require [clojure.tools.logging :as log]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.clients.user-sessions :refer [get-session set-session delete-session]]
            [terrain.util.service :refer [success-response]]))

(defn user-session
  ([]
     (let [user (:username current-user)]
       (log/debug "Getting user session for" user)
       (success-response (get-session user))))
  ([session]
     (let [user (:username current-user)]
       (log/debug "Setting user session for" user)
       (success-response (set-session user session)))))

(defn remove-session
  []
  (let [user (:username current-user)]
    (log/debug "Deleting user session for" user)
    (success-response (delete-session user))))
