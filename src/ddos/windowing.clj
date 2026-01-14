(ns ddos.windowing
  (:require [ddos.random :as r]))

;; F-je koje dodaju vremenske serije podacima

(defn add-window-metadata
  [sample window-id timestamp]
  (assoc sample
         :window-id window-id
         :timestamp timestamp))


;; Karakteristike napada u sekundama
;; Napomena -prob parametar je u procentima
(def attack-pattern-duration

  {:udp-flood-large
   {:min-duration 120
    :max-duration 1800
    :typical-duration 600
    :long-attack-prob 0.15
    :long-duration-range [1800 3600]}

   :udp-flood-mixed
   {:min-duration 180
    :max-duration 2400
    :typical-duration 900
    :long-attack-prob 0.2
    :long-duration-range [1800 3600]}

   :dns-amplification
   {:min-duration 300
    :max-duration 1800
    :typical-duration 600
    :long-attack-prob 0.10
    :long-duration-range [1800 3600]}

   :ntp-amplification
   {:min-duration 240
    :max-duration 1500
    :typical-duration 480
    :long-attack-prob 0.08
    :long-duration-range [1500 2700]}

   :ack-flood
   {:min-duration 1200
    :max-duration 5400
    :typical-duration 2700
    :long-attack-prob 0.30
    :long-duration-range [5400 14400]}


   :subnet-carpet-bombing
   {:min-duration 1800
    :max-duration 7200
    :typical-duration 3600
    :long-attack-prob 0.35
    :long-duration-range [7200 21600]}
   
  ;;  Kod obicnog saobracaja se uzima 
   :normal
   {:min-duration 3600
    :max-duration 86400
    :typical-duration 14400
    :long-attack-prob 0.0
    :long-duration-range [0 0]}})




;; Izmenjeno je da se ova funkcija odnosi na normalni saobracaj
(defn generate-windows-normal
  [attack-fn n start-ts window-ms]
  (map-indexed
   (fn [i _]
     (add-window-metadata
      (attack-fn)
      i
      (+ start-ts (* i window-ms))))
   (range n)))