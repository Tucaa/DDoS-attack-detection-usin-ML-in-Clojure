(ns collector
  (:gen-class)
  (:require [clojure.string :as str])
  ;Ovo proveri kako se importuje nece da radi kako treba
  (:import [clj_net_pcap Pcap]))

;F-ja koja cita podatke iz pcap fajla
(defn read-and-print-pcap-simple
  [pcap-file-path]
  (println (str "--- Fajl path " pcap-file-path " ---"))

  (Pcap/read pcap-file-path
             (fn [packet]
               (let [header (.getPcapHeader packet)]
                 (println (str "Timestamp: " (.getTimestamp header)))
                 (println (str "Original Length: " (.getOrigLength header) " bytes"))
                 (println (str "Captured Length: " (.getCaptureLength header) " bytes"))))))






(defn -main
  [& args]

  ;(read-and-print-pcap-simple "src/test.pcap"))