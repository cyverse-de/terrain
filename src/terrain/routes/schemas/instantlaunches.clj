(ns terrain.routes.schemas.instantlaunches
  (:use [common-swagger-api.schema :only [describe NonBlankString]]
        [schema.core :only [defschema Any Keyword optional-key]])
  (:import [java.util UUID]))

(def InstantLaunchIDParam (describe String "The ID of the instant launch"))

(def ListInstantLaunchesSummary "Lists instant launches")
(def ListInstantLaunchesDescription "Lists the instant launches available in the system")
(def ListFullInstantLaunchesSummary "Lists instant launches with more info")
(def ListFullInstantLaunchesDescription "Lists instant launches with quick launch, submission, and app information")
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
(def ListMetadataSummary "Lists the AVUs associated with instant launches")
(def ListMetadataDescription "Lists the AVUs associated with instant launches while allowing filters based on attributes, values, and units")
(def GetMetadataSummary "Lists the AVUs associated with an instant launch")
(def GetMetadataDescription "Lists the AVUs associated with a single instant launch")
(def UpsertMetadataSummary "Adds/Updates AVUs for an instant launch")
(def UpsertMetadataDescription "Adds or updates AVUs asssociated with a single instant launch")
(def ResetMetadataSummary "Resets the AVUs associated with an instant launch")
(def ResetMetadataDescription "Resets all of the AVUs associated with an instant launch to the AVUs passed into the API call")
(def GetFullInstantLaunchSummary "Returns full description of an instant launch")
(def GetFullInstantLaunchDescription "Returns full description of an instant launch, including more info about the quick launch, submission, and app")
(def ListFullMetadataSummary "Returns a listing containing full descriptions of instant launches")
(def ListFullMetadataDescription "Returns a listing containing full description of instant launches based on the metadata passed in")
(def ListQuickLaunchesForPublicAppsSummary "Lists quick launches for public apps")
(def ListQuickLaunchesForPublicAppsDescription "Lists public quick launches for public apps along with quick launches the user has access to")

(defschema InstantLaunch
  {(optional-key :id)              (describe UUID "The UUID of the instant launch")
   :quick_launch_id                (describe UUID "The UUID for the quick launch used for the instant launch")
   (optional-key :added_by)        (describe String "The username of the user who created the instant launch")
   (optional-key :added_on)        (describe String "The date and time when the instant launch was created")})

(defschema FullInstantLaunch
  (assoc InstantLaunch
         :quick_launch_name        (describe String "The name of the quick launch")
         :quick_launch_description (describe String "The description of the quick launch")
         :quick_launch_creator     (describe String "The username of the person that created the quick launch")
         :is_public                (describe Boolean "Whether or not the quick launch is public")
         :submission               (describe Any "The submission associated with the instant launch/quick launch")
         :app_id                   (describe String "The UUID of the app used in the instant launch/quick launch")
         :app_version_id           (describe String "The UUID of the app version used in the instant launch/quick launch")
         :app_name                 (describe String "The name of the app used in the instant launch/quick launch")
         :app_description          (describe String "The description of the app used in the instant launch/quick launch")
         :app_deleted              (describe Boolean "Whether or not the app is deleted")
         :app_disabled             (describe Boolean "Whether or not the app is disabled")
         :integrator               (describe String "The username of the person that integrated the app used in the instant launch/quick launch")))

(defschema InstantLaunchList
  {:instant_launches (describe [InstantLaunch] "A list of instant launches")})

(defschema FullInstantLaunchList
  {:instant_launches (describe [FullInstantLaunch] "A list of full instant launches")})

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

;;; Slightly different from the one in the metadata service. Doesn't include user info or the target-type.
(defschema MetadataListingQueryMap
  {(optional-key :attribute) (describe [String] "A list of attributes to filter metadata listings by")
   (optional-key :value)     (describe [String] "A list of values to filter metadata listing by")
   (optional-key :unit)      (describe [String] "A list of units to filter metadata listing by")})



