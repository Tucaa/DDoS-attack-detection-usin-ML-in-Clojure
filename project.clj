(defproject ddos "0.1.0-SNAPSHOT"
  :description "Synthetic time-series DDoS attack dataset generator for machine learning"
  :url "https://example.com/ddos-synthetic"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.12.1"]
                 [org.clojure/data.csv "1.1.0"]
                 [org.clojure/tools.logging "1.2.4"]
                 [uncomplicate/deep-diamond "0.42.0"
                  ;; Za sada je excludovana cuda radi se samo na cpu
                  :exclusions [org.bytedeco/cuda-platform
                               org.bytedeco/dnnl-platform]]
                 [uncomplicate/neanderthal "0.60.0"]
                ;;  [org.bytedeco/mkl "2025.2-1.5.12" :classifier "windows-x86_64"]
                 [org.bytedeco/mkl "2025.2-1.5.12" :classifier "windows-x86_64-redist"]
                 [incanter "1.9.3"]]

  :repositories [["clojars" "https://repo.clojars.org/"]
                 ["central" "https://repo1.maven.org/maven2/"]]

  ;; main namespace entrypoint
  :main ^:skip-aot ddos.core

  ;; where compiled artifacts go
  :target-path "target/%s"

  ;; build standalone jar if needed
  :profiles {:uberjar {:aot :all}})
