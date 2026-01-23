(ns ddos.windowing
  (:require [ddos.radnom :as r]
            [incanter.core :as i]))

;; F-je koje dodaju vremenske serije podacima

(defn add-window-metadata
  [sample window-id timestamp active_atk]
  (assoc sample
         :window-id window-id
         :timestamp timestamp
         :attack-active active_atk))


;; Karakteristike napada u sekundama
;; Napomena -prob parametar je u procentima
(def attack-pattern-duration

  {:udp-large-packets
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

   :icmp-flood
   {:min-duration 180
    :max-duration 1200
    :typical-duration 420
    :long-attack-prob 0.12
    :long-duration-range [1200 2400]}

   :subnet-carpet-bombing
   {:min-duration 1800
    :max-duration 7200
    :typical-duration 3600
    :long-attack-prob 0.35
    :long-duration-range [7200 21600]}


   :syn-flood
   {:min-duration 600
    :max-duration 3600
    :typical-duration 1800
    :long-attack-prob 0.25
    :long-duration-range [3600 7200]}

   ;;  Kod obicnog saobracaja se uzima 
   :normal
   {:min-duration 3600
    :max-duration 86400
    :typical-duration 14400
    :long-attack-prob 0.0
    :long-duration-range [0 0]}})



(defn define-duration
  [attack-type]
  (println attack-type)
  (let [profile (get attack-pattern-duration attack-type)]
    (when-not profile
      (throw (ex-info "Unknown parameter" {:received attack-type :keys attack-pattern-duration})))

    (let [{:keys [min-duration max-duration typical-duration long-attack-prob long-duration-range]} profile]
      (if (< (rand) long-attack-prob)
        (let [[long-min long-max] long-duration-range]
          (int (r/rand-uniform long-min long-max)))
        (let [dev (/ (- max-duration min-duration) 6.0)
              duration (r/rand-normal typical-duration dev)]
          (int (r/clamp duration min-duration max-duration)))))))


;; U buducnosti dodati talase napada posto moze i to da se desi 

(defn waves [attack-type]
  (case attack-type
    :dns-amplification (< (rand) 0.3)
    :ntp-amplification (< (rand) 0.3)
    :udp-flood-mixed (< (rand) 0.3)
    :icmp-flood (< (rand) 0.3)
    :udp-large-packets (< (rand) 0.3)
    false))


(defn wave-pattern [total-dur wave-num]
  (let [wave-dur (/ total-dur wave-num)
        ;; Inicijalno razlika imedju talasa  30% ako bude trebalo zameniti
        quiet-per (* wave-dur 0.3)]

    (for [i (range wave-num)]
      {:start-offset (int (* i wave-dur)) :dur (int (* wave-dur 0.7)) :wave-num (inc i)})))



(defn generate-attack-windows [attack-fn attack-type start-timestap windows-ms]

  (let [dur-sec (define-duration attack-type)
        windows-num (max 1 (int (/ (* dur-sec 1000) windows-ms)))
        ;; Deo za talase napada
        muliple-waves (waves attack-type)
        atk_wave (if muliple-waves
                   (wave-pattern dur-sec (r/rand-uniform 2 4))
                   [{:start-offset 0 :dur dur-sec :wave-num 1}])]
    (vec
     (for [i (range windows-num)]
       (let [time-sec (/ (* i windows-ms) 1000)
             timestamp (+ start-timestap (* i windows-ms))

             active-wave (some (fn [{:keys [start-offset dur]}]
                                 (and (>= time-sec start-timestap)
                                      (< time-sec (+ start-offset dur))))
                               atk_wave)]

         (add-window-metadata
          (attack-fn)
          i
          timestamp
          active-wave))))))

;; Izmenjeno je da se ova funkcija odnosi na normalni saobracaj
(defn generate-windows-normal
  [attack-fn n start-timestamp window-ms]
  (map-indexed
   (fn [i _]
     (add-window-metadata
      (attack-fn)
      i
      (+ start-timestamp (* i window-ms))
      ;; Za normalni saobracaj se uzima da je uvek aktivan
      true))

   (range n)))


;; Funkcija za generisanje vektora sa svim opsezima instanci napada
(defn generate-attack-vector
  [attack-fn attack-type instances duration-hours start-timestap window-ms]
  ;; (println "ARGUMENTI F-JE" attack-fn attack-type, instances, duration-hours, start-timestap, window-ms)
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
