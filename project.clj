(defproject clj-photomanager "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-codec "1.0.0"]
                 [compojure "1.3.3"]
                 [cheshire "5.4.0"]
                 [http-kit "2.1.18"]
                 [org.clojure/core.memoize "0.5.6"]
                 [midje "1.6.3"]]
  :plugins [[lein-ring "0.9.3"]]
  :ring {:handler clj-photomanager.core/app}
  :main ^:skip-aot clj-photomanager.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
