(ns terrain.services.vice
  "User-level VICE operations that verify the caller's analysis permissions
   before delegating to app-exposer endpoints that have no user enforcement of
   their own."
  (:require [clojure-commons.exception-util :as cxu]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.clients.app-exposer :as app-exposer]
            [terrain.clients.permissions :as perms]))

(def ^:private write-levels #{"write" "own"})

(defn- permission-level
  "Returns the current user's most privileged permission level on the analysis,
   or nil if the user has no access to it. The permissions service resolves
   grants held via group membership, so this matches the bar the apps service
   applies when it authorizes analysis requests."
  [analysis-id]
  (perms/get-analysis-permission-level (:shortUsername current-user) analysis-id))

(defn- validate-read-access
  "Reports analyses the user cannot read as not found rather than revealing
   that they exist."
  [analysis-id]
  (when-not (permission-level analysis-id)
    (cxu/not-found (str "analysis " analysis-id " not found"))))

(defn- validate-write-access
  "Requires a write-level permission, matching the bar the apps service
   applies to analysis stop requests. Analyses the user cannot read at all are
   reported as not found."
  [analysis-id]
  (let [level (permission-level analysis-id)]
    (cond (nil? level)
          (cxu/not-found (str "analysis " analysis-id " not found"))

          (not (contains? write-levels level))
          (cxu/forbidden (str "insufficient privileges for analysis " analysis-id)))))

(defn exit
  "Terminates the analysis without saving outputs."
  [analysis-id]
  (validate-write-access analysis-id)
  (app-exposer/admin-exit analysis-id))

(defn external-id
  "Returns the external ID associated with the analysis."
  [analysis-id]
  (validate-read-access analysis-id)
  (app-exposer/admin-external-id analysis-id))

(defn async-data
  "Returns the asynchronously-generated data for the analysis step identified
   by the external-id query parameter. Fails closed: a response that cannot be
   attributed to an analysis the user can read is treated as not found, and the
   error never echoes the analysis ID, which the caller may not be entitled to
   learn."
  [params]
  (let [data        (app-exposer/async-data params)
        analysis-id (:analysisID data)]
    (if (and analysis-id (permission-level analysis-id))
      data
      (cxu/not-found (str "no analysis found for external ID " (:external-id params))))))
