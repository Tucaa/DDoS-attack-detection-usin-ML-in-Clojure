(ns collector
  ;(:gen-class)
  (:import
    (:import (org.pcap4j.core Pcaps))))


;F-ja koja cita podatke iz pcap fajla (resiti import)
(defn read_pcap [file]
  (with-open [handle (org.pcap4j.core.Pcaps/openOffline file)]
    (loop []
      (let [packet (.getNextPacket handle)]
        (when packet
          (println "Packet length:" (.length packet))
          (recur))))))

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

  (def read (read_pcap "promenjena putanja fajla"))

  (println (str "Citanje fajla: " read))

  ;(def converted-kb (convert (:total-bytes results) :KB))

  ;(println (str "Konvertovana vrednost (KB): " converted-kb)))
  )
