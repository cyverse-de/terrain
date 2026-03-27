(ns terrain.routes.schemas.vice
  (:require
   [common-swagger-api.schema :refer [describe NonBlankString]]
   [schema.core :refer [Any defschema maybe optional-key]]
   [schema.core :as s])
  (:import
   [java.util UUID]))

(def AnalysisID (describe UUID "The UUID assigned to the analysis"))
(def Subdomain (describe NonBlankString "The subdomain assigned to the analysis"))
(def ExternalID (describe NonBlankString "The external ID assigned to the analysis step"))

(defschema AsyncDataParams
  {:external-id (describe ExternalID "The external ID assigned to the analysis step")})

(defschema TimeLimitQueryParams
  {:user (describe NonBlankString "The username for the user that started the analysis, not the logged in user")})

(defschema TimeLimit
  {:time_limit (describe (maybe String) "The scheduled end date as seconds since the epoch")})

(defschema ExternalIDResponse
  {:externalID (describe ExternalID "The single external ID associated with the VICE analysis")})

(defschema AsyncData
  {:analysisID (describe (maybe AnalysisID) "The UUID assigned to the analysis")
   :ipAddr     (describe (maybe String) "The IP address of the user that launched the analysis")
   :subdomain  (describe (maybe Subdomain) "The subdomain assigned to the VICE analysis")})

(defschema BaseListing
  {:name                      (describe String "The name of the resource")
   :namespace                 (describe String "The namespace for the resource")
   :analysisName              (describe String "The name of the analysis the resource is associated with")
   (optional-key :analysisID) (describe (maybe UUID) "The UUID assigned to the analysis")
   :appName                   (describe String "The name of the app the resource is associated with")
   :appID                     (describe UUID "The UUID of the app the resource is associated with")
   :externalID                (describe UUID "The UUID assigned to the job step")
   :userID                    (describe UUID "The UUID assigned to the user that launched the analysis")
   :username                  (describe String "The username of the user that launched the analysis")
   :creationTimestamp         (describe String "The time the resource was created")})

(defschema Deployment
  (merge
   BaseListing
   {:image   (describe String "The container image name used in the K8s Deployment")
    :port    (describe Long "The port number the pods in the deployment are listening on")
    :user    (describe Long "The user ID of the analysis process")
    :group   (describe Long "The group ID of the analysis process")
    :command (describe [String] "The command used to start the analysis")}))

(defschema ContainerStateWaiting
  {(optional-key :reason)  (describe (maybe String) "The reason the container is in the waiting state")
   (optional-key :message) (describe (maybe String) "The message associated with the waiting state")})

(defschema ContainerStateRunning
  {:startedAt (describe String "The time the container started running")})

(defschema ContainerStateTerminated
  {(optional-key :exitCode)    (describe Long "The exit code for the container")
   (optional-key :signal)      (describe Long "The numerical signal sent to the container process")
   (optional-key :reason)      (describe (maybe String) "The reason the container terminated")
   (optional-key :message)     (describe (maybe String) "The message associated with the container termination")
   (optional-key :startedAt)   (describe (maybe String) "The time the container started")
   (optional-key :finishedAt)  (describe (maybe String) "The time the container finished")
   (optional-key :containerID) (describe String "The ID of the container")})

(defschema ContainerState
  {(optional-key :waiting)    (describe (maybe ContainerStateWaiting) "The waiting container state")
   (optional-key :running)    (describe (maybe ContainerStateRunning) "The running container state")
   (optional-key :terminated) (describe (maybe ContainerStateTerminated) "The terminated container state")})

(defschema ContainerStatus
  {:name                        (describe String "The name of the container")
   :ready                       (describe Boolean "Whether or not the container is ready")
   :restartCount                (describe Long "The number of times the container has restarted")
   :state                       (describe ContainerState "The current state of the container")
   :lastState                   (describe ContainerState "The previous state of the container")
   :image                       (describe String "The image name used for the container")
   :imageID                     (describe String "The image ID associated with the container")
   (optional-key :containerID)  (describe String "The ID associated with the container")
   (optional-key :started)      (describe Boolean "Whether or not the container has started")
   (optional-key :volumeMounts) (describe (maybe [Any]) "The volume mounts associated with the container")
   s/Keyword                    s/Any})

(defschema Pod
  (merge
   BaseListing
   {:phase
    (describe String "The pod phase")

    :message
    (describe (maybe String) "The message associated with the current state/phase of the pod")

    :reason
    (describe (maybe String) "The reason the pod is in the phase")

    :containerStatuses
    (describe [ContainerStatus] "The list of container statuses for the pod")

    :initContainerStatuses
    (describe [ContainerStatus] "The list of container status for the init containers in the pod")}))

