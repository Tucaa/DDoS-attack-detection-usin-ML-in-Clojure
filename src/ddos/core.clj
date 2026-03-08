(ns ddos.core
  (:require [ddos.attacks :as attacks]
            [ddos.normal :as normal]
            [ddos.windowing :as w]
            [ddos.importexport :as ie])
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
;;     (export/write-csv samples "ddos_simple_dataset.csv")
;;     (println "Dataset generated: ddos_simple_dataset.csv")))



(defn generate-complete-dataset [window-ms]
  (w/generate-timeline window-ms

                       [:normal {:normal-fn normal/normal-mixed-traffic
                                 :num-windows 500}]

                       [:attack {:attack-fn attacks/udp-large-packets
                                 :attack-type :udp-large-packets}]

                       [:normal {:normal-fn normal/normal-mixed-traffic
                                 :num-windows 200}]

                       [:attack {:attack-fn attacks/dns-amplification
                                 :attack-type :dns-amplification}]

                       [:normal {:normal-fn normal/normal-mixed-traffic
                                 :num-windows 150}]

                       [:vector {:attack-fn attacks/udp-large-packets
                                 :attack-type :udp-large-packets
                                 :instances 4
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
                                 :num-windows 400}]))


(defn generate-mixed-dataset
  [window-ms days]
  (let [start-ts (System/currentTimeMillis)
        day-ms (* 24 60 60 1000)
        total-ms (* days day-ms)
        ;; Kranji timestamp za porveru
        end-ts (+ start-ts total-ms)] 

    (loop [curr-ts start-ts
           all-windows []]
      (if (>= curr-ts end-ts)
        all-windows ; Uslov za kraj i vraćanje rezultata
        (let [timeline-attacks
              (w/generate-timeline
               window-ms
               [:attack {:attack-fn attacks/syn-flood
                         :attack-type :syn-flood}]
               [:normal {:normal-fn normal/normal-mixed-traffic
                         :num-windows 100}]
               [:attack {:attack-fn attacks/dns-amplification
                         :attack-type :dns-amplification}]
               [:normal {:normal-fn normal/normal-mixed-traffic
                         :num-windows 100}]
               [:attack {:attack-fn attacks/subnet-carpet-bombing
                         :attack-type :subnet-carpet-bombing}])

              balanced-samples
              (mapcat
               (fn [attack-type attack-fn offset]
                 (map-indexed
                  (fn [idx sample]
                    (assoc sample
                           :label attack-type
                           :window-id idx
                           ;; Svaki uzorak dobija razlicit timestamp
                           :timestamp (+ curr-ts (* (+ offset idx) window-ms))
                           :ts_formated (w/format-timestamp (+ curr-ts (* (+ offset idx) window-ms)))
                           ;; Stavljeno je da su svi napadi aktivni
                           :attack-active true))
                  (repeatedly 100 attack-fn)))
               ["udp-flood-large" "icmp-flood" "udp-flood-mixed" "ntp-amplification" "ack-flood"]
               [attacks/udp-large-packets attacks/icmp-flood attacks/udp-flood-mixed attacks/ntp-amplification attacks/ack-flood]
               ;; Offset da se timestamps ne preklapaju izmedju klasa
               (map #(* % 100) (range 5)))

              last-timeline-ts
              (if (empty? timeline-attacks)
                curr-ts
                (:timestamp (last timeline-attacks)))

              extra-normal (w/generate-windows-normal normal/normal-mixed-traffic 1000 curr-ts window-ms)
              last-ts (if (empty? extra-normal) last-timeline-ts (:timestamp (last extra-normal)))]

          (recur (+ last-ts window-ms)
                 (concat all-windows timeline-attacks balanced-samples extra-normal)))))))


(defn -main [& args]
  ;; Ovde mozes da dodas da se dinamicki uzima argument kada se pokrene lein run pa da na osnovu pozivas jednu od funkcija i pises podatke
  (let [window-ms 5000
        ;; Uslov za definisanje prilikom poziva fje 
        days (if (empty? args) 1 (Integer/parseInt (first args)))
        data (do
               (println "Generating dataset")
               (generate-mixed-dataset window-ms days))]
    ;;  (generate-complete-dataset window-ms))]
    ;;  (generate-complete-dataset window-ms))]
    (println data)
    (ie/write-csv data "new_ddos_dataset.csv")))
