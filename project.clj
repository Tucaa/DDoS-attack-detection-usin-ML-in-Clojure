(defproject ddos "0.1.0-SNAPSHOT"
  :description "Synthetic time-series DDoS attack dataset generator for machine learning"
  :url "https://example.com/ddos-synthetic"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 ;; CSV writer
                 [org.clojure/data.csv "1.1.0"]
                 ;; logging (useful later when simulating attacks)
                 [org.clojure/tools.logging "1.2.4"]]

  ;; main namespace entrypoint
  :main ^:skip-aot ddos.core

  ;; where compiled artifacts go
  :target-path "target/%s"

  ;; build standalone jar if needed
  :profiles {:uberjar {:aot :all}})
