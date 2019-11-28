(defproject chicago "1.0.0"
  :description "Dataflows you can't refuse"
  :plugins [[lein-tools-deps "0.4.3"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files ["deps.edn" :install :user :project]}
  :main ^:skip-aot chicago.core
  :profiles {:uberjar {:aot :all}})
