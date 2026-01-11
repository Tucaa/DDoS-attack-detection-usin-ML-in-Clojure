(ns ddos.attacks
  (:require [ddos.radnom :as r]))


;; Abnormalna velicina UDP paketa
(defn udp-large-packets []
  (let [packet-rate (r/rand-normal 80000 15000)
        byte-rate (* packet-rate (r/rand-normal 1300 100))
        unique-src (r/rand-uniform 500 3000)]
    {:label "udp-flood-large"

     ;; Volumetrija
     :packet-rate (r/clamp packet-rate 40000 150000)
     :byte-rate (r/clamp byte-rate 50000000 200000000)
     :avg-packet-size (r/rand-normal 1300 100)
     :std-packet-size (r/rand-uniform 50 150)

     ;; Protocol distribucija
     :udp-ratio (r/rand-uniform 0.90 0.99)
     :tcp-ratio (r/rand-uniform 0.01 0.08)
     :icmp-ratio (r/rand-uniform 0.0 0.02)

     ;; TCP flags (nizak jer je UDP dominantan)
     :tcp-syn-ratio (r/rand-uniform 0.0 0.05)
     :tcp-ack-ratio (r/rand-uniform 0.0 0.05)
     :tcp-fin-ratio (r/rand-uniform 0.0 0.02)

     ;; IP 
     :unique-src-ips unique-src
     :unique-dst-ips (r/rand-uniform 5 50)
     :unique-flows (r/clamp (* unique-src (r/rand-uniform 1.2 2.0)) 600 5000)
     :src-ip-entropy (r/rand-uniform 4.0 6.0)
     :dst-ip-entropy (r/rand-uniform 1.0 2.5)

     ;; Patterni portova
     :dst-port-entropy (r/rand-uniform 0.5 2.0)
     :top-dst-port-share (r/rand-uniform 0.30 0.70)

     ;; Top talkeri
     :top-src-ip-packet-share (r/rand-uniform 0.03 0.15)
     :top-src-ip-byte-share (r/rand-uniform 0.03 0.15)

     ;; DNS (nizak jer nije DNS napad)
     :dns-query-ratio (r/rand-uniform 0.0 0.02)
     :dns-response-ratio (r/rand-uniform 0.0 0.02)

     ;; Subnet spread
     :dst-subnet-spread (r/rand-uniform 0.05 0.25)}))

;; DNS Amplification
(defn dns-amplification []
  (let [packet-rate (r/rand-normal 60000 12000)
        unique-src (r/rand-uniform 2000 10000)]
    {:label "dns-amplification"

     ;; Volumetrija (veliki paketi zbog DNS responsa)
     :packet-rate (r/clamp packet-rate 30000 120000)
     :byte-rate (* packet-rate (r/rand-uniform 800 1200))
     :avg-packet-size (r/rand-uniform 900 1400)
     :std-packet-size (r/rand-uniform 100 300)

     ;; Protocol (UDP dominantan)
     :udp-ratio (r/rand-uniform 0.92 0.99)
     :tcp-ratio (r/rand-uniform 0.01 0.05)
     :icmp-ratio (r/rand-uniform 0.0 0.03)

     ;; TCP flags
     :tcp-syn-ratio (r/rand-uniform 0.0 0.03)
     :tcp-ack-ratio (r/rand-uniform 0.0 0.03)
     :tcp-fin-ratio (r/rand-uniform 0.0 0.01)

     ;; IP diversity (mnogo source IP)
     :unique-src-ips unique-src
     :unique-dst-ips (r/rand-uniform 5 100)
     :unique-flows (r/clamp (* unique-src (r/rand-uniform 0.8 1.5)) 2000 15000)
     :src-ip-entropy (r/rand-uniform 5.0 7.0)
     :dst-ip-entropy (r/rand-uniform 1.5 3.0)

     ;; Port patterns (port 53 dominantan)
     :dst-port-entropy (r/rand-uniform 0.3 1.0)
     :top-dst-port-share (r/rand-uniform 0.80 0.98)

     ;; Top talkers (distribuirano)
     :top-src-ip-packet-share (r/rand-uniform 0.01 0.08)
     :top-src-ip-byte-share (r/rand-uniform 0.01 0.08)

     ;; DNS specific (KLJUČNA karakteristika!)
     :dns-query-ratio (r/rand-uniform 0.02 0.10)
     :dns-response-ratio (r/rand-uniform 0.85 0.98)

     ;; Subnet spread
     :dst-subnet-spread (r/rand-uniform 0.10 0.40)}))


