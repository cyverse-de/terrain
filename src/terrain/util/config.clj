(ns terrain.util.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [async-tasks-client.core :as async-tasks-client]
            [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [metadata-client.core :as metadata-client]))

(def de-system-id "de")
(def docs-uri "/docs")

(def ^:private props
  "A ref for storing the configuration properties."
  (ref nil))

(def ^:private config-valid
  "A ref for storing a configuration validity flag."
  (ref true))

(def ^:private configs
  "A ref for storing the symbols used to get configuration settings."
  (ref []))

(defn masked-config
  "Returns a masked version of the Terrain config as a map."
  []
  (cc/mask-config props :filters [#"(?:irods)[-.](?:user|pass|key|secret)"]))

(cc/defprop-optint listen-port
  "The port that terrain listens to."
  [props config-valid configs]
  "terrain.app.listen-port" 60000)

(cc/defprop-optstr environment-name
  "The name of the environment that this instance of Terrain belongs to."
  [props config-valid configs]
  "terrain.app.environment-name" "docker-compose")

(cc/defprop-optvec allowed-groups
  "The names of the groups that are permitted to access secured admin services."
  [props config-valid configs]
  "terrain.cas.allowed-groups" ["core-services", "tito-admins", "tito-qa-admins", "dev"])

(cc/defprop-str uid-domain
  "The domain name to append to the user identifier to get the fully qualified
   user identifier."
  [props config-valid configs]
  "terrain.uid.domain")

(cc/defprop-optboolean admin-routes-enabled
  "Enables or disables the administration routes."
  [props config-valid configs]
  "terrain.routes.admin" true)

(cc/defprop-optboolean notification-routes-enabled
  "Enables or disables notification endpoints."
  [props config-valid configs]
  "terrain.routes.notifications" true)

(cc/defprop-optboolean app-routes-enabled
  "Enables or disables app endpoints."
  [props config-valid configs]
  "terrain.routes.apps" true)

(cc/defprop-optboolean bag-routes-enabled
  "Enables or disables bag endpoints."
  [props config-valid configs]
  "terrain.routes.bags" true)

(cc/defprop-optboolean metadata-routes-enabled
  "Enables or disables metadata endpoints."
  [props config-valid configs]
  "terrain.routes.metadata" true)

(cc/defprop-optboolean pref-routes-enabled
  "Enables or disables preferences endpoints."
  [props config-valid configs]
  "terrain.routes.prefs" true)

(cc/defprop-optboolean user-info-routes-enabled
  "Enables or disables user-info endpoints."
  [props config-valid configs]
  "terrain.routes.user-info" true)

(cc/defprop-optboolean data-routes-enabled
  "Enables or disables data endpoints."
  [props config-valid configs]
  "terrain.routes.data" true)

(cc/defprop-optboolean session-routes-enabled
  "Enables or disables user session endpoints."
  [props config-valid configs]
  "terrain.routes.session" true)

(cc/defprop-optboolean collaborator-routes-enabled
  "Enables or disables collaborator routes."
  [props config-valid configs]
  "terrain.routes.collaborator" true)

(cc/defprop-optboolean fileio-routes-enabled
  "Enables or disables the fileio routes."
  [props config-valid configs]
  "terrain.routes.fileio" true)

(cc/defprop-optboolean filesystem-routes-enabled
  "Enables or disables the filesystem routes."
  [props config-valid configs]
  "terrain.routes.filesystem" true)

(cc/defprop-optboolean search-routes-enabled
  "Enables or disables the search related routes."
  [props config-valid configs]
  "terrain.routes.search" false)

(cc/defprop-optboolean request-routes-enabled
  "Enables or disables routes related to administrative requests."
  [props config-valid configs]
  "terrain.routes.requests" true)

(cc/defprop-optboolean setting-routes-enabled
  "Enables or disables routes related to user settings."
  [props config-valid configs]
  "terrain.routes.settings" true)

(cc/defprop-optboolean coge-enabled
  "Enables or disables COGE endpoints."
  [props config-valid configs]
  "terrain.routes.coge" true)

(cc/defprop-optstr iplant-email-base-url
  "The base URL to use when connnecting to the iPlant email service."
  [props config-valid configs app-routes-enabled]
  "terrain.email.base-url" "http://iplant-email")

(cc/defprop-str tool-request-dest-addr
  "The destination email address for tool request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.tool-request-dest")

(cc/defprop-str tool-request-src-addr
  "The source email address for tool request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.tool-request-src")

(cc/defprop-str permanent-id-request-dest-addr
  "The destination email address for Permanent ID Request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.perm-id-req.dest")

(cc/defprop-str permanent-id-request-src-addr
  "The source email address of Permanent ID Request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.perm-id-req.src")

(cc/defprop-str support-email-dest-addr
  "The destination email address for DE support request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.support-email-dest")

(cc/defprop-optstr apps-base-url
  "The base URL to use when connecting to secured Apps services."
  [props config-valid configs app-routes-enabled]
  "terrain.apps.base-url" "http://apps")

(cc/defprop-optstr async-tasks-base-url
  "The base URL to use when connecting to the async-tasks services."
  [props config-valid configs]
  "terrain.async-tasks.base-url" "http://async-tasks:60000")

(cc/defprop-optstr metadata-base-url
  "The base URL to use when connecting to the metadata services."
  [props config-valid configs metadata-routes-enabled]
  "terrain.metadata.base-url" "http://metadata")

(cc/defprop-optstr notifications-base-url
  "The base URL to use when connecting to the notification agent."
  [props config-valid configs notification-routes-enabled]
  "terrain.notifications.base-url" "http://notifications")

(cc/defprop-optstr ipg-base
  "The base URL for the iplant-groups service."
  [props config-valid configs]
  "terrain.iplant-groups.base-url" "http://iplant-groups")

(cc/defprop-optstr grouper-user
  "The administrative user to use for Grouper."
  [props config-valid configs]
  "terrain.iplant-groups.grouper-user" "de_grouper")

(cc/defprop-optstr de-users-group
  "The name of the DE users group, excluding the folder."
  [props config-valid configs]
  "terrain.iplant-groups.de-users-group" "de-users")

(cc/defprop-optstr permissions-base
  "The base URL for the permissions service."
  [props config-valid configs]
  "terrain.permissions.base-url" "http://permissions")

(cc/defprop-optstr requests-base
  "The base URL for the requests service."
  [props config-valid configs]
  "terrain.requests.base-url" "http://requests")

(cc/defprop-optstr jex-base-url
  "The base URL for the JEX."
  [props config-valid configs app-routes-enabled]
  "terrain.jex.base-url" "http://jex-adapter")


;;;iRODS connection information


(cc/defprop-optstr irods-home
  "Returns the path to the home directory in iRODS. Usually /iplant/home"
  [props config-valid configs data-routes-enabled]
  "terrain.irods.home" "/iplant/home")

(cc/defprop-optstr irods-user
  "Returns the user that porklock should connect as."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.user" "rods")

(cc/defprop-optstr irods-pass
  "Returns the iRODS user's password."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.pass" "notprod")

(cc/defprop-optstr irods-host
  "Returns the iRODS hostname/IP address."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.host" "irods")

(cc/defprop-optstr irods-port
  "Returns the iRODS port."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.port" "1247")

(cc/defprop-optstr irods-zone
  "Returns the iRODS zone."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.zone" "iplant")

(cc/defprop-optstr irods-resc
  "Returns the iRODS resource."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.resc" "")

(cc/defprop-optint irods-max-retries
  "The number of retries for failed operations."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.max-retries" 10)

(cc/defprop-optint irods-retry-sleep
  "The number of milliseconds to sleep between retries."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.retry-sleep" 1000)

(cc/defprop-optboolean irods-use-trash
  "Toggles whether to move deleted files to the trash first."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.use-trash" true)

(cc/defprop-optvec irods-admins
  "The admin users in iRODS."
  [props config-valid configs fileio-routes-enabled]
  "terrain.irods.admin-users" ["rods", "rodsadmin"])
;;;End iRODS connection information

;;; ICAT connection information
(cc/defprop-optstr icat-host
  "The hostname for the server running the ICAT database."
  [props config-valid configs data-routes-enabled]
  "terrain.icat.host" "irods")

(cc/defprop-optint icat-port
  "The port that the ICAT is accepting connections on."
  [props config-valid configs data-routes-enabled]
  "terrain.icat.port" 5432)

(cc/defprop-optstr icat-user
  "The user for the ICAT database."
  [props config-valid configs data-routes-enabled]
  "terrain.icat.user" "rods")

(cc/defprop-optstr icat-password
  "The password for the ICAT database."
  [props config-valid configs data-routes-enabled]
  "terrain.icat.password" "notprod")

(cc/defprop-optstr icat-db
  "The database name for the ICAT database. Yeah, it's most likely going to be 'ICAT'."
  [props config-valid configs data-routes-enabled]
  "terrain.icat.db" "ICAT")
;;; End ICAT connection information.

;;; Garnish configuration
(cc/defprop-optstr garnish-type-attribute
  "The value that goes in the attribute column for AVUs that define a file type."
  [props config-valid configs data-routes-enabled]
  "terrain.garnish.type-attribute" "ipc-filetype")
;;; End of Garnish configuration

;;; File IO configuration
(cc/defprop-optuuid fileio-url-import-app
  "The identifier of the internal app used for URL imports."
  [props config-valid configs fileio-routes-enabled]
  "terrain.fileio.url-import-app" "1E8F719B-0452-4D39-A2F3-8714793EE3E6")
;;; End File IO configuration

;;; Filesystem configuration (a.k.a. data-info).

(cc/defprop-optstr fs-community-data
  "The path to the root directory for community data."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.fs.community-data" "/iplant/home/shared")

(cc/defprop-optvec fs-bad-names
  "The bad data names."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.fs.bad-names" "cacheServiceTempDir")

(cc/defprop-optvec fs-perms-filter
  "Hmmm..."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.fs.perms-filter" ["rods", "rodsadmin"])

(cc/defprop-optstr fs-bad-chars
  "The characters that are considered invalid in iRODS dir- and filenames."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.fs.bad-chars" "\u0060\u0027\u000A\u0009")

(cc/defprop-optint fs-max-paths-in-request
  "The number of paths that are allowable in an API request."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.fs.max-paths-in-request" 1000)

;;; End Filesystem configuration

(cc/defprop-optint default-search-result-limit
  "This is the default limit for the number of results for a data search."
  [props config-valid configs search-routes-enabled]
  "terrain.search.default-limit" 50)

(cc/defprop-optstr data-info-base-url
  "The base URL for the data info service."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.data-info.base-url" "http://data-info")

(cc/defprop-optstr es-url
  "The URL for Elastic Search"
  [props config-valid configs data-routes-enabled]
  "terrain.infosquito.es-url" "http://elasticsearch:9200")

(cc/defprop-optstr es-index
  "The ElasticSearch index"
  [props config-valid configs data-routes-enabled]
  "terrain.es.index" "data")

(cc/defprop-optstr es-user
  "The ElasticSearch username"
  [props config-valid configs data-routes-enabled]
  "terrain.es.username" nil)

(cc/defprop-optstr es-password
  "The ElasticSearch password"
  [props config-valid configs data-routes-enabled]
  "terrain.es.password" nil)

(cc/defprop-optstr jwt-private-signing-key
  "The path to the private key used for signing JWT assertions."
  [props config-valid configs]
  "terrain.jwt.signing-key.private" "/etc/iplant/crypto/private-key.pem")

(cc/defprop-optstr jwt-private-signing-key-password
  "The password used to access the private key used for signing JWT assertions."
  [props config-valid configs]
  "terrain.jwt.signing-key.password" "notprod")

(cc/defprop-optstr jwt-public-signing-key
  "The path to the public key used to validate JWT assertions."
  [props config-valid configs]
  "terrain.jwt.signing-key.public" "/etc/iplant/crypto/public-key.pem")

(cc/defprop-optstr jwt-accepted-keys-dir
  "The path to the directory containing public signing keys for JWT assertions."
  [props config-valid configs]
  "terrain.jwt.accepted-keys.dir" "/etc/iplant/crypto/accepted_keys")

(cc/defprop-optstr jwt-signing-algorithm
  "The algorithm used to sign JWT assertions."
  [props config-valid configs]
  "terrain.jwt.signing-key.algorithm" "rs256")

(cc/defprop-optint jwt-validity-window-end
  "The number of seconds before newly created JWT assertions expire."
  [props config-valid configs]
  "terrain.jwt.validity-window.end" 300)

(cc/defprop-optstr wso2-jwt-header
  "The name of the HTTP header used by requests forwarded from WSO2."
  [props config-valid configs]
  "terrain.wso2.jwt-header" "x-jwt-assertion-iplant-org")

(cc/defprop-optstr coge-base-url
  "The base URL for CoGe services."
  [props config-valid configs coge-enabled]
  "terrain.coge.base-url" "https://genomevolution.org/coge/api/v1")

(cc/defprop-optstr coge-data-folder-name
  "The name of the coge data folder in each user's home folder."
  [props config-valid configs coge-enabled]
  "terrain.coge.data-folder-name" "coge_data")

(cc/defprop-optstr coge-user
  "The COGE user that needs file sharing permissions for genome viewer services."
  [props config-valid configs coge-enabled]
  "terrain.coge.user" "coge")

(cc/defprop-optstr communities-metadata-attr
  "The attr of an App Community tag AVU."
  [props config-valid configs]
  "terrain.communities.metadata.attr" "cyverse-community")

(cc/defprop-optstr default-output-dir
  "The default name of the default job output directory."
  [props config-valid configs]
  "terrain.job-exec.default-output-folder" "analyses")

(cc/defprop-optint permanent-id-async-move-wait-seconds-max
  "The max amount of time to wait for async folder moves, in seconds.
   By default, 1023 = (2^10-1) seconds, since the waiting thread doubles its
   sleep-seconds each time it polls until the async task completes."
  [props config-valid configs]
  "terrain.permanent-id.async-move.wait-seconds.max" 1023)  ;; Just over 17 minutes

(cc/defprop-optstr permanent-id-curators-group
  "The data store group that manages permanent ID request data."
  [props config-valid configs]
  "terrain.permanent-id.curators-group" "data-curators")

(cc/defprop-optstr permanent-id-staging-dir
  "The data store directory where user folders are staged for permanent ID requests."
  [props config-valid configs]
  "terrain.permanent-id.staging-dir" "/iplant/home/shared/commons_repo/staging")

(cc/defprop-optstr permanent-id-publish-dir
  "The data store directory where curated folders with a permanent ID are published."
  [props config-valid configs]
  "terrain.permanent-id.publish-dir" "/iplant/home/shared/commons_repo/curated")

(cc/defprop-str permanent-id-target-base-url
  "The base URL where curated folders with a permanent ID are published."
  [props config-valid configs]
  "terrain.permanent-id.target-base-url")

(cc/defprop-optstr permanent-id-identifier-attr
  "The metadata attribute where a new permanent ID is stored."
  [props config-valid configs]
  "terrain.permanent-id.attr.identifier" "identifier")

(cc/defprop-optstr permanent-id-identifier-type-attr
  "The metadata attribute for the type of the `permanent-id-identifier-attr` AVU."
  [props config-valid configs]
  "terrain.permanent-id.attr.identifier-type" "identifierType")

(cc/defprop-optstr permanent-id-date-attr
  "The metadata attribute where a permanent ID request's publication year is set."
  [props config-valid configs]
  "terrain.permanent-id.attr.publication-year" "publicationYear")

(cc/defprop-optstr ezid-base-url
  "The EZID API base URL."
  [props config-valid configs]
  "terrain.permanent-id.ezid.base-url" "https://ez.test.datacite.org")

(cc/defprop-optstr ezid-username
  "The EZID API account username."
  [props config-valid configs]
  "terrain.permanent-id.ezid.username" "apitest")

(cc/defprop-optstr ezid-password
  "The EZID API account password."
  [props config-valid configs]
  "terrain.permanent-id.ezid.password" "notprod")

(cc/defprop-optstr ezid-shoulders-ark
  "The EZID ARK shoulder or namespace."
  [props config-valid configs]
  "terrain.permanent-id.ezid.shoulders.ark" "ark:/99999/fk4")

(cc/defprop-optstr ezid-shoulders-doi
  "The EZID DOI shoulder or namespace."
  [props config-valid configs]
  "terrain.permanent-id.ezid.shoulders.doi" "doi:10.5072/FK2")

(cc/defprop-optstr prefs-base-url
  "The hostname of the user-preferences service"
  [props config-valid configs]
  "terrain.preferences.host" "http://user-info/preferences")

(cc/defprop-optstr search-base-url
  "The hostname of the search service"
  [props config-valid configs]
  "terrain.search.base-url" "http://search")

(cc/defprop-optstr sessions-base-url
  "The hostname of the user-sessions service"
  [props config-valid configs]
  "terrain.sessions.host" "http://user-info/sessions")

(cc/defprop-optstr saved-searches-base-url
  "The base URL of the saved-searches service"
  [props config-valid configs]
  "terrain.saved-searches.host" "http://user-info/searches")

(cc/defprop-optstr bags-base-url
  "The base URL for accessing bag information"
  [props config-valid configs]
  "terrain.bags.host" "http://user-info/bags")

(cc/defprop-optstr oauth-base-uri
  "The base URI for the OAuth API endpoints."
  [props config-valid configs]
  "terrain.oauth.base-uri" "https://cas/oauth2.0")

(cc/defprop-optstr oauth-client-id
  "The client ID to use when authenticating to the OAuth server."
  [props config-valid configs]
  "terrain.oauth.client-id" "fake-client-id")

(cc/defprop-optstr oauth-client-secret
  "The client secret to use when authenticating to the OAuth server."
  [props config-valid configs]
  "terrain.oauth.client-secret" "notprod")

(cc/defprop-optstr analyses-base-uri
  "The base URI for the analyses service."
  [props config-valid configs]
  "terrain.analyses.base-uri" "http://analyses")

(cc/defprop-optstr app-exposer-base-uri
  "The base URI for the app-exposer service."
  [props config-valid configs]
  "terrain.app-exposer.base-uri" "http://app-exposer")

(cc/defprop-optstr keycloak-base-uri
  "The base URI for the Keycloak server."
  [props config-valid configs]
  "terrain.keycloak.base-uri" "https://kc.cyverse.org/auth")

(cc/defprop-optstr keycloak-realm
  "The Keycloak realm to use."
  [props config-valid configs]
  "terrain.keycloak.realm" "CyVerse")

(cc/defprop-str keycloak-client-id
  "The Keycloak client ID to use."
  [props config-valid configs]
  "terrain.keycloak.client-id")

(cc/defprop-str keycloak-client-secret
  "The keycloak client secret to use."
  [props config-valid configs]
  "terrain.keycloak.client-secret")

(cc/defprop-optstr dashboard-aggregator-url
  "The URL to the dashboard-aggregator service."
  [props config-valid configs]
  "terrain.dashboard-aggregator.base-uri" "http://dashboard-aggregator")

(def async-tasks-client
  (memoize #(async-tasks-client/new-async-tasks-client (async-tasks-base-url))))

(def metadata-client
  (memoize #(metadata-client/new-metadata-client (metadata-base-url))))

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn- exception-filters
  []
  (filter #(not (nil? %))
          [(icat-password) (icat-user) (irods-pass) (irods-user)]))

(defn jwt-opts
  []
  {:private-key-path     (jwt-private-signing-key)
   :private-key-password (jwt-private-signing-key-password)
   :public-key-path      (jwt-public-signing-key)
   :alg                  (keyword (jwt-signing-algorithm))
   :accepted-keys-dir    (jwt-accepted-keys-dir)
   :validity-window-end  (jwt-validity-window-end)})

(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props :filters [#"irods\.user" #"icat\.user" #"oauth\.pem"])
  (validate-config)
  (ce/register-filters (exception-filters)))

(defn load-config-from-map
  "Loads the configuration settings from a Clojure map. This function is used for unit testing."
  [config-map]
  (cc/load-config-from-map config-map props)
  (validate-config)
  (ce/register-filters (exception-filters)))
