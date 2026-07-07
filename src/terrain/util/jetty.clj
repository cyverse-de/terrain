(ns terrain.util.jetty
  "Jetty customization: per-endpoint connection idle timeouts.

   The ring-jetty adapter exposes only a single connector-level idle timeout, so a longer
   grace period for slow uploads would otherwise apply to every endpoint. Installing an
   HttpConfiguration customizer lets us keep a short default idle timeout while raising it
   only for the (long-lived, streaming) upload endpoint."
  (:import [org.eclipse.jetty.server Server Connector HttpConnectionFactory HttpConfiguration$Customizer Request]
           [java.util.function Consumer]))

(defn- idle-timeout-customizer
  "A per-request Jetty customizer. Sets the connection's idle timeout to upload-idle-ms for
   requests whose path satisfies upload-path?, and to default-idle-ms otherwise (so the value
   is correct even when a connection is reused across requests).

   For upload requests it also registers a completion listener that resets the idle timeout
   back to default-idle-ms once the request finishes. Otherwise the raised timeout would
   persist on a keep-alive connection until the next request ran customize, letting a
   post-upload connection linger for upload-idle-ms if the client stalls without sending
   another request."
  [upload-path? ^long default-idle-ms ^long upload-idle-ms]
  (reify HttpConfiguration$Customizer
    (customize [_ request _response-headers]
      (let [^Request request request
            endpoint (.. request getConnectionMetaData getConnection getEndPoint)
            path     (.. request getHttpURI getPath)]
        (if (upload-path? path)
          (do
            (.setIdleTimeout endpoint upload-idle-ms)
            (Request/addCompletionListener request
                                           (reify Consumer
                                             (accept [_ _failure]
                                               (.setIdleTimeout endpoint default-idle-ms)))))
          (.setIdleTimeout endpoint default-idle-ms)))
      request)))

(defn idle-timeout-configurator
  "Returns a ring-jetty :configurator fn that installs idle-timeout-customizer on every HTTP
   connection factory of the server."
  [upload-path? default-idle-ms upload-idle-ms]
  (fn [^Server server]
    (let [customizer (idle-timeout-customizer upload-path? (long default-idle-ms) (long upload-idle-ms))]
      (doseq [^Connector connector (.getConnectors server)
              factory              (.getConnectionFactories connector)
              :when                (instance? HttpConnectionFactory factory)]
        (.addCustomizer (.getHttpConfiguration ^HttpConnectionFactory factory) customizer)))))
