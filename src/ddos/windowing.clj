(ns ddos.windowing)

;; F-je koje dodaju vremenske serije podacima

(defn add-window-metadata
  [sample window-id timestamp]
  (assoc sample
         :window-id window-id
         :timestamp timestamp))

(defn generate-windows
  [attack-fn n start-ts window-ms]
  (map-indexed
   (fn [i _]
     (add-window-metadata
      (attack-fn)
      i
      (+ start-ts (* i window-ms))))
   (range n)))