;; Subnet carpet Bombing
(defn subnet-carpet-bombing []
  (let [packet-rate (r/rand-normal 70000 18000)
        unique-dst (r/rand-uniform 5000 20000)]
    {:label "subnet-carpet-bombing"

     ;; Volumetrija
     :packet-rate (r/clamp packet-rate 35000 140000)
     :byte-rate (* packet-rate (r/rand-uniform 100 400))
     :avg-packet-size (r/rand-uniform 64 250)
     :std-packet-size (r/rand-uniform 20 80)

     ;; Protocol (može biti mixed)
     :udp-ratio (r/rand-uniform 0.50 0.85)
     :tcp-ratio (r/rand-uniform 0.10 0.45)
     :icmp-ratio (r/rand-uniform 0.02 0.15)

     ;; TCP flags
     :tcp-syn-ratio (r/rand-uniform 0.05 0.30)
     :tcp-ack-ratio (r/rand-uniform 0.05 0.25)
     :tcp-fin-ratio (r/rand-uniform 0.01 0.10)

     ;; IP diversity (KLJUČNO: mnogo destination IPs u subnet-u)
     :unique-src-ips (r/rand-uniform 1000 8000)
     :unique-dst-ips unique-dst
     :unique-flows (r/clamp (* unique-dst (r/rand-uniform 0.9 1.3)) 5000 25000)
     :src-ip-entropy (r/rand-uniform 4.5 6.5)
     :dst-ip-entropy (r/rand-uniform 6.0 8.0) ;; Visoka dst entropija!

     ;; Port patterns
     :dst-port-entropy (r/rand-uniform 1.5 4.0)
     :top-dst-port-share (r/rand-uniform 0.10 0.35)

     ;; Top talkers
     :top-src-ip-packet-share (r/rand-uniform 0.01 0.06)
     :top-src-ip-byte-share (r/rand-uniform 0.01 0.06)

     ;; DNS
     :dns-query-ratio (r/rand-uniform 0.0 0.05)
     :dns-response-ratio (r/rand-uniform 0.0 0.05)

     ;; Subnet spread (KLJUČNA karakteristika - visoka!)
     :dst-subnet-spread (r/rand-uniform 0.70 0.95)}))

;; SYN Flood Attack
(defn syn-flood []
  (let [packet-rate (r/rand-normal 90000 20000)
        unique-src (r/rand-uniform 3000 15000)]
    {:label "syn-flood"

     ;; Volumetrija (manji paketi - samo SYN)
     :packet-rate (r/clamp packet-rate 50000 180000)
     :byte-rate (* packet-rate (r/rand-uniform 60 100))
     :avg-packet-size (r/rand-uniform 54 80)  ;; SYN paketi su mali
     :std-packet-size (r/rand-uniform 5 20)

     ;; Protocol (TCP dominantan)
     :udp-ratio (r/rand-uniform 0.0 0.05)
     :tcp-ratio (r/rand-uniform 0.92 0.99)
     :icmp-ratio (r/rand-uniform 0.0 0.03)

     ;; TCP flags (KLJUČNO: visok SYN, nizak ACK)
     :tcp-syn-ratio (r/rand-uniform 0.85 0.98)  ;; Skoro sve SYN
     :tcp-ack-ratio (r/rand-uniform 0.02 0.10)  ;; Malo ACK
     :tcp-fin-ratio (r/rand-uniform 0.0 0.03)

     ;; IP diversity (mnogo source IPs, mali broj dest IPs)
     :unique-src-ips unique-src
     :unique-dst-ips (r/rand-uniform 5 100)
     :unique-flows (r/clamp (* unique-src (r/rand-uniform 0.9 1.5)) 3000 20000)
     :src-ip-entropy (r/rand-uniform 5.0 7.0)
     :dst-ip-entropy (r/rand-uniform 1.5 3.0)

     ;; Port patterns (često targetuje specifične portove - 80, 443)
     :dst-port-entropy (r/rand-uniform 0.5 2.0)
     :top-dst-port-share (r/rand-uniform 0.40 0.80)

     ;; Top talkers
     :top-src-ip-packet-share (r/rand-uniform 0.02 0.10)
     :top-src-ip-byte-share (r/rand-uniform 0.02 0.10)

     ;; DNS
     :dns-query-ratio (r/rand-uniform 0.0 0.02)
     :dns-response-ratio (r/rand-uniform 0.0 0.02)

     ;; Subnet spread
     :dst-subnet-spread (r/rand-uniform 0.05 0.30)}))

