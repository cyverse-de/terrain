(ns terrain.util.config
  (:require [clojure.string :as string]
            [async-tasks-client.core :as async-tasks-client]
            [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [metadata-client.core :as metadata-client]
            [slingshot.slingshot :refer [throw+]]))

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

(declare listen-port)
(cc/defprop-optint listen-port
  "The port that terrain listens to."
  [props config-valid configs]
  "terrain.app.listen-port" 60000)

(declare environment-name)
(cc/defprop-optstr environment-name
  "The name of the environment that this instance of Terrain belongs to."
  [props config-valid configs]
  "terrain.app.environment-name" "docker-compose")

(declare allowed-groups)
(cc/defprop-optvec allowed-groups
  "The names of the groups that are permitted to access secured admin services."
  [props config-valid configs]
  "terrain.authz.allowed-groups" ["core-services", "tito-admins", "tito-qa-admins", "dev"])

(declare uid-domain-raw)
(cc/defprop-str uid-domain-raw
  "The domain name to append to the user identifier to get the fully qualified
   user identifier."
  [props config-valid configs]
  "terrain.uid.domain")

(defn uid-domain
  []
  (string/replace (uid-domain-raw) #"^@+" ""))

(declare admin-routes-enabled)
(cc/defprop-optboolean admin-routes-enabled
  "Enables or disables the administration routes."
  [props config-valid configs]
  "terrain.routes.admin" true)

(declare notification-routes-enabled)
(cc/defprop-optboolean notification-routes-enabled
  "Enables or disables notification endpoints."
  [props config-valid configs]
  "terrain.routes.notifications" true)

(declare app-routes-enabled)
(cc/defprop-optboolean app-routes-enabled
  "Enables or disables app endpoints."
  [props config-valid configs]
  "terrain.routes.apps" true)

(declare bag-routes-enabled)
(cc/defprop-optboolean bag-routes-enabled
  "Enables or disables bag endpoints."
  [props config-valid configs]
  "terrain.routes.bags" true)

(declare metadata-routes-enabled)
(cc/defprop-optboolean metadata-routes-enabled
  "Enables or disables metadata endpoints."
  [props config-valid configs]
  "terrain.routes.metadata" true)

(declare pref-routes-enabled)
(cc/defprop-optboolean pref-routes-enabled
  "Enables or disables preferences endpoints."
  [props config-valid configs]
  "terrain.routes.prefs" true)

(declare user-info-routes-enabled)
(cc/defprop-optboolean user-info-routes-enabled
  "Enables or disables user-info endpoints."
  [props config-valid configs]
  "terrain.routes.user-info" true)

(declare data-routes-enabled)
(cc/defprop-optboolean data-routes-enabled
  "Enables or disables data endpoints."
  [props config-valid configs]
  "terrain.routes.data" true)

(declare session-routes-enabled)
(cc/defprop-optboolean session-routes-enabled
  "Enables or disables user session endpoints."
  [props config-valid configs]
  "terrain.routes.session" true)

(declare collaborator-routes-enabled)
(cc/defprop-optboolean collaborator-routes-enabled
  "Enables or disables collaborator routes."
  [props config-valid configs]
  "terrain.routes.collaborator" true)

(declare fileio-routes-enabled)
(cc/defprop-optboolean fileio-routes-enabled
  "Enables or disables the fileio routes."
  [props config-valid configs]
  "terrain.routes.fileio" true)

(declare filesystem-routes-enabled)
(cc/defprop-optboolean filesystem-routes-enabled
  "Enables or disables the filesystem routes."
  [props config-valid configs]
  "terrain.routes.filesystem" true)

(declare search-routes-enabled)
(cc/defprop-optboolean search-routes-enabled
  "Enables or disables the search related routes."
  [props config-valid configs]
  "terrain.routes.search" false)

(declare request-routes-enabled)
(cc/defprop-optboolean request-routes-enabled
  "Enables or disables routes related to administrative requests."
  [props config-valid configs]
  "terrain.routes.requests" true)

(declare setting-routes-enabled)
(cc/defprop-optboolean setting-routes-enabled
  "Enables or disables routes related to user settings."
  [props config-valid configs]
  "terrain.routes.settings" true)

(declare coge-enabled)
(cc/defprop-optboolean coge-enabled
  "Enables or disables COGE endpoints."
  [props config-valid configs]
  "terrain.routes.coge" true)

(declare resource-usage-api-routes-enabled)
(cc/defprop-optboolean resource-usage-api-routes-enabled
  "Enables or disables resource-usage-api endpoints."
  [props config-valid configs]
  "terrain.routes.resource-usage-api" true)

(declare qms-api-routes-enabled)
(cc/defprop-optboolean qms-api-routes-enabled
  "Enables or disables the QMS related endpoints"
  [props config-valid configs]
  "terrain.routes.qms-api" true)

(declare data-usage-api-routes-enabled)
(cc/defprop-optboolean data-usage-api-routes-enabled
  "Enables or disables data-usage-api endpoints."
  [props config-valid configs]
  "terrain.routes.data-usage-api" true)

(declare iplant-email-base-url)
(cc/defprop-optstr iplant-email-base-url
  "The base URL to use when connnecting to the iPlant email service."
  [props config-valid configs app-routes-enabled]
  "terrain.email.base-url" "http://iplant-email")

(declare tool-request-dest-addr)
(cc/defprop-str tool-request-dest-addr
  "The destination email address for tool request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.tool-request-dest")

(declare tool-request-src-addr)
(cc/defprop-str tool-request-src-addr
  "The source email address for tool request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.tool-request-src")

(declare permanent-id-request-dest-addr)
(cc/defprop-str permanent-id-request-dest-addr
  "The destination email address for Permanent ID Request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.perm-id-req.dest")

(declare permanent-id-request-src-addr)
(cc/defprop-str permanent-id-request-src-addr
  "The source email address of Permanent ID Request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.perm-id-req.src")

(declare support-email-dest-addr)
(cc/defprop-str support-email-dest-addr
  "The destination email address for DE support request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.support-email-dest")

(declare support-email-src-addr)
(cc/defprop-str support-email-src-addr
  "The default source email address for DE support request messages."
  [props config-valid configs app-routes-enabled]
  "terrain.email.support-email-src")

(declare apps-base-url)
(cc/defprop-optstr apps-base-url
  "The base URL to use when connecting to secured Apps services."
  [props config-valid configs app-routes-enabled]
  "terrain.apps.base-url" "http://apps")

(declare async-tasks-base-url)
(cc/defprop-optstr async-tasks-base-url
  "The base URL to use when connecting to the async-tasks services."
  [props config-valid configs]
  "terrain.async-tasks.base-url" "http://async-tasks")

(declare metadata-base-url)
(cc/defprop-optstr metadata-base-url
  "The base URL to use when connecting to the metadata services."
  [props config-valid configs metadata-routes-enabled]
  "terrain.metadata.base-url" "http://metadata")

(declare notifications-base-url)
(cc/defprop-optstr notifications-base-url
  "The base URL to use when connecting to the notification agent."
  [props config-valid configs notification-routes-enabled]
  "terrain.notifications.base-url" "http://notifications")

(declare ipg-base)
(cc/defprop-optstr ipg-base
  "The base URL for the iplant-groups service."
  [props config-valid configs]
  "terrain.iplant-groups.base-url" "http://iplant-groups")

(declare grouper-user)
(cc/defprop-optstr grouper-user
  "The administrative user to use for Grouper."
  [props config-valid configs]
  "terrain.iplant-groups.grouper-user" "de_grouper")

(declare de-users-group)
(cc/defprop-optstr de-users-group
  "The name of the DE users group, excluding the folder."
  [props config-valid configs]
  "terrain.iplant-groups.de-users-group" "de-users")

(declare permissions-base)
(cc/defprop-optstr permissions-base
  "The base URL for the permissions service."
  [props config-valid configs]
  "terrain.permissions.base-url" "http://permissions")

(declare requests-base)
(cc/defprop-optstr requests-base
  "The base URL for the requests service."
  [props config-valid configs]
  "terrain.requests.base-url" "http://requests")

(declare jex-base-url)
(cc/defprop-optstr jex-base-url
  "The base URL for the JEX."
  [props config-valid configs app-routes-enabled]
  "terrain.jex.base-url" "http://jex-adapter")


;;;iRODS connection information


(declare irods-home)
(cc/defprop-optstr irods-home
  "Returns the path to the home directory in iRODS. Usually /iplant/home"
  [props config-valid configs data-routes-enabled]
  "terrain.irods.home" "/iplant/home")

(declare irods-user)
(cc/defprop-optstr irods-user
  "Returns the user that porklock should connect as."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.user" "rods")

(declare irods-pass)
(cc/defprop-optstr irods-pass
  "Returns the iRODS user's password."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.pass" "notprod")

(declare irods-host)
(cc/defprop-optstr irods-host
  "Returns the iRODS hostname/IP address."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.host" "irods")

(declare irods-port)
(cc/defprop-optstr irods-port
  "Returns the iRODS port."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.port" "1247")

(declare irods-zone)
(cc/defprop-optstr irods-zone
  "Returns the iRODS zone."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.zone" "iplant")

(declare irods-resc)
(cc/defprop-optstr irods-resc
  "Returns the iRODS resource."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.resc" "")

(declare irods-max-retries)
(cc/defprop-optint irods-max-retries
  "The number of retries for failed operations."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.max-retries" 10)

(declare irods-retry-sleep)
(cc/defprop-optint irods-retry-sleep
  "The number of milliseconds to sleep between retries."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.retry-sleep" 1000)

(declare irods-use-trash)
(cc/defprop-optboolean irods-use-trash
  "Toggles whether to move deleted files to the trash first."
  [props config-valid configs data-routes-enabled]
  "terrain.irods.use-trash" true)

(declare irods-admins)
(cc/defprop-optvec irods-admins
  "The admin users in iRODS."
  [props config-valid configs fileio-routes-enabled]
  "terrain.irods.admin-users" ["rods", "rodsadmin"])
;;;End iRODS connection information

;;; ICAT connection information
(declare icat-host)
(cc/defprop-optstr icat-host
  "The hostname for the server running the ICAT database."
  [props config-valid configs data-routes-enabled]
  "terrain.icat.host" "irods")

(declare icat-port)
(cc/defprop-optint icat-port
  "The port that the ICAT is accepting connections on."
  [props config-valid configs data-routes-enabled]
  "terrain.icat.port" 5432)

(declare icat-user)
(cc/defprop-optstr icat-user
  "The user for the ICAT database."
  [props config-valid configs data-routes-enabled]
  "terrain.icat.user" "rods")

(declare icat-password)
(cc/defprop-optstr icat-password
  "The password for the ICAT database."
  [props config-valid configs data-routes-enabled]
  "terrain.icat.password" "notprod")

(declare icat-db)
(cc/defprop-optstr icat-db
  "The database name for the ICAT database. Yeah, it's most likely going to be 'ICAT'."
  [props config-valid configs data-routes-enabled]
  "terrain.icat.db" "ICAT")
;;; End ICAT connection information.

;;; Garnish configuration
(declare garnish-type-attribute)
(cc/defprop-optstr garnish-type-attribute
  "The value that goes in the attribute column for AVUs that define a file type."
  [props config-valid configs data-routes-enabled]
  "terrain.garnish.type-attribute" "ipc-filetype")
;;; End of Garnish configuration

;;; File IO configuration
(declare fileio-url-import-app)
(cc/defprop-optuuid fileio-url-import-app
  "The identifier of the internal app used for URL imports."
  [props config-valid configs fileio-routes-enabled]
  "terrain.fileio.url-import-app" "1E8F719B-0452-4D39-A2F3-8714793EE3E6")
;;; End File IO configuration

;;; Filesystem configuration (a.k.a. data-info).

(declare fs-community-data)
(cc/defprop-optstr fs-community-data
  "The path to the root directory for community data."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.fs.community-data" "/iplant/home/shared")

(declare fs-bad-names)
(cc/defprop-optvec fs-bad-names
  "The bad data names."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.fs.bad-names" "cacheServiceTempDir")

(declare fs-perms-filter)
(cc/defprop-optvec fs-perms-filter
  "Hmmm..."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.fs.perms-filter" ["rods", "rodsadmin"])

(declare fs-bad-chars)
(cc/defprop-optstr fs-bad-chars
  "The characters that are considered invalid in iRODS dir- and filenames."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.fs.bad-chars" "\u0060\u0027\u000A\u0009")

(declare fs-max-paths-in-request)
(cc/defprop-optint fs-max-paths-in-request
  "The number of paths that are allowable in an API request."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.fs.max-paths-in-request" 1000)

;;; End Filesystem configuration

(declare default-search-result-limit)
(cc/defprop-optint default-search-result-limit
  "This is the default limit for the number of results for a data search."
  [props config-valid configs search-routes-enabled]
  "terrain.search.default-limit" 50)

(declare data-info-base-url)
(cc/defprop-optstr data-info-base-url
  "The base URL for the data info service."
  [props config-valid configs filesystem-routes-enabled]
  "terrain.data-info.base-url" "http://data-info")

(declare es-enabled)
(cc/defprop-optboolean es-enabled
  "Whether Elasticsearch is enabled for the deployment."
  [props config-valid configs]
  "terrain.es.enabled" false)

(declare es-url)
(cc/defprop-optstr es-url
  "The URL for Elastic Search"
  [props config-valid configs data-routes-enabled]
  "terrain.infosquito.es-url" "http://elasticsearch:9200")

(declare es-index)
(cc/defprop-optstr es-index
  "The ElasticSearch index"
  [props config-valid configs data-routes-enabled]
  "terrain.es.index" "data")

(declare es-user)
(cc/defprop-optstr es-user
  "The ElasticSearch username"
  [props config-valid configs data-routes-enabled]
  "terrain.es.username" nil)

(declare es-password)
(cc/defprop-optstr es-password
  "The ElasticSearch password"
  [props config-valid configs data-routes-enabled]
  "terrain.es.password" nil)

(declare jwt-private-signing-key)
(cc/defprop-optstr jwt-private-signing-key
  "The path to the private key used for signing JWT assertions."
  [props config-valid configs]
  "terrain.jwt.signing-key.private" "/etc/iplant/crypto/signing_key/private-key.pem")

(declare jwt-private-signing-key-password)
(cc/defprop-optstr jwt-private-signing-key-password
  "The password used to access the private key used for signing JWT assertions."
  [props config-valid configs]
  "terrain.jwt.signing-key.password" "notprod")

(declare jwt-public-signing-key)
(cc/defprop-optstr jwt-public-signing-key
  "The path to the public key used to validate JWT assertions."
  [props config-valid configs]
  "terrain.jwt.signing-key.public" "/etc/iplant/crypto/signing_key/public-key.pem")

(declare jwt-accepted-keys-dir)
(cc/defprop-optstr jwt-accepted-keys-dir
  "The path to the directory containing public signing keys for JWT assertions."
  [props config-valid configs]
  "terrain.jwt.accepted-keys.dir" "/etc/iplant/crypto/accepted_keys")

(declare jwt-signing-algorithm)
(cc/defprop-optstr jwt-signing-algorithm
  "The algorithm used to sign JWT assertions."
  [props config-valid configs]
  "terrain.jwt.signing-key.algorithm" "rs256")

(declare jwt-validity-window-end)
(cc/defprop-optint jwt-validity-window-end
  "The number of seconds before newly created JWT assertions expire."
  [props config-valid configs]
  "terrain.jwt.validity-window.end" 300)

(declare wso2-jwt-header)
(cc/defprop-optstr wso2-jwt-header
  "The name of the HTTP header used by requests forwarded from WSO2."
  [props config-valid configs]
  "terrain.wso2.jwt-header" "x-jwt-assertion-iplant-org")

(declare coge-base-url)
(cc/defprop-optstr coge-base-url
  "The base URL for CoGe services."
  [props config-valid configs coge-enabled]
  "terrain.coge.base-url" "https://genomevolution.org/coge/api/v1")

(declare coge-data-folder-name)
(cc/defprop-optstr coge-data-folder-name
  "The name of the coge data folder in each user's home folder."
  [props config-valid configs coge-enabled]
  "terrain.coge.data-folder-name" "coge_data")

(declare coge-user)
(cc/defprop-optstr coge-user
  "The COGE user that needs file sharing permissions for genome viewer services."
  [props config-valid configs coge-enabled]
  "terrain.coge.user" "coge")

(declare communities-metadata-attr)
(cc/defprop-optstr communities-metadata-attr
  "The attr of an App Community tag AVU."
  [props config-valid configs]
  "terrain.communities.metadata.attr" "cyverse-community")

(declare default-output-dir)
(cc/defprop-optstr default-output-dir
  "The default name of the default job output directory."
  [props config-valid configs]
  "terrain.job-exec.default-output-folder" "analyses")

(declare permanent-id-async-move-wait-seconds-max)
(cc/defprop-optint permanent-id-async-move-wait-seconds-max
  "The max amount of time to wait for async folder moves, in seconds.
   By default, 1023 = (2^10-1) seconds, since the waiting thread doubles its
   sleep-seconds each time it polls until the async task completes."
  [props config-valid configs]
  "terrain.permanent-id.async-move.wait-seconds.max" 1023)  ;; Just over 17 minutes

(declare permanent-id-curators-group)
(cc/defprop-optstr permanent-id-curators-group
  "The data store group that manages permanent ID request data."
  [props config-valid configs]
  "terrain.permanent-id.curators-group" "data-curators")

(declare permanent-id-staging-dir)
(cc/defprop-optstr permanent-id-staging-dir
  "The data store directory where user folders are staged for permanent ID requests."
  [props config-valid configs]
  "terrain.permanent-id.staging-dir" "/iplant/home/shared/commons_repo/staging")

(declare permanent-id-publish-dir)
(cc/defprop-optstr permanent-id-publish-dir
  "The data store directory where curated folders with a permanent ID are published."
  [props config-valid configs]
  "terrain.permanent-id.publish-dir" "/iplant/home/shared/commons_repo/curated")

(declare permanent-id-target-base-url)
(cc/defprop-str permanent-id-target-base-url
  "The base URL where curated folders with a permanent ID are published."
  [props config-valid configs]
  "terrain.permanent-id.target-base-url")

(declare permanent-id-identifier-attr)
(cc/defprop-optstr permanent-id-identifier-attr
  "The metadata attribute where a new permanent ID is stored."
  [props config-valid configs]
  "terrain.permanent-id.attr.identifier" "identifier")

(declare permanent-id-identifier-type-attr)
(cc/defprop-optstr permanent-id-identifier-type-attr
  "The metadata attribute for the type of the `permanent-id-identifier-attr` AVU."
  [props config-valid configs]
  "terrain.permanent-id.attr.identifier-type" "identifierType")

(declare permanent-id-date-attr)
(cc/defprop-optstr permanent-id-date-attr
  "The metadata attribute where a permanent ID request's publication year is set."
  [props config-valid configs]
  "terrain.permanent-id.attr.publication-year" "publicationYear")

(declare datacite-api-url)
(cc/defprop-optstr datacite-api-url
  "The DataCite API URL."
  [props config-valid configs]
  "terrain.permanent-id.datacite.base-url" "https://api.test.datacite.org")

(declare datacite-username)
(cc/defprop-optstr datacite-username
  "The DataCite API account username."
  [props config-valid configs]
  "terrain.permanent-id.datacite.username" "apitest")

(declare datacite-password)
(cc/defprop-optstr datacite-password
  "The DataCite API account password."
  [props config-valid configs]
  "terrain.permanent-id.datacite.password" "notprod")

(declare datacite-doi-prefix)
(cc/defprop-optstr datacite-doi-prefix
  "The DOI prefix or namespace to use when creating DOIs."
  [props config-valid configs]
  "terrain.permanent-id.datacite.doi-prefix" "10.33540")

(declare prefs-base-url)
(cc/defprop-optstr prefs-base-url
  "The hostname of the user-preferences service"
  [props config-valid configs]
  "terrain.preferences.host" "http://user-info/preferences")

(declare search-base-url)
(cc/defprop-optstr search-base-url
  "The hostname of the search service"
  [props config-valid configs]
  "terrain.search.base-url" "http://search")

(declare sessions-base-url)
(cc/defprop-optstr sessions-base-url
  "The hostname of the user-sessions service"
  [props config-valid configs]
  "terrain.sessions.host" "http://user-info/sessions")

(declare saved-searches-base-url)
(cc/defprop-optstr saved-searches-base-url
  "The base URL of the saved-searches service"
  [props config-valid configs]
  "terrain.saved-searches.host" "http://user-info/searches")

(declare bags-base-url)
(cc/defprop-optstr bags-base-url
  "The base URL for accessing bag information"
  [props config-valid configs]
  "terrain.bags.host" "http://user-info/bags")

(declare analyses-base-uri)
(cc/defprop-optstr analyses-base-uri
  "The base URI for the analyses service."
  [props config-valid configs]
  "terrain.analyses.base-uri" "http://analyses")

(declare app-exposer-base-uri)
(cc/defprop-optstr app-exposer-base-uri
  "The base URI for the app-exposer service."
  [props config-valid configs]
  "terrain.app-exposer.base-uri" "http://app-exposer")

(declare resource-usage-api-uri)
(cc/defprop-optstr resource-usage-api-uri
  "The base URI for the resource-usage-api service."
  [props config-valid configs]
  "terrain.resource-usage-api.base-uri" "http://resource-usage-api")

(declare qms-api-uri)
(cc/defprop-optstr qms-api-uri
  "The base URI for the QMS service."
  [props config-valid configs]
  "terrain.qms.base-uri" "http://qms")

(declare data-usage-api-uri)
(cc/defprop-optstr data-usage-api-uri
  "The base URI for the data-usage-api service."
  [props config-valid configs]
  "terrain.data-usage-api.base-uri" "http://data-usage-api")

(declare keycloak-base-uri)
(cc/defprop-optstr keycloak-base-uri
  "The base URI for the Keycloak server."
  [props config-valid configs]
  "terrain.keycloak.base-uri" "https://kc.cyverse.org/auth")

(declare keycloak-realm)
(cc/defprop-optstr keycloak-realm
  "The Keycloak realm to use."
  [props config-valid configs]
  "terrain.keycloak.realm" "CyVerse")

(declare keycloak-client-id)
(cc/defprop-str keycloak-client-id
  "The Keycloak client ID to use."
  [props config-valid configs]
  "terrain.keycloak.client-id")

(declare keycloak-client-secret)
(cc/defprop-str keycloak-client-secret
  "The keycloak client secret to use."
  [props config-valid configs]
  "terrain.keycloak.client-secret")

(declare keycloak-admin-base-uri)
(cc/defprop-optstr keycloak-admin-base-uri
  "The base URI to use for administrative requests to Keycloak."
  [props config-valid configs]
  "terrain.keycloak.admin-base-uri" "https://keycloaktest2.cyverse.org/auth/admin")

(declare keycloak-admin-client-id)
(cc/defprop-str keycloak-admin-client-id
  "The Keycloak admin client ID to use."
  [props config-valid configs]
  "terrain.keycloak.admin-client-id")

(declare keycloak-admin-client-secret)
(cc/defprop-str keycloak-admin-client-secret
  "The keycloak admin client secret to use."
  [props config-valid configs]
  "terrain.keycloak.admin-client-secret")

(declare dashboard-aggregator-url)
(cc/defprop-optstr dashboard-aggregator-url
  "The URL to the dashboard-aggregator service."
  [props config-valid configs]
  "terrain.dashboard-aggregator.base-uri" "http://dashboard-aggregator")

(declare nats-urls)
(cc/defprop-optstr nats-urls
  "A comma separated list of NATS connection URLs."
  [props config-valid configs]
  "terrain.nats.urls" "tls://nats-0.nats,tls://nats-1.nats,tls://nats-2.nats,tls://nats-3.nats")

(declare nats-reconnect-wait)
(cc/defprop-optint nats-reconnect-wait
  "How long to wait between NATS reconnection attempts"
  [props config-valid configs]
  "terrain.nats.reconnect.wait" 1)

(declare nats-max-reconnects)
(cc/defprop-optint nats-max-reconnects
  "The maximum number of reconnection attempts to NATS"
  [props config-valid configs]
  "terrain.nats.reconnect.max" 10)

(declare nats-tls-enabled)
(cc/defprop-optboolean nats-tls-enabled
  "Whether to use TLS with the connection to NATS"
  [props config-valid configs]
  "terrain.nats.tls.enabled" true)

(declare nats-tls-key)
(cc/defprop-optstr nats-tls-key
  "The filename of the TLS key used for connecting to NATS. Must be present in nats-tls-dir"
  [props config-valid configs]
  "terrain.nats.tls.key" "/etc/nats/tls/pkcs8/tls.pkcs8")

(declare nats-tls-crt)
(cc/defprop-optstr nats-tls-crt
  "The filename of the TLS key used for connecting to NATS. Must be present in nats-tls-dir"
  [props config-valid configs]
  "terrain.nats.tls.crt" "/etc/nats/tls/tls.crt")

(declare nats-tls-ca)
(cc/defprop-optstr nats-tls-ca
  "The filename of the TLS CA crt used for connecting to NATS. Must be present in nats-tls-dir"
  [props config-valid configs]
  "terrain.nats.tls.ca" "/etc/nats/tls/ca.crt")

(declare add-addon-subject)
(cc/defprop-optstr add-addon-subject
  "The NATS subject for adding add-ons"
  [props config-valid configs]
  "terrain.nats.subjects.addons.add" "cyverse.qms.addon.add")

(declare list-addons-subject)
(cc/defprop-optstr list-addons-subject
  "The NATS subject for listing add-ons"
  [props config-valid configs]
  "terrain.nats.subjects.addons.list" "cyverse.qms.addon.list")

(declare update-addon-subject)
(cc/defprop-optstr update-addon-subject
  "The NATS subject for updating an addon"
  [props config-valid configs]
  "terrain.nats.subjects.addons.update" "cyverse.qms.addon.update")

(declare delete-addon-subject)
(cc/defprop-optstr delete-addon-subject
  "The NATS subject for deleting an addon"
  [props config-valid configs]
  "terrain.nats.subjects.addons.delete" "cyverse.qms.addon.delete")

(declare add-subscription-addon-subject)
(cc/defprop-optstr add-subscription-addon-subject
  "The NATS subject for adding a subscription addon"
  [props config-valid configs]
  "terrain.nats.subjects.subscription.addons.add" "cyverse.qms.user.plan.addons.add")

(declare list-subscription-addons-subject)
(cc/defprop-optstr list-subscription-addons-subject
  "The NATS subject for listing a subscription's addons"
  [props config-valid configs]
  "terrain.nats.subjects.subscription.addons.list" "cyverse.qms.user.plan.addons.list")

(declare update-subscription-addon-subject)
(cc/defprop-optstr update-subscription-addon-subject
  "The NATS subject for updating a subscription addon"
  [props config-valid configs]
  "terrain.nats.subjects.subscription.addons.update" "cyverse.qms.user.plan.addons.update")

(declare delete-subscription-addon-subject)
(cc/defprop-optstr delete-subscription-addon-subject
  "The NATS subject for deleting a subscription addon"
  [props config-valid configs]
  "terrain.nats.subjects.subscription.addons.delete" "cyverse.qms.user.plan.addons.delete")

(declare get-subscription-addon-subject)
(cc/defprop-optstr get-subscription-addon-subject
  "The NATS subject for getting a subscription addon"
  [props config-valid configs]
  "terrain.nats.subjects.subscription.addons.get" "cyverse.qms.user.plan.addons.get")


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