(defschema ConfigMap
  (merge
   BaseListing
   {:data (describe Any "The data of the config map")}))

(defschema ServicePort
  {:name
   (describe String "The name of the port")

   (optional-key :nodePort)
   (describe (maybe Long) "The exposed port on the k8s nodes")

   (optional-key :targetPort)
   (describe (maybe Long) "The target port in the selected pods. Will not be present if targetPortName is set")

   (optional-key :targetPortName)
   (describe (maybe String) "The name of the target port on the selected pods. Will not be present if targetPort is set")

   :port
   (describe Long "The service port")

   :protocol
   (describe String "The protocol the primary service port supports")})

(defschema Service
  (merge
   BaseListing
   {:ports (describe [ServicePort] "The list of ports open in the service")}))

(def Group
  (describe String "A Kubernetes API group name"))

(def Kind
  (describe String "The type of a Kubernetes resource"))

(def ObjectName
  (describe String "The name of a Kubernetes resource"))

(def Namespace
  (describe String "The name of a Kubernetes namespace"))

(def Duration
  (describe String "A period of time specified in GEP-2257 format"))

(def PortNumber
  (describe Long "A port number"))

(def PreciseHostname
  (describe String "The fully qualified domain name of a network host"))

(def http-method-set #{:GET :HEAD :POST :PUT :DELETE :CONNECT :OPTIONS :TRACE :PATCH})

(def HTTPMethodWithWildcard
  (describe (apply s/enum (conj http-method-set :*)) "The name of an HTTP method or * to match any HTTP method"))

(def HTTPMethod
  (describe (apply s/enum http-method-set) "The name of an HTTP method"))

(def HTTPPathModifierType
  (describe (s/enum :ReplaceFullMatch :ReplacePrefixMatch) "The type of path modification to perform"))

(defschema HTTPPathModifier
  {(optional-key :type)               (describe HTTPPathModifierType "The type of path modification to perform")
   (optional-key :replaceFullPath)    (describe String "The replacement for the full path of the original URL")
   (optional-key :replacePrefixMatch) (describe String "The replacement for the path prefix")})

(defschema LocalObjectReference
  {(optional-key :group) (describe Group "The Kubernetes API group of the referent")
   (optional-key :kind)  (describe Kind "The kind of Kubernetes resource being referenced")
   (optional-key :name)  (describe ObjectName "The name of the referent")})

(defschema BackendObjectReference
  {(optional-key :group)     (describe Group "The Kubernetes API group of the referent")
   (optional-key :kind)      (describe Kind "The kind of Kubernetes resource being referenced")
   (optional-key :name)      (describe ObjectName "The name of the referent")
   (optional-key :namespace) (describe Namespace "The Kubernetes namespace of the referent")
   (optional-key :port)      (describe Long "The destination port number to use for the referent")})

(def HTTPHeaderName
  (describe String "The name of an HTTP header"))

(def PathMatchType
  (describe
   (s/enum :Exact :PathPrefix :RegularExpression)
   "The method used to compare the path match value to the path"))

(defschema HTTPPathMatch
  {:type  (describe PathMatchType "Defines how to match against the path value")
   :value (describe String "The path value to match against")})

(defschema HTTPHeaderMatch
  {:type  (describe (s/enum :Exact :RegularExpression) "Specifies how to match against the query parameter value")
   :name  (describe HTTPHeaderName "The name of the HTTP header to match, must be an exact string match")
   :value (describe String "The value of the HTTP header to match")})

(defschema HTTPQueryParamMatch
  {:type  (describe (s/enum :Exact :RegularExpression) "Specifies how to match against the query parameter value")
   :name  (describe String "The name of the HTTP query parameter to match, must be an exact string match")
   :value (describe String "The value of the HTTP query parameter to match")})

(defschema HTTPRouteMatch
  {(optional-key :path)        (describe HTTPPathMatch "HTTP path matcher configuration")
   (optional-key :headers)     (describe [HTTPHeaderMatch] "HTTP header matcher configurations")
   (optional-key :queryParams) (describe [HTTPQueryParamMatch] "HTTP query parameter matcher configurations")
   (optional-key :method)      (describe HTTPMethod "HTTP method matcher configuration")})

(def HTTPRouteFilterType
  (describe
   (s/enum
    :RequestHeaderModifier
    :ResponseHeaderModifier
    :RequestRedirect
    :URLRewrite
    :RequestMirror
    :CORS
    :ExternalAuth
    :ExtensionRef)
   "The type of filter to apply to HTTP requests"))

(defschema HTTPHeader
  {:name  (describe HTTPHeaderName "The name of the HTTP header")
   :value (describe String "The value of the HTTP header")})

(defschema HTTPHeaderFilter
  {(optional-key :set)    (describe [HTTPHeader] "HTTP headers to overwrite, replacing existing values")
   (optional-key :add)    (describe [HTTPHeader] "HTTP headers to add, appending to existing headers")
   (optional-key :remove) (describe [String] "The names of HTTP headers to remove")})

(defschema Fraction
  {:numerator   (describe Long "The numerator of the fraction")
   :denominator (describe Long "The denominator of the fraction")})

(defschema HTTPRequestMirrorFilter
  {(optional-key :backendRef)
   (describe BackendObjectReference "Refers to the backend where mirrored requests are sent")

   (optional-key :percent)
   (describe Long "The percentage of requests that should be sent to the backend")

   (optional-key :fraction)
   (describe Fraction "The fraction of requests that should be sent to the backend")})

(defschema HTTPRequestRedirectFilter
  {(optional-key :scheme)     (describe (s/enum :http :https) "The scheme to use in the Location response header")
   (optional-key :hostname)   (describe PreciseHostname "The hostname to use in the Location response header")
   (optional-key :path)       (describe HTTPPathModifier "URL path modification settings")
   (optional-key :port)       (describe PortNumber "The port to use in the Location response header")
   (optional-key :statusCode) (describe (s/enum 301 302) "The HTTP status code to use in the response")})

(defschema HTTPURLRewriteFilter
  {(optional-key :hostname)
   (describe PreciseHostname "The value used to replace the Host header in the forwarded request")

   (optional-key :path)
   (describe HTTPPathModifier "Specifies the path modifications to apply to the request")})

(def CORSOrigin
  (describe String "The URI of an allowed origin or * to match any origin"))

(defschema HTTPCORSFilter
  {(optional-key :allowOrigins)
   (describe [CORSOrigin] "The origins to allow the response to be shared with")

   (optional-key :allowCredentials)
   (describe Boolean "Indicates whether credentials should be shared with the origin")

   (optional-key :allowMethods)
   (describe [HTTPMethodWithWildcard] "The HTTP methods allowed for the origin")

   (optional-key :allowHeaders)
   (describe [HTTPHeaderName] "The HTTP request headers allowed for the origin")

   (optional-key :exposeHeaders)
   (describe [HTTPHeaderName] "The HTTP response headers to expose to the origin")

   (optional-key :maxAge)
   (describe Long "The number of seconds for the client to cache preflight request results")})

(def HTTPRouteExternalAuthProtocol
  (describe (s/enum :GRPC :HTTP) "The external auth protocol to use"))

(defschema GRPCAuthConfig
  {(optional-key :allowedHeaders) (describe [String] "The HTTP request headers to forward to the auth server")})

(defschema HTTPAuthConfig
  {(optional-key :path)
   (describe String "The HTTP path prefix to use for the auth server")

   (optional-key :allowedHeaders)
   (describe [String] "The HTTP request headers to forward to the auth server")

   (optional-key :allowedResponseHeaders)
   (describe [String] "The HTTP headers from the authorization response to copy to the backend request")})

(defschema ForwardBodyConfig
  {(optional-key :maxSize) (describe Long "The maximum size of the body to forward to the auth server")})

(defschema HTTPExternalAuthFilter
  {(optional-key :protocol)
   (describe HTTPRouteExternalAuthProtocol "The protocol to use when communicating with the auth server")

   (optional-key :backendRef)
   (describe BackendObjectReference "A reference to the backend to send auth requests to")

   (optional-key :grpc)
   (describe GRPCAuthConfig "Configuration for communication with ext_authz backends")

   (optional-key :http)
   (describe HTTPAuthConfig "Configuration for communication with HTTP auth backends")

   (optional-key :forwardBody)
   (describe ForwardBodyConfig "Controls whether request bodies will be forwarded to the auth server")})

(defschema HTTPRouteFilter
  {(optional-key :type)                   (describe HTTPRouteFilterType "The type of filter to apply to the request")
   (optional-key :requestHeaderModifier)  (describe HTTPHeaderFilter "Request header modification settings")
   (optional-key :responseHeaderModifier) (describe HTTPHeaderFilter "Response header modification settings")
   (optional-key :requestMirror)          (describe HTTPRequestMirrorFilter "Request mirroring settings")
   (optional-key :requestRedirect)        (describe HTTPRequestRedirectFilter "Request redirect settings")
   (optional-key :urlRewrite)             (describe HTTPURLRewriteFilter "URL rewrite settings")
   (optional-key :cors)                   (describe HTTPCORSFilter "CORS settings")
   (optional-key :externalAuth)           (describe HTTPExternalAuthFilter "External auth settings")
   (optional-key :extensionRef)           (describe LocalObjectReference "Implementation-specific filter settings")})

(defschema HTTPBackendRef
  {(optional-key :group)     (describe Group "The Kubernetes API group of the referent")
   (optional-key :kind)      (describe Kind "The kind of Kubernetes resource being referenced")
   (optional-key :name)      (describe ObjectName "The name of the referent")
   (optional-key :namespace) (describe Namespace "The namespace of the referent")
   (optional-key :port)      (describe PortNumber "The destination port to use for the resource")
   (optional-key :weight)    (describe Long "The proportion of requests to forward to the referenced backend")
   (optional-key :filters)   (describe [HTTPRouteFilter] "Filters to apply to requests forwarded to the backend")})

(defschema HTTPRouteTimeouts
  {(optional-key :request)
   (describe Duration "The maximum amount of time for a gateway to respond to a request")

   (optional-key :backendRequest)
   (describe Duration "The maximum amount of time for a request from the gateway to a backend")})

(def HTTPRouteRetryStatusCode
  (describe Long "An HTTP status code between 400 and 599"))

(defschema HTTPRouteRetry
  {(optional-key :codes)    (describe [HTTPRouteRetryStatusCode] "Status codes that should trigger a retry")
   (optional-key :attempts) (describe Long "The maximum number of retry attempts")
   (optional-key :backoff)  (describe Duration "The minimum amount of time between retries")})

(def SessionPersistenceType
  (describe (s/enum :Cookie :Header) "Specifies whether the session is stored in a cookie or in a header"))

(def CookieLifetimeType
  (describe (s/enum :Session :Persistent) "Specifies whether the cookie has a permanent or session-based lifetime"))

(defschema CookieConfig
  {(optional-key :lifetimeType)
   (describe CookieLifetimeType "Specifies whether the cookie has a permanent or session-based lifetime")})

(defschema SessionPersistence
  {(optional-key :sessionName)     (describe String "The name of the session token")
   (optional-key :absoluteTimeout) (describe Duration "The absolute timeout of the session")
   (optional-key :idleTimeout)     (describe Duration "The idle timeout of the session")
   (optional-key :type)            (describe SessionPersistenceType "The session storage mechanism")
   (optional-key :cookieConfig)    (describe CookieConfig "Cookie-based session storage configuration")})

(defschema HTTPRouteRule
  {(optional-key :name)               (describe String "The name of the HTTPRoute")
   (optional-key :matches)            (describe [HTTPRouteMatch] "The list of matching route specifications")
   (optional-key :filters)            (describe [HTTPRouteFilter] "The list of filters applied to this rule")
   (optional-key :backendRefs)        (describe [HTTPBackendRef] "The backends where matching requests should be routed")
   (optional-key :timeouts)           (describe HTTPRouteTimeouts "Configurable timeouts for the route")
   (optional-key :retry)              (describe HTTPRouteRetry "Configuration indicating when to retry a request")
   (optional-key :sessionPersistence) (describe SessionPersistence "Session persistence configuration")})

(defschema HTTPRoute
  (merge
   BaseListing
   {:rules (describe [HTTPRouteRule] "Rules used to match requests with the route")}))

(defschema FullResourceListing
  {:deployments (describe [Deployment] "The list of deployments")
   :pods        (describe [Pod] "The list of pods")
   :configMaps  (describe [ConfigMap] "The list of config maps")
   :services    (describe [Service] "The list of services")
   :routes      (describe [HTTPRoute] "The list of HTTP routes")})

(defschema FilterParams
  {(optional-key :analysis-name)
   (describe (maybe String) "The name of the analysis")

   (optional-key :app-id)
   (describe (maybe UUID) "The UUID of the running app for the analysis")

   (optional-key :app-name)
   (describe (maybe String) "The name of the running app for the analysis")

   (optional-key :external-id)
   (describe (maybe UUID) "The value of the external_id field in the job_steps table. Used widely in the API")

   (optional-key :user-id)
   (describe (maybe UUID) "The UUID assigned to the user that launched the analysis")

   (optional-key :username)
   (describe (maybe String) "The user of the user that launched the analysis")})

(defschema NonAdminFilterParams
  (dissoc FilterParams (optional-key :user-id) (optional-key :username)))

(def URLReadySummary "Whether or not a VICE analysis is browser accessible")

(def URLReadyDescription
  "Returns a map detailing whether or not the VICE analysis is ready to be accessed through a browser")

(def DescriptionSummary "Returns a JSON description of the VICE analysis")
(def DescriptionDescription "Returns a JSON description of the VICE analysis after verifying the user has access to it")
(def AdminDescriptionDescription "Returns a JSON description of the VICE analysis, bypassing permissions checks")

(def Host (describe String "The hostname/subdomain of a VICE analysis"))

(defschema URLReady
  {:ready                          (describe Boolean "Whether or not the analysis is ready to be accessed.")
   (optional-key :access_url)      (describe (maybe String) "The access URL for the analysis, if ready.")})
