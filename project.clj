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
            :url "https://cyverse.org/license"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "terrain-standalone.jar"
  :dependencies [[org.clojure/clojure "1.12.1"]
                 [org.clojure/data.codec "0.2.0"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [cheshire "6.0.0"]
                 [clj-http "3.13.1"]
                 [clj-time "0.15.2"]
                 [clojurewerkz/elastisch "3.0.1"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [org.cyverse/dire "0.5.6"]
                 [me.raynes/fs "1.4.6" :exclusions [org.apache.commons/commons-compress]]
                 [medley "1.4.0"]
                 [metosin/ring-http-response "0.9.5"]
                 [potemkin "0.4.8"]
                 [org.apache.tika/tika-core "3.2.2" :exclusions [org.slf4j/slf4j-api]]
                 [ring/ring-jetty-adapter "1.14.2"]
                 [slingshot "0.12.2"]
                 [org.cyverse/async-tasks-client "0.0.5"]
                 [org.cyverse/clj-icat-direct "2.9.7"]
                 [org.cyverse/clj-jargon "3.1.4"
                  :exclusions [org.bouncycastle/bcprov-jdk16]]
                 [org.cyverse/clojure-commons "3.0.11"]
                 [org.cyverse/cyverse-groups-client "0.1.9"]
                 [org.cyverse/common-cfg "2.8.3"]
                 [org.cyverse/common-cli "2.8.2"]
                 [org.cyverse/common-swagger-api "3.4.13"]
                 [org.cyverse/kameleon "3.0.10"
                  :exclusion [com.impossibl.pgjdbc-ng/pgjdbc-ng]]
                 [com.impossibl.pgjdbc-ng/pgjdbc-ng "0.8.9"]
                 [org.cyverse/metadata-client "3.2.0"]
                 [org.cyverse/metadata-files "2.1.1"]
                 [org.cyverse/permissions-client "2.8.4"]
                 [org.cyverse/service-logging "2.8.5"]
                 [io.nats/jnats "2.21.5"]
                 [less-awful-ssl "1.0.7"]
                 [clojure.java-time "1.4.3"]
                 [com.appsflyer/pronto "3.0.0"]
                 [org.cyverse/cyverse-de-protobufs "0.0.5"]]
  :eastwood {:exclude-namespaces [terrain.util.jwt :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :plugins [[lein-ancient "0.7.0"]
            [lein-cljfmt "0.9.2"]
            [lein-ring "0.12.6"]
            [test2junit "1.4.4"]
            [jonase/eastwood "1.4.3"]]
  :profiles {:dev     {:dependencies [[clj-http-fake "1.0.4"]]
                       :resource-paths ["conf/test" "test-resources"]}
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
                  {:url "https://oss.sonatype.org/content/repositories/releases/"}]]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/terrain-logging.xml"])
