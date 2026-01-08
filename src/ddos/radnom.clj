(ns ddos.radnom)

(defn rand-uniform [min max]
  (+ min (* (rand) (- max min))))

;; Box Mullerova transformacija 
(defn rand-normal
  [mean std]
  (let [u1 (rand)
        u2 (rand)
        z (* (Math/sqrt (* -2 (Math/log u1)))
             (Math/cos (* 2 Math/PI u2)))]
    (+ mean (* std z))))

(defn clamp [x low high]
  (-> x (max low) (min high)))


;; Shanon entropija
(defn calculate-entropy
  [unique-count total-items]
  (if (or (zero? unique-count) (zero? total-items))
    0.0
    (let [p (/ unique-count total-items)]
      (* -1 p (Math/log p)))))