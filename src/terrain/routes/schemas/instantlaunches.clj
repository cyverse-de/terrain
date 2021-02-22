(ns terrain.routes.schemas.instantlaunches
  (:use [common-swagger-api.schema :only [describe NonBlankString]]
        [schema.core :only [defschema Any Keyword optional-key]])
  (:import [java.util UUID]))

(def InstantLaunchIDParam (describe String "The ID of the instant launch"))

(def ListInstantLaunchesSummary "Lists instant launches")
(def ListInstantLaunchesDescription "Lists the instant launches available in the system")
(def AddInstantLaunchSummary "Adds an instant launch")
(def AddInstantLaunchDescription "Creates a new instant launch")
(def GetInstantLaunchSummary "Returns an instant launch")
(def GetInstantLaunchDescription "Returns an instant launch based on its UUID")
(def UpdateInstantLaunchSummary "Updates an instant launch")
(def UpdateInstantLaunchDescription "Updates an instant launch's fields")
(def DeleteInstantLaunchSummary "Deletes an instant launch")
(def DeleteInstantLaunchDescription "Deletes an instant launch by its UUID")
(def AddLatestILMappingsDefaultsSummary "Adds a new mapping as the latest")
(def AddLatestILMappingsDefaultsDescription "Adds a new mapping as the latest, incrementing the version number")
(def UpdateLatestILMappingsDefaultsSummary "Updates the latest mapping")
(def UpdateLatestILMappingsDefaultsDescription "Updates the latest mapping without changing the version number")
(def DeleteLatestILMappingsDefaultsSummary "Deletes the latest mapping")
(def DeleteLatestILMappingsDefaultsDescription "Deletes the latest mapping, effectively rolling back to the previous version")
(def LatestILMappingsDefaultsSummary "The latest defaults for instant launch mappings")
(def LatestILMappingsDefaultsDescription "The latest defaults for instant launch mappings,
   which determine which files can be used with a particular instant launch")

(defschema InstantLaunch
  {(optional-key :id)              (describe UUID "The UUID of the instant launch")
   :quick_launch_id                (describe UUID "The UUID for the quick launch used for the instant launch")
   (optional-key :added_by)        (describe String "The username of the user who created the instant launch")
   (optional-key :added_on)        (describe String "The date and time when the instant launch was created")})

(defschema InstantLaunchList
  {:instant_launches (describe [InstantLaunch] "A list of instant launches")})

(defschema InstantLaunchSelector
  {:pattern    (describe String "The pattern used to match against the file. Interpretation is defined by the 'kind' field")
   :kind       (describe String "The kind of selector. Freeform for now")
   :default    (describe InstantLaunch "The default instant launch for the file")
   :compatible (describe [InstantLaunch] "Instant launches that are compatible with the file, but are not the default")})

(defschema InstantLaunchMapping
  {(describe Keyword "Instant Launch pattern key") (describe InstantLaunchSelector "Instant Launch pattern definition")})

(defschema DefaultInstantLaunchMapping
  {:id      (describe UUID "The UUID of the default instant launch mapping")
   :version (describe String "The format version of the instant launch mapping")
   :mapping (describe InstantLaunchMapping "The set of patterns use to match files to instant launches")})



