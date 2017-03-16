(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/terrain "2.10.0-SNAPSHOT"
  :description "Discovery Environment API gateway/API services catch-all project"
  :url "https://github.com/cyverse-de/terrain"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "terrain-standalone.jar"
  :dependencies [[org.clojure/clojure "1.8.0"]
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
                 [org.apache.tika/tika-core "1.14"]      ; provides org.apache.tika
                 [org.nexml.model/nexml "1.5-SNAPSHOT"]  ; provides org.nexml.model
                 [org.biojava.thirdparty/forester "1.005" ]
                 [slingshot "0.12.2"]
                 [org.cyverse/clj-icat-direct "2.8.0"]
                 [org.cyverse/clj-jargon "2.8.0"]
                 [org.cyverse/clojure-commons "2.8.1"]
                 [org.cyverse/tree-urls-client "2.8.1"]
                 [org.cyverse/common-cfg "2.8.1"]
                 [org.cyverse/common-cli "2.8.1"]
                 [org.cyverse/kameleon "3.0.1"]
                 [org.cyverse/heuristomancer "2.8.0"]
                 [org.cyverse/service-logging "2.8.0"]]
  :eastwood {:exclude-namespaces [terrain.util.jwt :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :plugins [[lein-ring "0.9.2" :exclusions [org.clojure/clojure]]
            [swank-clojure "1.4.2" :exclusions [org.clojure/clojure]]
            [test2junit "1.2.2"]
            [jonase/eastwood "0.2.3"]]
  :profiles {:dev     {:resource-paths ["conf/test"]}
             :uberjar {:aot :all}}
  :main ^:skip-aot terrain.core
  :ring {:handler terrain.core/app
         :init terrain.core/lein-ring-init
         :port 31325
         :auto-reload? false}
  :uberjar-exclusions [#".*[.]SF" #"LICENSE" #"NOTICE"]
  :repositories [["biojava"
                  {:url "http://www.biojava.org/download/maven"}]
                 ["sonatype-releases"
                  {:url "https://oss.sonatype.org/content/repositories/releases/"}]
                 ["nexml"
                  {:url "http://nexml.github.io/maven/repository"
                   :checksum :ignore}]]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/terrain-logging.xml"])
