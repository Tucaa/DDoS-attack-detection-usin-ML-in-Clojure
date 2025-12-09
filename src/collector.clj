(ns collector
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:import [io.pkts Pcap]
           [io.pkts.packet Packet]
           [io.pkts.buffer Buffer]
           [io.pkts.protocol Protocol]))


;F-ja koja cita podatke iz pcap fajla (resiti import) za sada samo binarni format dok ne resis problem sa citanjem preko io.pkts
(defn raw_file [file]
  (with-open [in (io/input-stream file)]
    (loop [buffer (byte-array 4096)]
      (let [n (.read in buffer)]
        (when (pos? n)
          (println (String. buffer 0 n "ISO-8859-1"))
          (recur buffer))))))


;Funkcija koja agregira tot_bytes i koja vraca counter i finalnu agregiranu vrednost
(defn aggregate
  [data]
  (reduce
    (fn [acc item]
      (let [bytes (:tot-bytes item)]
        {:tot_bytes (+ (:tot_bytes acc) bytes)
         :count       (inc (:count acc))}))
    ;Početno stanje akumulatora
    {:tot_bytes 0 :count 0}
    data))

;Funkcija za konvertovanje vrednosti na osnovu definisane jedinice ('KB, MB, GB ...)
;Kasnije modifikovati da dinamicki pronalazi da li su ('KB, MB ili GB)
;Mapirati sa ovom f-jom
(defn convert
  [unit]
  (let [base 1024]
    (case unit
      :KB (/ bytes base)
      :MB (/ bytes (* base base)) ; 1024^2
      :GB (/ bytes (* base base base)) ; 1024^3
      :TB (/ bytes (* base base base base)) ; 1024^4
      )))


(def test_data
  [{:src-ip "192.168.1.100" :protocol :tcp :tot-bytes 1024}
   {:src-ip "10.0.0.5"      :protocol :udp :tot-bytes 512}
   {:src-ip "192.168.1.100" :protocol :icmp :tot-bytes 128}
   {:src-ip "10.0.0.1"      :protocol :tcp :tot-bytes 2048}])




(defn -main
  [& args]

  (def results  (aggregate test_data))

  (println (str "Ulazni podaci: " test_data))
  (println (str "Rezultati: " results))

  (def test_pcap (raw_file "D:/Milan/test.pcap"))

  ;(def converted-kb (convert (:total-bytes results) :KB))

  ;(println (str "Konvertovana vrednost (KB): " converted-kb)))
  )
