(ns ddos.attacks
  (:require [ddos.radnom :as r]))


;; Abnormalna velicina UDP paketa
(defn udp-large-packets []
  (let [packet-rate (r/rand-normal 50000 6000)
        avg-size (r/rand-normal 1400 50)]
    {:label :udp-large
     :packet-rate (r/clamp packet-rate 20000 120000)
     :avg-packet-size (r/clamp avg-size 1200 1500)
     :std-packet-size (r/rand-uniform 20 80)
     :udp-ratio (r/rand-uniform 0.85 1.0)
     :tcp-ratio (r/rand-uniform 0.0 0.05)
     :icmp-ratio (r/rand-uniform 0.0 0.05)
     :unique-src-ips (r/rand-uniform 100 600)
     :unique-flows (r/rand-uniform 150 700)
     :top-ip-packet-share (r/rand-uniform 0.05 0.25)
     :top-ip-byte-share (r/rand-uniform 0.05 0.25)
     :src-ip-entropy (r/rand-uniform 3.5 5.0)
     :dst-port-entropy (r/rand-uniform 0.5 1.2)
     :dns-response-ratio (r/rand-uniform 0.0 0.05)
     :subnet-spread (r/rand-uniform 0.05 0.25)}))


;; UDP paketi bez  payloada
(defn udp-empty-packets []
  {:label :udp-empty
   :packet-rate (r/rand-uniform 60000 150000)
   :avg-packet-size (r/rand-uniform 28 60)
   :std-packet-size (r/rand-uniform 5 15)
   :udp-ratio (r/rand-uniform 0.9 1.0)
   :tcp-ratio 0.0
   :icmp-ratio (r/rand-uniform 0.0 0.05)
   :unique-src-ips (r/rand-uniform 200 800)
   :unique-flows (r/rand-uniform 300 1200)
   :top-ip-packet-share (r/rand-uniform 0.02 0.15)
   :top-ip-byte-share (r/rand-uniform 0.01 0.10)
   :src-ip-entropy (r/rand-uniform 4.0 5.5)
   :dst-port-entropy (r/rand-uniform 0.8 1.5)
   :dns-response-ratio 0.0
   :subnet-spread (r/rand-uniform 0.1 0.4)})

;; TCP ACK bez ACK number (Acknowledgement broj)
(defn tcp-ack-anomaly []
  {:label :tcp-ack-anomaly
   :packet-rate (r/rand-uniform 20000 60000)
   :avg-packet-size (r/rand-uniform 60 120)
   :std-packet-size (r/rand-uniform 10 30)
   :udp-ratio 0.0
   :tcp-ratio (r/rand-uniform 0.9 1.0)
   :icmp-ratio 0.0
   :ack-rate (r/rand-uniform 0.7 1.0)
   :ack-without-number-rate (r/rand-uniform 0.6 0.95)
   :syn-rate (r/rand-uniform 0.0 0.1)
   :unique-src-ips (r/rand-uniform 50 300)
   :unique-flows (r/rand-uniform 100 600)
   :top-ip-packet-share (r/rand-uniform 0.1 0.4)
   :src-ip-entropy (r/rand-uniform 2.5 4.0)})

;; DNS Amplification
(defn dns-amplification []
  {:label :dns-amplification
   :packet-rate (r/rand-uniform 30000 90000)
   :avg-packet-size (r/rand-uniform 900 1400)
   :std-packet-size (r/rand-uniform 80 200)
   :udp-ratio (r/rand-uniform 0.85 1.0)
   :tcp-ratio 0.0
   :dns-response-ratio (r/rand-uniform 0.7 1.0)
   :unique-src-ips (r/rand-uniform 500 5000)
   :src-ip-entropy (r/rand-uniform 4.5 6.0)
   :dst-port-entropy (r/rand-uniform 2.5 4.0)
   :top-ip-packet-share (r/rand-uniform 0.02 0.1)})


;; Subnet carpet Bombing
(defn subnet-carpet-bombing []
  {:label :subnet-carpet
   :packet-rate (r/rand-uniform 40000 100000)
   :avg-packet-size (r/rand-uniform 60 200)
   :udp-ratio (r/rand-uniform 0.6 0.9)
   :unique-src-ips (r/rand-uniform 1000 10000)
   :src-ip-entropy (r/rand-uniform 3.0 4.5)
   :subnet-spread (r/rand-uniform 0.6 0.95)
   :dst-port-entropy (r/rand-uniform 0.2 0.6)
   :top-ip-packet-share (r/rand-uniform 0.01 0.05)})

;; F-ja za generisanje podataka
(def attack-generators
  {:udp-large udp-large-packets
   :udp-empty udp-empty-packets
   :tcp-ack tcp-ack-anomaly
   :dns-amplification dns-amplification
   :subnet-carpet subnet-carpet-bombing})