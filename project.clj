(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/terrain "3.0.0-SNAPSHOT"
  :description "Discovery Environment API gateway/API services catch-all project"
  :url "https://github.com/cyverse-de/terrain"
  :license {:name "BSD Standard License"
            :url "http://www.cyverse.org/sites/default/files/CyVerse-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "terrain-standalone.jar"
  :middleware [lein-git-down.plugin/inject-properties]
  :git-down {org.cyverse.de/cyverse-de-protobufs {:coordinates cyverse-de/p}}
  :dependencies [[org.clojure/clojure "1.11.2"]
                 [org.clojure/data.codec "0.2.0"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [cheshire "5.12.0"]
                 [clj-http "3.12.3"]
                 [clj-time "0.15.2"]
                 [clojurewerkz/elastisch "2.2.0"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [compojure "1.6.1"]
                 [dire "0.5.4"]
                 [me.raynes/fs "1.4.6" :exclusions [org.apache.commons/commons-compress]]
                 [medley "1.4.0"]
                 [metosin/ring-http-response "0.9.3"]
                 [potemkin "0.4.7"]
                 [proto-repl "0.3.1"]
                 [org.apache.tika/tika-core "2.9.1"]
                 [ring/ring-jetty-adapter "1.8.0"]
                 [slingshot "0.12.2"]
                 [org.cyverse/async-tasks-client "0.0.4"]
                 [org.cyverse/clj-icat-direct "2.9.4"]
                 [org.cyverse/clj-jargon "3.1.0"
                  :exclusions [org.bouncycastle/bcprov-jdk16]]
                 [org.cyverse/clojure-commons "3.0.7"]
                 [org.cyverse/cyverse-groups-client "0.1.8"]
                 [org.cyverse/common-cfg "2.8.2"]
                 [org.cyverse/common-cli "2.8.1"]
                 [org.cyverse/common-swagger-api "3.4.4"]
                 [org.cyverse/kameleon "3.0.8"
                  :exclusion [com.impossibl.pgjdbc-ng/pgjdbc-ng]]
                 [com.impossibl.pgjdbc-ng/pgjdbc-ng "0.8.9"]
                 [org.cyverse/metadata-client "3.1.1"]
                 [org.cyverse/metadata-files "2.1.0"]
                 [org.cyverse/otel "0.2.5"]
                 [org.cyverse/permissions-client "2.8.3"]
                 [org.cyverse/service-logging "2.8.3"]
                 [io.nats/jnats "2.17.3"]
                 [less-awful-ssl "1.0.6"]
                 [clojure.java-time "1.4.2"]
                 [com.appsflyer/pronto "2.1.2"]
                 [org.cyverse.de/cyverse-de-protobufs "2317731ae9fac1ecce3cedfb7ee168ba28305d73"]]
  :eastwood {:exclude-namespaces [terrain.util.jwt :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :plugins [[lein-ancient "0.7.0"]
            [lein-cljfmt "0.6.4"]
            [lein-ring "0.12.5" :exclusions [org.clojure/clojure]]
            [swank-clojure "1.4.2" :exclusions [org.clojure/clojure]]
            [test2junit "1.2.2"]
            [jonase/eastwood "1.4.2"]
            [reifyhealth/lein-git-down "0.4.1"]]
  :profiles {:dev     {:dependencies [[clj-http-fake "1.0.4"]]
                       :resource-paths ["conf/test" "test-resources"]
                       :jvm-opts ["-Dotel.javaagent.enabled=false"]}
             :uberjar {:aot :all}}
  :main ^:skip-aot terrain.core
  :ring {:handler terrain.core/dev-handler
         :init terrain.core/lein-ring-init
         :port 31325
         :auto-reload? false}
  :uberjar-exclusions [#".*[.]SF" #"LICENSE" #"NOTICE"]
  :repositories [["cyverse-de"
                  {:url "https://raw.github.com/cyverse-de/mvn/master/releases"}]
                 ["sonatype-releases"
                  {:url "https://oss.sonatype.org/content/repositories/releases/"}]
                 ["public-github" {:url "git://github.com" :protocol :https}]]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/terrain-logging.xml"
             "-javaagent:./opentelemetry-javaagent.jar"
             "-Dotel.resource.attributes=service.name=terrain"])