;; ICMP Flood Attack
(defn icmp-flood []
  (let [packet-rate (r/rand-normal 100000 25000)
        unique-src (r/rand-uniform 2000 12000)]
    {:label "icmp-flood"

     ;; Volumetrija (mali paketi ICMP echo request)
     :packet-rate (r/clamp packet-rate 60000 200000)
     :byte-rate (* packet-rate (r/rand-uniform 70 120))
     :avg-packet-size (r/rand-uniform 64 128)
     :std-packet-size (r/rand-uniform 10 40)

     ;; Protocol (ICMP dominantan)
     :udp-ratio (r/rand-uniform 0.0 0.05)
     :tcp-ratio (r/rand-uniform 0.0 0.05)
     :icmp-ratio (r/rand-uniform 0.90 0.99)

     ;; TCP flags (zanemarljivo jer nema TCP)
     :tcp-syn-ratio (r/rand-uniform 0.0 0.02)
     :tcp-ack-ratio (r/rand-uniform 0.0 0.02)
     :tcp-fin-ratio (r/rand-uniform 0.0 0.01)

     ;; IP diversity
     :unique-src-ips unique-src
     :unique-dst-ips (r/rand-uniform 10 150)
     :unique-flows (r/clamp (* unique-src (r/rand-uniform 0.8 1.3)) 2000 15000)
     :src-ip-entropy (r/rand-uniform 4.5 6.5)
     :dst-ip-entropy (r/rand-uniform 2.0 3.5)

     ;; Port patterns (ICMP nema portove ali možemo koristiti type/code)
     :dst-port-entropy (r/rand-uniform 0.0 0.5)
     :top-dst-port-share (r/rand-uniform 0.80 0.98)

     ;; Top talkers
     :top-src-ip-packet-share (r/rand-uniform 0.02 0.12)
     :top-src-ip-byte-share (r/rand-uniform 0.02 0.12)

     ;; DNS
     :dns-query-ratio 0.0
     :dns-response-ratio 0.0

     ;; Subnet spread
     :dst-subnet-spread (r/rand-uniform 0.10 0.40)}))

;; UDP Flood (Generic/Mixed ports)
(defn udp-flood-mixed []
  (let [packet-rate (r/rand-normal 85000 18000)
        unique-src (r/rand-uniform 1500 8000)]
    {:label "udp-flood-mixed"

     ;; Volumetrija (srednje veličine paketi)
     :packet-rate (r/clamp packet-rate 45000 160000)
     :byte-rate (* packet-rate (r/rand-uniform 200 600))
     :avg-packet-size (r/rand-uniform 150 500)
     :std-packet-size (r/rand-uniform 50 150)

     ;; Protocol (UDP dominantan)
     :udp-ratio (r/rand-uniform 0.88 0.98)
     :tcp-ratio (r/rand-uniform 0.01 0.10)
     :icmp-ratio (r/rand-uniform 0.01 0.05)

     ;; TCP flags
     :tcp-syn-ratio (r/rand-uniform 0.02 0.08)
     :tcp-ack-ratio (r/rand-uniform 0.02 0.08)
     :tcp-fin-ratio (r/rand-uniform 0.0 0.03)

     ;; IP diversity
     :unique-src-ips unique-src
     :unique-dst-ips (r/rand-uniform 10 200)
     :unique-flows (r/clamp (* unique-src (r/rand-uniform 1.0 2.0)) 2000 12000)
     :src-ip-entropy (r/rand-uniform 4.0 6.0)
     :dst-ip-entropy (r/rand-uniform 2.0 4.0)

     ;; Port patterns (KLJUČNO: mixed ports, viša entropija)
     :dst-port-entropy (r/rand-uniform 2.5 4.5)
     :top-dst-port-share (r/rand-uniform 0.15 0.40)

     ;; Top talkers
     :top-src-ip-packet-share (r/rand-uniform 0.03 0.15)
     :top-src-ip-byte-share (r/rand-uniform 0.03 0.15)

     ;; DNS
     :dns-query-ratio (r/rand-uniform 0.0 0.05)
     :dns-response-ratio (r/rand-uniform 0.0 0.05)

     ;; Subnet spread
     :dst-subnet-spread (r/rand-uniform 0.10 0.35)}))

