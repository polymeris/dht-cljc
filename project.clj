(defproject com.jamesleonis/dht-cljc "0.1.0"
  :description "A Clojure(script) BitTorrent DHT plubming library"
  :url "https://github.com/jamesleonis/dht-cljc"
  :license {:name "Eclipse Public License - v 2.0"
            :url "https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.txt"}

  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.10.339" :scope "provided"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :source-paths ["src"]
  :test-paths ["test"]

  :aliases
  {"cljs-test" ["do" "clean," "cljsbuild" "once" "tests"]
   "test-all" ["do" "clean," "cljsbuild" "once" "tests," "test"]
   "cljs-auto-test" ["cljsbuild" "auto" "tests"]}

  :cljsbuild
  {:test-commands {"unit-tests" ["node" "target/unit-tests.js"]}
   :builds
   {:tests
    {:source-paths ["src" "test"]
     :notify-command ["node" "target/unit-tests.js"]
     :compiler {:output-to "target/unit-tests.js"
                :optimizations :none
                :target :nodejs
                :main dht-cljc.runner}}
    :prod
    {:source-paths ["src"]
     :compiler {:output-to "target/dht-cljc.js"
                :output-dir "target/cljsbuild/main"
                :optimizations :advanced}}}})
