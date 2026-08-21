(defproject org.cyverse/terrain "3.0.0-SNAPSHOT"
  :description "Discovery Environment API gateway/API services catch-all project"
  :url "https://github.com/cyverse-de/terrain"
  :license {:name "BSD Standard License"
            :url "https://cyverse.org/license"}
  :uberjar-name "terrain-standalone.jar"
  ;; Fail the build on a new dependency conflict rather than printing a
  ;; warning nobody reads.
  :pedantic? :abort
  ;; Records versions Leiningen already resolves, read off the resolved
  ;; classpath rather than copied from lein's "Consider using these
  ;; :managed-dependencies" hint -- that hint names the version that LOST the
  ;; conflict, so pasting it would be a silent upgrade.
  ;;
  ;; No jackson entries: databind/annotations only needed pinning because
  ;; clj-jargon dragged them down to 2.14.1, and terrain no longer depends on
  ;; clj-jargon. Nothing else asks for that version, so the family resolves on
  ;; its own; pinning it now would put databind below the jackson-core cheshire
  ;; brings.
  :managed-dependencies [[com.google.guava/guava "16.0.1"]
                         [commons-codec "1.16.1"]
                         [commons-io "2.16.1"]
                         [org.apache.commons/commons-fileupload2-core "2.0.0-M1"]
                         [org.ring-clojure/ring-core-protocols "1.13.0"]
                         [org.ring-clojure/ring-websocket-protocols "1.13.0"]
                         [prismatic/schema "1.1.12"]
                         [ring/ring-codec "1.2.0"]
                         [ring/ring-core "1.13.0"]]
  :dependencies [[org.clojure/clojure "1.12.5"]
                 [org.clojure/data.codec "0.2.1"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [cheshire "6.2.0"]
                 [clj-http "3.13.1"]
                 [clj-time "0.15.2"]
                 ;; Only the elastisch.native namespaces need the Elasticsearch jar; terrain uses the REST client.
                 [clojurewerkz/elastisch "3.0.1" :exclusions [org.elasticsearch/elasticsearch]]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [org.cyverse/dire "0.5.6"]
                 [me.raynes/fs "1.4.6" :exclusions [org.apache.commons/commons-compress]]
                 [dev.weavejester/medley "1.10.0"]
                 [metosin/ring-http-response "0.9.5"]
                 [potemkin "0.4.9"]
                 [org.apache.tika/tika-core "3.3.2" :exclusions [org.slf4j/slf4j-api]]
                 [ring/ring-jetty-adapter "1.15.5"]
                 [slingshot "0.12.2"]
                 [org.cyverse/async-tasks-client "0.0.6"]
                 [org.cyverse/clojure-commons "3.0.13"]
                 [org.cyverse/cyverse-groups-client "0.1.10"]
                 [org.cyverse/common-cfg "2.8.4"]
                 [org.cyverse/common-cli "2.8.3"]
                 [org.cyverse/common-swagger-api "3.4.24"]
                 [metosin/ring-swagger-ui "5.32.11"]
                 [org.cyverse/kameleon "3.0.11"
                  :exclusions [com.impossibl.pgjdbc-ng/pgjdbc-ng]]
                 [com.impossibl.pgjdbc-ng/pgjdbc-ng "0.8.9"]
                 [org.cyverse/metadata-client "3.2.2"]
                 [org.cyverse/metadata-files "2.1.2"]
                 [org.cyverse/permissions-client "2.8.6"]
                 [org.cyverse/service-logging "2.8.6"]]
  ;; Many of the org.cyverse libraries declare this development tool at compile scope, so it would
  ;; otherwise ship in the uberjar. Excluded globally rather than per-dependency to cover all of them.
  :exclusions [cider/cider-nrepl]
  :eastwood {:exclude-namespaces [terrain.util.jwt :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :plugins [[lein-ancient "1.0.0"]
            [lein-ring "0.12.6"]
            [test2junit "1.4.4"]
            [jonase/eastwood "1.4.3"]]
  ;; cljfmt lives in its own profile: its tree and test2junit's disagree on which
  ;; Clojure to use, which trips :pedantic? :abort on a conflict between two
  ;; plugins that never reaches the runtime classpath. This repo also carried the
  ;; deprecated `lein-cljfmt` coordinates, so cljfmt was not actually runnable
  ;; here. Format with `lein with-profile +cljfmt cljfmt check`.
  :profiles {:cljfmt  {:plugins [[dev.weavejester/lein-cljfmt "0.16.4"]]
                       :pedantic? :warn}
             :dev     {:dependencies [[clj-http-fake "1.0.4"]]
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
