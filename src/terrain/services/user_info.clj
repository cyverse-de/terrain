(ns terrain.services.user-info
  (:require [terrain.clients.iplant-groups :as ipg]
            [terrain.auth.user-attributes :as user]))

(defn- add-user-info
  "Adds the information for a single user to a user-info lookup result."
  [result [username user-info]]
  (if (nil? user-info)
    result
    (assoc result username user-info)))

(defn- get-user-info
  "Gets the information for a single user, returning a vector in which the first
   element is the username and the second element is either the user info or nil
   if the user doesn't exist."
  [username]
  (->> (ipg/lookup-subject (:shortUsername user/current-user) username)
       (vector username)))

(defn user-info
  "Performs a user search for one or more usernames, returning a response whose
   body consists of a JSON object indexed by username."
  [usernames]
  (reduce add-user-info {} (map get-user-info usernames)))
