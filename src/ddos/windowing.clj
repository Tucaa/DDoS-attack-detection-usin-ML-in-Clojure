(ns ddos.windowing
  (:require [ddos.radnom :as r]
            [incanter.core :as i]))

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



(defn define-duration

  [attack-type]
  (let [profile (get attack-pattern-duration attack-type)
        {:keys [min-duration max-duration typical-duration long-attack-prob long-duration-range]} profile]

    (if (< (rand) long-attack-prob)
      ;; Dugacak napad
      (let [[long-min long-max] long-duration-range]
        (int (r/rand-uniform long-min long-max)))

      ;; Normalni saobracaj
      (let [dev (/ (- max-duration min-duration) 6.0)
            duration (r/rand-normal typical-duration dev)]
        (int (r/clamp duration min-duration max-duration))))))


;; U buducnosti dodati talase napada posto moze i to da se desi 


(defn generate-attack-windows [attack-fn attack-type start-timestap windows-ms]

  (let [dur-sec (define-duration attack-type)
        windows-num (max 1 (int (/ (* dur-sec 1000) windows-ms)))]
    (vec
     (for [i (range windows-num)]
       (let [;;  time-sec (/ (* i windows-ms) 1000)
             timestamp (+ start-timestap (* i windows-ms))]

         (add-window-metadata
          (attack-fn)
          i
          timestamp))))))

;; Izmenjeno je da se ova funkcija odnosi na normalni saobracaj
(defn generate-windows-normal
  [attack-fn n start-timestamp window-ms]
  (map-indexed
   (fn [i _]
     (add-window-metadata
      (attack-fn)
      i
      (+ start-timestamp (* i window-ms))))
   (range n)))


;; Funkcija za generisanje vektora sa svim opsezima instanci napada
(defn generate-attack-vector

  [attack-fn attack-type instances duration-hours start-timestap window-ms]
  (let [duration-ms (* duration-hours 3600 1000)
        spacing-ms (/ duration-ms instances)]
    (vec
     (mapcat
      (fn [ind-instance]
        (let [instance-start (+ start-timestap (* ind-instance spacing-ms))
              windows (generate-attack-windows attack-fn attack-type instance-start window-ms)]

          (map #(assoc %
                       :instance-id ind-instance
                       :vector-id (str attack-type "-vector"))
               windows)))
      (range instances)))))


;; Funkcija za generisanja timeline odnosno organizovanog dataseta kod kojeg se napadi ne preklapaju
(defn generate-timeline
  [window-ms & attack-specs]
  (let [start-ts (System/currentTimeMillis)]
    (loop [curr-time start-ts
           remaining-specs attack-specs
           all-windows []]

      (if (empty? remaining-specs)
        all-windows
        (let [[spec-type spec-data] (first remaining-specs)
              new-window (case spec-type
                           :attack
                           (let [{:keys [attack-fn attack-type]} spec-data]
                             (generate-attack-windows attack-fn attack-type curr-time window-ms))

                           :vector
                           (let [{:keys [attack-fn attack-type instances duration-hours]} spec-data]
                             (generate-attack-vector attack-fn attack-type instances duration-hours curr-time window-ms))

                           :normal
                           (let [{:keys [normal-fn num-windows]} spec-data]
                             (generate-windows-normal normal-fn num-windows curr-time window-ms)))

              last-ts (if (empty? new-window)
                        curr-time
                        (:timestamp (last new-window)))

              quiet-period-ms (* (r/rand-uniform 5 15) 60 1000) ;; Za pocetak je stavljeno da miran period bude od 5 do 15 min kasnije promentiti
              next (+ last-ts window-ms quiet-period-ms)]

          (recur next
                 (rest remaining-specs)
                 (concat all-windows new-window)))))))
