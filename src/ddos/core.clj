(ns ddos.core
  (:require [ddos.attacks :as attacks]
            [ddos.windowing :as w]
            [ddos.export :as export])
  (:gen-class))



;; Po potrebi korigovati parametre 
(defn -main [& _]
  (let [start-ts (System/currentTimeMillis)
        window-ms 5000
        samples
        (concat
         (w/generate-windows attacks/udp-large-packets 1000 start-ts window-ms)
         (w/generate-windows attacks/udp-empty-packets 1000 start-ts window-ms)
         (w/generate-windows attacks/tcp-ack-anomaly 1000 start-ts window-ms)
         (w/generate-windows attacks/dns-amplification 1000 start-ts window-ms))]
    (export/write-csv samples "ddos_dataset.csv")
    (println "Dataset generated: ddos_dataset.csv")))