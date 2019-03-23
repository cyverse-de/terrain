(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/terrain "2.12.0-SNAPSHOT"
  :description "Discovery Environment API gateway/API services catch-all project"
  :url "https://github.com/cyverse-de/terrain"
  :license {:name "BSD Standard License"
            :url "http://www.cyverse.org/sites/default/files/CyVerse-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "terrain-standalone.jar"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.codec "0.1.1"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [cheshire "5.6.3"]
                 [clj-http "3.4.1"]
                 [clj-time "0.12.2"]
                 [clojurewerkz/elastisch "2.2.0"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [compojure "1.5.1"]
                 [metosin/compojure-api "1.1.8"]   ; should be held to the same version as the one
                                                   ; used by org.iplantc/clojure-commons
                 [dire "0.5.4"]
                 [me.raynes/fs "1.4.6"]
                 [medley "0.8.4"]
                 [proto-repl "0.3.1"]
                 [org.apache.tika/tika-core "1.14"]      ; provides org.apache.tika
                 [ring/ring-jetty-adapter "1.5.0"]
                 [slingshot "0.12.2"]
                 [org.cyverse/clj-icat-direct "2.8.2"]
                 [org.cyverse/clj-jargon "2.8.3"]
                 [org.cyverse/clojure-commons "3.0.2"]
                 [org.cyverse/cyverse-groups-client "0.1.7"]
                 [org.cyverse/common-cfg "2.8.1"]
                 [org.cyverse/common-cli "2.8.1"]
                 [org.cyverse/common-swagger-api "2.10.4"]
                 [org.cyverse/kameleon "3.0.1"]
                 [org.cyverse/metadata-client "3.0.1"]
                 [org.cyverse/permissions-client "2.8.1"]
                 [org.cyverse/service-logging "2.8.0"]]
  :eastwood {:exclude-namespaces [terrain.util.jwt :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :plugins [[lein-ring "0.12.5" :exclusions [org.clojure/clojure]]
            [swank-clojure "1.4.2" :exclusions [org.clojure/clojure]]
            [test2junit "1.2.2"]
            [jonase/eastwood "0.3.5"]]
  :profiles {:dev     {:resource-paths ["conf/test"]}
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