;;NTP Amplification Attack port 123!
(defn ntp-amplification []
  (let [packet-rate (r/rand-normal 55000 12000)
        unique-src (r/rand-uniform 1500 7000)]
    {:label "ntp-amplification"

     ;; Volumetrija (veliki response paketi)
     :packet-rate (r/clamp packet-rate 30000 110000)
     :byte-rate (* packet-rate (r/rand-uniform 400 700))
     :avg-packet-size (r/rand-uniform 400 600)
     :std-packet-size (r/rand-uniform 50 120)

     ;; Protocol (UDP - port 123)
     :udp-ratio (r/rand-uniform 0.90 0.99)
     :tcp-ratio (r/rand-uniform 0.01 0.07)
     :icmp-ratio (r/rand-uniform 0.0 0.03)

     ;; TCP flags
     :tcp-syn-ratio (r/rand-uniform 0.0 0.05)
     :tcp-ack-ratio (r/rand-uniform 0.0 0.05)
     :tcp-fin-ratio (r/rand-uniform 0.0 0.02)

     ;; IP diversity (mnogo reflektora)
     :unique-src-ips unique-src
     :unique-dst-ips (r/rand-uniform 5 80)
     :unique-flows (r/clamp (* unique-src (r/rand-uniform 0.8 1.4)) 1500 9000)
     :src-ip-entropy (r/rand-uniform 4.5 6.5)
     :dst-ip-entropy (r/rand-uniform 1.5 3.0)

     ;; Port patterns (port 123 dominantan)
     :dst-port-entropy (r/rand-uniform 0.3 1.2)
     :top-dst-port-share (r/rand-uniform 0.75 0.95)

     ;; Top talkers
     :top-src-ip-packet-share (r/rand-uniform 0.02 0.10)
     :top-src-ip-byte-share (r/rand-uniform 0.02 0.10)

     ;; DNS (nije DNS attack)
     :dns-query-ratio (r/rand-uniform 0.0 0.02)
     :dns-response-ratio (r/rand-uniform 0.0 0.02)

     ;; Subnet spread
     :dst-subnet-spread (r/rand-uniform 0.08 0.35)}))

