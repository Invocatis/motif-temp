(defproject motif "1.1.1-SNAPSHOT"
  :description "Recursive, data driven, pattern matching for Clojure"
  :url "https://github.com/invocatis/motif"

  :license {:name "GNU GENERAL PUBLIC LICENSE"
            :url "https://www.gnu.org/licenses/gpl.txt"}

  :dependencies [[org.clojure/clojure "1.9.0"]]

  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]

  :target-path "target/%s"

  :profiles
    {:uberjar {:aot :all}
     :dev {:dependencies [[org.clojure/tools.namespace "0.3.0-alpha4"]
                          [criterium "0.4.4"]]
           :plugins [[lein-doo "0.1.10"]]}}

  :doo
    {:build "test"
     :alias {:default [:node]}}

  :cljsbuild
    {:builds
     [{:id           "test"
       :source-paths ["src/main/clojure" "src/test/clojure" "src/test/cljs"]
       :compiler     {:main          motif.runner
                      :output-to     "target/doo/test.js"
                      :output-dir    "target/doo/out"
                      :target        :nodejs
                      :language-in   :ecmascript5
                      :optimizations :none}}]})
