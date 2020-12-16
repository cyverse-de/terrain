(ns terrain.routes.schemas.instantlaunches
  (:use [common-swagger-api.schema :only [describe NonBlankString]]
        [schema.core :only [defschema Any Keyword optional-key]])
  (:import [java.util UUID]))

(defschema InstantLaunch
  {:id              (describe UUID "The UUID of the instant launch")
   :quick_launch_id (describe UUID "The UUID for the quick launch used for the instant launch")
   :added_by        (describe String "The username of the user that created the instant launch")
   :added_on        (describe String "The date and time when the instant launch was created")})

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

(def LatestILMappingsDefaultsSummary "The latest defaults for instant launch mappings")
(def LatestILMappingsDefaultsDescription "The latest defaults for instant launch mappings,
   which determine which files can be used with a particular instant launch")

