(ns ddos.normal
  (:require [ddos.radnom :as r]))

;;  Normal Web Traffic (HTTP/HTTPS)
(defn normal-web-traffic []
  {:label :normal
   
   ;; Volumetrija (normalan rate)
   :packet-rate (r/rand-uniform 2000 12000)
   :byte-rate (* (r/rand-uniform 2000 12000) (r/rand-uniform 400 1200))
   :avg-packet-size (r/rand-uniform 400 1200)
   :std-packet-size (r/rand-uniform 200 500)
   
   ;; Protocol (TCP dominantan za web)
   :udp-ratio (r/rand-uniform 0.05 0.20)
   :tcp-ratio (r/rand-uniform 0.75 0.92)
   :icmp-ratio (r/rand-uniform 0.01 0.05)
   
   ;; TCP flags (normalan handshake pattern)
   :tcp-syn-ratio (r/rand-uniform 0.08 0.15)
   :tcp-ack-ratio (r/rand-uniform 0.70 0.85)
   :tcp-fin-ratio (r/rand-uniform 0.05 0.12)
   
   ;; IP diversity 
   :unique-src-ips (r/rand-uniform 100 800)
   :unique-dst-ips (r/rand-uniform 80 600)
   :unique-flows (r/rand-uniform 200 1200)
   :src-ip-entropy (r/rand-uniform 3.0 5.0)
   :dst-ip-entropy (r/rand-uniform 2.5 4.5)
   
   ;; Port patterns (HTTP/HTTPS - port 80, 443)
   :dst-port-entropy (r/rand-uniform 1.5 3.0)
   :top-dst-port-share (r/rand-uniform 0.40 0.70)
   
   ;; Top talkers (distribuirano)
   :top-src-ip-packet-share (r/rand-uniform 0.05 0.20)
   :top-src-ip-byte-share (r/rand-uniform 0.05 0.25)
   
   ;; DNS (normalna rezolucija)
   :dns-query-ratio (r/rand-uniform 0.02 0.08)
   :dns-response-ratio (r/rand-uniform 0.02 0.08)
   
   ;; Subnet spread
   :dst-subnet-spread (r/rand-uniform 0.15 0.45)})

;;  Normal Business/Enterprise Traffic
(defn normal-enterprise-traffic []
  {:label :normal
   
   ;; Volumetrija
   :packet-rate (r/rand-uniform 3000 15000)
   :byte-rate (* (r/rand-uniform 3000 15000) (r/rand-uniform 300 900))
   :avg-packet-size (r/rand-uniform 300 900)
   :std-packet-size (r/rand-uniform 150 400)
   
   ;; Protocol (mixed TCP/UDP za različite servise)
   :udp-ratio (r/rand-uniform 0.15 0.35)
   :tcp-ratio (r/rand-uniform 0.60 0.80)
   :icmp-ratio (r/rand-uniform 0.02 0.08)
   
   ;; TCP flags
   :tcp-syn-ratio (r/rand-uniform 0.10 0.18)
   :tcp-ack-ratio (r/rand-uniform 0.65 0.80)
   :tcp-fin-ratio (r/rand-uniform 0.08 0.15)
   
   ;; IP diversity
   :unique-src-ips (r/rand-uniform 200 1200)
   :unique-dst-ips (r/rand-uniform 150 900)
   :unique-flows (r/rand-uniform 400 2000)
   :src-ip-entropy (r/rand-uniform 3.5 5.5)
   :dst-ip-entropy (r/rand-uniform 3.0 5.0)
   
   ;; Port patterns (raznovrsni business portovi)
   :dst-port-entropy (r/rand-uniform 2.5 4.5)
   :top-dst-port-share (r/rand-uniform 0.20 0.50)
   
   ;; Top talkers
   :top-src-ip-packet-share (r/rand-uniform 0.08 0.25)
   :top-src-ip-byte-share (r/rand-uniform 0.10 0.30)
   
   ;; DNS
   :dns-query-ratio (r/rand-uniform 0.03 0.10)
   :dns-response-ratio (r/rand-uniform 0.03 0.10)
   
   ;; Subnet spread
   :dst-subnet-spread (r/rand-uniform 0.20 0.55)})

;;  Normal Streaming/Media Traffic
(defn normal-streaming-traffic []
  {:label :normal
   
   ;; Volumetrija (viši throughput, stabilniji)
   :packet-rate (r/rand-uniform 5000 20000)
   :byte-rate (* (r/rand-uniform 5000 20000) (r/rand-uniform 800 1400))
   :avg-packet-size (r/rand-uniform 800 1400)
   :std-packet-size (r/rand-uniform 100 300)
   
   ;; Protocol (UDP za streaming, TCP za kontrolu)
   :udp-ratio (r/rand-uniform 0.60 0.85)
   :tcp-ratio (r/rand-uniform 0.12 0.35)
   :icmp-ratio (r/rand-uniform 0.01 0.05)
   
   ;; TCP flags
   :tcp-syn-ratio (r/rand-uniform 0.05 0.12)
   :tcp-ack-ratio (r/rand-uniform 0.70 0.88)
   :tcp-fin-ratio (r/rand-uniform 0.03 0.10)
   
   ;; IP diversity (limitiran broj streaming servera)
   :unique-src-ips (r/rand-uniform 80 500)
   :unique-dst-ips (r/rand-uniform 20 150)
   :unique-flows (r/rand-uniform 150 800)
   :src-ip-entropy (r/rand-uniform 2.5 4.5)
   :dst-ip-entropy (r/rand-uniform 1.5 3.5)
   
   ;; Port patterns
   :dst-port-entropy (r/rand-uniform 1.0 2.5)
   :top-dst-port-share (r/rand-uniform 0.50 0.80)
   
   ;; Top talkers
   :top-src-ip-packet-share (r/rand-uniform 0.10 0.35)
   :top-src-ip-byte-share (r/rand-uniform 0.15 0.40)
   
   ;; DNS
   :dns-query-ratio (r/rand-uniform 0.01 0.05)
   :dns-response-ratio (r/rand-uniform 0.01 0.05)
   
   ;; Subnet spread
   :dst-subnet-spread (r/rand-uniform 0.10 0.35)})

