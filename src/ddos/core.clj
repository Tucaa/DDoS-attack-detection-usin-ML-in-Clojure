(ns ddos.core
  (:require [ddos.attacks :as attacks]
            [ddos.normal :as normal]
            [ddos.windowing :as w]
            [ddos.export :as export])
  (:gen-class))



;; Po potrebi korigovati parametre (za pocetak je stavljeno 1k uzoraka svakog patterna)
;; Vidi da li ces ovde da mesas saobracaj ili ces to uraditi u drugim funkcijama u normal.clj i attack.clj
(defn -main [& _]
  (let [start-ts (System/currentTimeMillis)
        window-ms 5000
        samples
        (concat
         ;;  Attacck patterni
         (w/generate-windows attacks/udp-large-packets 1000 start-ts window-ms)
         ;;  (w/generate-windows attacks/udp-empty-packets 1000 start-ts window-ms)
         ;;  (w/generate-windows attacks/tcp-ack-anomaly 1000 start-ts window-ms)
         (w/generate-windows attacks/subnet-carpet-bombing 1000 start-ts window-ms)
         (w/generate-windows attacks/syn-flood 1000 start-ts window-ms)
         (w/generate-windows attacks/icmp-flood 1000 start-ts window-ms)
         (w/generate-windows attacks/udp-flood-mixed 1000 start-ts window-ms)
         (w/generate-windows attacks/ntp-amplification 1000 start-ts window-ms)
         (w/generate-windows attacks/ack-flood 1000 start-ts window-ms)
         (w/generate-windows attacks/dns-amplification 1000 start-ts window-ms)
         ;; Normanal saobracaj
         (w/generate-windows normal/normal-web-traffic 1000 start-ts window-ms)
         (w/generate-windows normal/normal-enterprise-traffic 1000 start-ts window-ms)
         (w/generate-windows normal/normal-streaming-traffic 1000 start-ts window-ms)
         (w/generate-windows normal/normal-dns-traffic 1000 start-ts window-ms)
         (w/generate-windows normal/normal-email-traffic 1000 start-ts window-ms)
         (w/generate-windows normal/normal-email-traffic 1000 start-ts window-ms))]
    (export/write-csv samples "ddos_dataset.csv")
    (println "Dataset generated: ddos_dataset.csv")))