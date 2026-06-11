(ns terrain.services.vice
  "User-level VICE operations that verify the caller's analysis permissions
   before delegating to app-exposer endpoints that have no user enforcement of
   their own."
  (:require [clojure-commons.exception-util :as cxu]
            [terrain.auth.user-attributes :refer [current-user]]
            [terrain.clients.app-exposer :as app-exposer]
            [terrain.clients.apps.raw :as apps]))

(def ^:private write-levels #{"write" "own"})

(defn- analysis-permission-grants
  "Returns the permission grants for the analysis. The permission lister
   rejects analyses the caller cannot read, so calling this is also a
   read-level access check."
  [analysis-id]
  (-> (apps/list-job-permissions {:analyses [analysis-id]} {})
      :analyses
      first
      :permissions))

(defn- validate-write-access
  "Applies the same write-permission bar the apps service uses for analysis
   stop requests."
  [analysis-id]
  (let [username  (:shortUsername current-user)
        writable? (fn [{:keys [subject permission]}]
                    (and (= (:id subject) username)
                         (contains? write-levels permission)))]
    (when-not (some writable? (analysis-permission-grants analysis-id))
      (cxu/forbidden (str "insufficient privileges for analysis " analysis-id)))))

(defn exit
  "Terminates the analysis without saving outputs."
  [analysis-id]
  (validate-write-access analysis-id)
  (app-exposer/admin-exit analysis-id))

(defn external-id
  "Returns the external ID associated with the analysis."
  [analysis-id]
  (analysis-permission-grants analysis-id)
  (app-exposer/admin-external-id analysis-id))

(defn async-data
  "Returns the asynchronously-generated data for the analysis step identified
   by the external-id query parameter."
  [params]
  (let [data (app-exposer/async-data params)]
    (when-let [analysis-id (:analysisID data)]
      (analysis-permission-grants analysis-id))
    data))