;; Normal DNS Traffic
(defn normal-dns-traffic []
  {:label :normal
   
   ;; Volumetrija (nizak rate, mali paketi)
   :packet-rate (r/rand-uniform 1000 6000)
   :byte-rate (* (r/rand-uniform 1000 6000) (r/rand-uniform 80 200))
   :avg-packet-size (r/rand-uniform 80 200)
   :std-packet-size (r/rand-uniform 30 80)
   
   ;; Protocol (UDP dominantan za DNS)
   :udp-ratio (r/rand-uniform 0.85 0.95)
   :tcp-ratio (r/rand-uniform 0.03 0.12)
   :icmp-ratio (r/rand-uniform 0.01 0.05)
   
   ;; TCP flags
   :tcp-syn-ratio (r/rand-uniform 0.05 0.15)
   :tcp-ack-ratio (r/rand-uniform 0.60 0.80)
   :tcp-fin-ratio (r/rand-uniform 0.05 0.15)
   
   ;; IP diversity
   :unique-src-ips (r/rand-uniform 150 1000)
   :unique-dst-ips (r/rand-uniform 5 30)
   :unique-flows (r/rand-uniform 200 1500)
   :src-ip-entropy (r/rand-uniform 3.0 5.5)
   :dst-ip-entropy (r/rand-uniform 1.0 2.5)
   
   ;; Port patterns (port 53)
   :dst-port-entropy (r/rand-uniform 0.3 1.0)
   :top-dst-port-share (r/rand-uniform 0.85 0.98)
   
   ;; Top talkers
   :top-src-ip-packet-share (r/rand-uniform 0.03 0.15)
   :top-src-ip-byte-share (r/rand-uniform 0.03 0.15)
   
   ;; DNS (BALANCED query i response)
   :dns-query-ratio (r/rand-uniform 0.45 0.55)
   :dns-response-ratio (r/rand-uniform 0.45 0.55)
   
   ;; Subnet spread
   :dst-subnet-spread (r/rand-uniform 0.05 0.20)})

;; Normal Email/SMTP Traffic
(defn normal-email-traffic []
  {:label :normal
   
   ;; Volumetrija (nizak rate)
   :packet-rate (r/rand-uniform 800 5000)
   :byte-rate (* (r/rand-uniform 800 5000) (r/rand-uniform 300 800))
   :avg-packet-size (r/rand-uniform 300 800)
   :std-packet-size (r/rand-uniform 150 400)
   
   ;; Protocol (TCP dominantan)
   :udp-ratio (r/rand-uniform 0.05 0.15)
   :tcp-ratio (r/rand-uniform 0.80 0.92)
   :icmp-ratio (r/rand-uniform 0.01 0.05)
   
   ;; TCP flags
   :tcp-syn-ratio (r/rand-uniform 0.08 0.15)
   :tcp-ack-ratio (r/rand-uniform 0.72 0.87)
   :tcp-fin-ratio (r/rand-uniform 0.06 0.13)
   
   ;; IP diversity
   :unique-src-ips (r/rand-uniform 100 700)
   :unique-dst-ips (r/rand-uniform 50 400)
   :unique-flows (r/rand-uniform 150 900)
   :src-ip-entropy (r/rand-uniform 2.8 4.8)
   :dst-ip-entropy (r/rand-uniform 2.3 4.3)
   
   ;; Port patterns (SMTP ports: 25, 465, 587)
   :dst-port-entropy (r/rand-uniform 1.0 2.5)
   :top-dst-port-share (r/rand-uniform 0.50 0.80)
   
   ;; Top talkers
   :top-src-ip-packet-share (r/rand-uniform 0.08 0.25)
   :top-src-ip-byte-share (r/rand-uniform 0.10 0.30)
   
   ;; DNS
   :dns-query-ratio (r/rand-uniform 0.03 0.10)
   :dns-response-ratio (r/rand-uniform 0.03 0.10)
   
   ;; Subnet spread
   :dst-subnet-spread (r/rand-uniform 0.15 0.45)})

;; Izmesan Normal Traffic
(defn normal-mixed-traffic []
  (let [traffic-type (rand)]
    (cond
      (< traffic-type 0.35) (normal-web-traffic)
      (< traffic-type 0.60) (normal-enterprise-traffic)
      (< traffic-type 0.80) (normal-streaming-traffic)
      (< traffic-type 0.92) (normal-dns-traffic)
      :else (normal-email-traffic))))