;;  ACK Flood Attack
(defn ack-flood []
  (let [packet-rate (r/rand-normal 95000 22000)
        unique-src (r/rand-uniform 4000 18000)]
    {:label "ack-flood"

     ;; Volumetrija
     :packet-rate (r/clamp packet-rate 50000 190000)
     :byte-rate (* packet-rate (r/rand-uniform 50 90))
     :avg-packet-size (r/rand-uniform 50 100)
     :std-packet-size (r/rand-uniform 8 25)

     ;; Protocol (TCP dominantan)
     :udp-ratio (r/rand-uniform 0.0 0.05)
     :tcp-ratio (r/rand-uniform 0.92 0.99)
     :icmp-ratio (r/rand-uniform 0.0 0.03)

     ;; TCP flags (KLJUČNO: visok ACK nizak SYN)
     :tcp-syn-ratio (r/rand-uniform 0.01 0.08)
     :tcp-ack-ratio (r/rand-uniform 0.88 0.98)  ;; Skoro sve ACK
     :tcp-fin-ratio (r/rand-uniform 0.01 0.05)

     ;; IP diversity
     :unique-src-ips unique-src
     :unique-dst-ips (r/rand-uniform 10 150)
     :unique-flows (r/clamp (* unique-src (r/rand-uniform 0.8 1.4)) 4000 22000)
     :src-ip-entropy (r/rand-uniform 5.0 7.0)
     :dst-ip-entropy (r/rand-uniform 2.0 3.5)

     ;; Port patterns
     :dst-port-entropy (r/rand-uniform 0.8 2.5)
     :top-dst-port-share (r/rand-uniform 0.35 0.70)

     ;; Top talkers
     :top-src-ip-packet-share (r/rand-uniform 0.02 0.12)
     :top-src-ip-byte-share (r/rand-uniform 0.02 0.12)

     ;; DNS
     :dns-query-ratio (r/rand-uniform 0.0 0.02)
     :dns-response-ratio (r/rand-uniform 0.0 0.02)

     ;; Subnet spread
     :dst-subnet-spread (r/rand-uniform 0.10 0.40)}))

;; F-ja za generisanje podataka 
;; Izmesaj i ovde podatke
;; (def attack-generators
;;   {:udp-large udp-large-packets
;;    ;;  :udp-empty udp-empty-packets
;;    ;;  :tcp-ack tcp-ack-anomaly
;;    "dns-amplification" dns-amplification
;;    :subnet-carpet subnet-carpet-bombing
;;    :syn-flood syn-flood
;;    "icmp-flood" icmp-flood
;;    "udp-flood-mixed" udp-flood-mixed
;;    "ntp-amplification" ntp-amplification
;;    "ack-flood" ack-flood})


;; UDP paketi bez  payloada
;; Ovo se izbacuje zato sto sFlow ne vidi payload, ostaviti za slucaj Netflowa
;; (defn udp-empty-packets []
;;   {:label :udp-empty
;;    :packet-rate (r/rand-uniform 60000 150000)
;;    :avg-packet-size (r/rand-uniform 28 60)
;;    :std-packet-size (r/rand-uniform 5 15)
;;    :udp-ratio (r/rand-uniform 0.9 1.0)
;;    :tcp-ratio 0.0
;;    :icmp-ratio (r/rand-uniform 0.0 0.05)
;;    :unique-src-ips (r/rand-uniform 200 800)
;;    :unique-flows (r/rand-uniform 300 1200)
;;    :top-ip-packet-share (r/rand-uniform 0.02 0.15)
;;    :top-ip-byte-share (r/rand-uniform 0.01 0.10)
;;    :src-ip-entropy (r/rand-uniform 4.0 5.5)
;;    :dst-port-entropy (r/rand-uniform 0.8 1.5)
;;    :dns-response-ratio 0.0
;;    :subnet-spread (r/rand-uniform 0.1 0.4)})

;; TCP ACK bez ACK number (Acknowledgement broj) Ne moze se tako jednostavno utvrditi iz sflow saobracaja
;; Previse granularno. Ostaviti za slucaj NetFlowa
;; (defn tcp-ack-anomaly []
;;   {:label :tcp-ack-anomaly
;;    :packet-rate (r/rand-uniform 20000 60000)
;;    :avg-packet-size (r/rand-uniform 60 120)
;;    :std-packet-size (r/rand-uniform 10 30)
;;    :udp-ratio 0.0
;;    :tcp-ratio (r/rand-uniform 0.9 1.0)
;;    :icmp-ratio 0.0
;;    :ack-rate (r/rand-uniform 0.7 1.0)
;;    :ack-without-number-rate (r/rand-uniform 0.6 0.95)
;;    :syn-rate (r/rand-uniform 0.0 0.1)
;;    :unique-src-ips (r/rand-uniform 50 300)
;;    :unique-flows (r/rand-uniform 100 600)
;;    :top-ip-packet-share (r/rand-uniform 0.1 0.4)
;;    :src-ip-entropy (r/rand-uniform 2.5 4.0)})