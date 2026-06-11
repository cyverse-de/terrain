(ns terrain.services.vice
  "User-level VICE operations that verify the caller's analysis permissions
   before delegating to app-exposer endpoints that have no user enforcement of
   their own."
  (:require [cheshire.core :as json]
            [clojure-commons.exception-util :as cxu]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.clients.app-exposer :as app-exposer]
            [terrain.clients.apps.raw :as apps]))

(defn- get-readable-analysis
  "Returns the analysis listing entry visible to the current user, or throws
   not-found; the apps service hides analyses the user cannot read, so this is
   also a read-level access check."
  [analysis-id]
  (let [filter  (json/encode [{:field "id" :value (str analysis-id)}])
        listing (apps/list-jobs {:filter filter})]
    (or (first (:analyses listing))
        (cxu/not-found (str "analysis " analysis-id " not found")))))

(def ^:private write-levels #{"write" "own"})

(defn- has-write-grant?
  "Checks the analysis permission listing for a write-level grant to the
   current user. The listing only contains grants to subjects other than the
   requesting user, so this is only consulted for non-owners."
  [analysis-id]
  (let [username  (:shortUsername current-user)
        grants    (-> (apps/list-job-permissions {:analyses [analysis-id]} {})
                      :analyses
                      first
                      :permissions)
        writable? (fn [{:keys [subject permission]}]
                    (and (= (:id subject) username)
                         (contains? write-levels permission)))]
    (boolean (some writable? grants))))

(defn- validate-write-access
  "Allows the analysis owner or a user holding a write-level grant, matching
   the bar the apps service applies to analysis stop requests."
  [analysis-id]
  (let [{owner :username} (get-readable-analysis analysis-id)]
    (when-not (or (= owner (:username current-user))
                  (has-write-grant? analysis-id))
      (cxu/forbidden (str "insufficient privileges for analysis " analysis-id)))))

(defn exit
  "Terminates the analysis without saving outputs."
  [analysis-id]
  (validate-write-access analysis-id)
  (app-exposer/admin-exit analysis-id))

(defn external-id
  "Returns the external ID associated with the analysis."
  [analysis-id]
  (get-readable-analysis analysis-id)
  (app-exposer/admin-external-id analysis-id))

(defn async-data
  "Returns the asynchronously-generated data for the analysis step identified
   by the external-id query parameter."
  [params]
  (let [data (app-exposer/async-data params)]
    (when-let [analysis-id (:analysisID data)]
      (get-readable-analysis analysis-id))
    data))
