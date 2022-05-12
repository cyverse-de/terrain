(ns terrain.routes.schemas.vice
  (:use [common-swagger-api.schema :only [describe NonBlankString]]
        [schema.core :only [defschema Any maybe optional-key]])
  (:import [java.util UUID]))

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
  {:name                       (describe String "The name of the container")
   :ready                      (describe Boolean "Whether or not the container is ready")
   :restartCount               (describe Long "The number of times the container has restarted")
   :state                      (describe ContainerState "The current state of the container")
   :lastState                  (describe ContainerState "The previous state of the container")
   :image                      (describe String "The image name used for the container")
   :imageID                    (describe String "The image ID assocaited with the container")
   (optional-key :containerID) (describe String "The ID associated with the container")
   (optional-key :started)     (describe Boolean "Whether or not the container has started")})

(defschema Pod
  (merge
   BaseListing
   {:phase                 (describe String "The pod phase")
    :message               (describe (maybe String) "The message associated with the current state/phase of the pod")
    :reason                (describe (maybe String) "The reason the pod is in the phase")
    :containerStatuses     (describe [ContainerStatus] "The list of container statuses for the pod")
    :initContainerStatuses (describe [ContainerStatus] "The list of container status for the init containers in the pod")}))

(defschema ConfigMap
  (merge
   BaseListing
   {:data (describe Any "The data of the config map")}))

(defschema ServicePort
  {:name                          (describe String "The name of the port")
   (optional-key :nodePort)       (describe (maybe Long) "The exposed port on the k8s nodes")
   (optional-key :targetPort)     (describe (maybe Long) "The target port in the selected pods. Will not be present if targetPortName is set")
   (optional-key :targetPortName) (describe (maybe String) "The name of the target port on the selected pods. Will not be present if targetPort is set")
   :port                          (describe Long "The service port")
   :protocol                      (describe String "The protocol the primary service port supports")})

(defschema Service
  (merge
   BaseListing
   {:ports (describe [ServicePort] "The list of ports open in the service")}))

(defschema IngressRule
  {:host (describe String "The host the rule applies to")
   :http (describe Any "The content of the rule")}) ; Yes, I got lazy and didn't want to model the entire thing.

(defschema Ingress
  (merge
   BaseListing
   {:rules (describe [IngressRule] "The list of rules making up the Ingress")
    :defaultBackend (describe String "The default service that accepts ingress requests that match no rules")}))

(defschema FullResourceListing
  {:deployments (describe [Deployment] "The list of deployments")
   :pods        (describe [Pod] "The list of pods")
   :configMaps  (describe [ConfigMap] "The list of config maps")
   :services    (describe [Service] "The list of services")
   :ingresses   (describe [Ingress] "The list of ingresses")})

(defschema FilterParams
  {(optional-key :analysis-name) (describe (maybe String) "The name of the analysis")
   (optional-key :app-id)        (describe (maybe UUID) "The UUID of the running app for the analysis")
   (optional-key :app-name)      (describe (maybe String) "The name of the running app for the analysis")
   (optional-key :external-id)   (describe (maybe UUID) "The value of the external_id field in the job_steps table. Used widely in the API")
   (optional-key :user-id)       (describe (maybe UUID) "The UUID assigned to the user that launched the analysis")
   (optional-key :username)      (describe (maybe String) "The user of the user that launched the analysis")})

(defschema NonAdminFilterParams
  (dissoc FilterParams (optional-key :user-id) (optional-key :username)))

(def URLReadySummary "Whether or not a VICE analysis is browser accessible")
(def URLReadyDescription "Returns a map detailing whether or not the VICE analysis is ready to be accessed through a browser")
(def AdminURLReadySummary "Whether or not a VICE analysis is browser accessible. No permissions checks")
(def AdminURLReadyDescription "Returns a map detailing whether or not the VICE analysis is ready to be accessed through a browser. No permissions checks.")

(def DescriptionSummary "Returns a JSON description of the VICE analysis")
(def DescriptionDescription "Returns a JSON description of the VICE analysis after verifying the user has access to it")
(def AdminDescriptionDescription "Returns a JSON description of the VICE analysis, bypassing permissions checks")

(def Host (describe String "The hostname/subdomain of a VICE analysis"))

(defschema URLReady
  {:ready (describe Boolean "Whether or not the analysis is ready to be accessed.")})