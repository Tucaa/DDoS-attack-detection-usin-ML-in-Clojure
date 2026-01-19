(ns ddos.core
  (:require [ddos.attacks :as attacks]
            [ddos.normal :as normal]
            [ddos.windowing :as w]
            [ddos.export :as export])
  (:gen-class))



;; Po potrebi korigovati parametre (za pocetak je stavljeno 1k uzoraka svakog patterna)
;; Vidi da li ces ovde da mesas saobracaj ili ces to uraditi u drugim funkcijama u normal.clj i attack.clj

;; Ovo je bila inicijalna f-ja za generisanje dataseta
;; (defn simple-dataset
;;   [window-ms]
;;   (let [start-ts (System/currentTimeMillis)
;;         window-ms 5000
;;         samples
;;         (->> (concat
;;               ;;  Attacck patterni
;;               (w/generate-windows attacks/udp-large-packets 1000 start-ts window-ms)
;;               ;;  (w/generate-windows attacks/udp-empty-packets 1000 start-ts window-ms)
;;               ;;  (w/generate-windows attacks/tcp-ack-anomaly 1000 start-ts window-ms)
;;               (w/generate-windows attacks/subnet-carpet-bombing 1000 start-ts window-ms)
;;               (w/generate-windows attacks/syn-flood 1000 start-ts window-ms)
;;               (w/generate-windows attacks/icmp-flood 1000 start-ts window-ms)
;;               (w/generate-windows attacks/udp-flood-mixed 1000 start-ts window-ms)
;;               (w/generate-windows attacks/ntp-amplification 1000 start-ts window-ms)
;;               (w/generate-windows attacks/ack-flood 1000 start-ts window-ms)
;;               (w/generate-windows attacks/dns-amplification 1000 start-ts window-ms)
;;               ;; Normanal saobracaj
;;               (w/generate-windows normal/normal-web-traffic 1000 start-ts window-ms)
;;               (w/generate-windows normal/normal-enterprise-traffic 1000 start-ts window-ms)
;;               (w/generate-windows normal/normal-streaming-traffic 1000 start-ts window-ms)
;;               (w/generate-windows normal/normal-dns-traffic 1000 start-ts window-ms)
;;               (w/generate-windows normal/normal-email-traffic 1000 start-ts window-ms)
;;               (w/generate-windows normal/normal-email-traffic 1000 start-ts window-ms))
;;              (map #(vector (rand) %))
;;              (sort-by first)
;;              (map second))]
;;     (export/write-csv samples "ddos_dataset.csv")
;;     (println "Dataset generated: ddos_dataset.csv")))



(defn generate-complete-dataset [window-ms]
  (w/generate-timeline window-ms)

   [:normal {:normal-fn normal/normal-mixed-traffic
             :num-windows 500}]
   
   [:attack {:attack-fn attacks/udp-large-packets
             :attack-type :udp-flood-large}]
   
   [:normal {:normal-fn normal/normal-mixed-traffic
             :num-windows 200}]
   
   [:attack {:attack-fn attacks/dns-amplification
             :attack-type :dns-amplification}]
   
   [:normal {:normal-fn normal/normal-mixed-traffic
             :num-windows 150}]
   
   [:campaign {:attack-fn attacks/syn-flood
               :attack-type :syn-flood
               :num-instances 4
               :duration-hours 8}]
   
   [:normal {:normal-fn normal/normal-mixed-traffic
             :num-windows 300}]
    [:attack {:attack-fn attacks/icmp-flood
              
             :attack-type :icmp-flood}]
   
   [:normal {:normal-fn normal/normal-mixed-traffic
             :num-windows 250}]
   
   [:attack {:attack-fn attacks/ack-flood
             :attack-type :ack-flood}]
   
   [:normal {:normal-fn normal/normal-mixed-traffic
             :num-windows 200}]
   
   [:attack {:attack-fn attacks/ntp-amplification
             :attack-type :ntp-amplification}]
   
   [:normal {:normal-fn normal/normal-mixed-traffic
             :num-windows 180}]
   
   [:attack {:attack-fn attacks/udp-flood-mixed
             :attack-type :udp-flood-mixed}]
   
   [:normal {:normal-fn normal/normal-mixed-traffic
             :num-windows 220}]
   
   [:attack {:attack-fn attacks/subnet-carpet-bombing
             :attack-type :subnet-carpet-bombing}]
   
   [:normal {:normal-fn normal/normal-mixed-traffic
             :num-windows 400}])
  

(defn -main [& args]
  (let [window-ms 5000
        data (do
               (println "Generating dataset")
               (generate-complete-dataset window-ms))]
    (println data)
    (export/write-csv data "new_ddos_dataset.csv")))
