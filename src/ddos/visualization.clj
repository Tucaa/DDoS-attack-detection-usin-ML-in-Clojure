(ns ddos.visualization
  (:require [incanter.core :as i]
            [incanter.charts :as c]
            [incanter.stats :as s]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))


(defn parse-number [s]
  (try
    (if (string? s)
      (Double/parseDouble s)
      s)
    (catch Exception _ nil)))

(defn load-csv [filepath]
  (with-open [reader (io/reader filepath)]
    (let [data (csv/read-csv reader)
          headers (map keyword (first data))
          rows (rest data)]
      (i/dataset headers
                 (map (fn [row]
                        (map parse-number row))
                      rows)))))

(defn save-graph [graph filename]
  (i/save graph filename :width 800 :height 600))

(defn graph-class-distribution [dataset]
  (let [labels (i/$ :label dataset)
        freq-map (frequencies labels)
        classes (keys freq-map)
        counts (vals freq-map)]
    (doto (c/bar-chart classes counts
                       :title "Attack class distribution"
                       :x-label "Attack type"
                       :y-label "Count"
                       :vertical true)
      i/view)))


(defn graph-packet-rate-histogram [dataset]
  (doto (c/histogram (i/$ :packet-rate dataset)
                     :title "Packet rate distribution"
                     :x-label "Packet Rate"
                     :y-label "Frequency"
                     :nbins 50)
    i/view))




(defn packet-size|rate-ratio [dataset]
  (let [labels (distinct (i/$ :label dataset))]
    (let [chart (c/scatter-plot
                 (i/$ :packet-rate dataset)
                 (i/$ :avg-packet-size dataset)
                 :title "Packet rate vs avg packet size"
                 :x-label "Packet rate"
                 :y-label "Size"
                 :group-by (i/$ :label dataset)
                 :legend true)]
      (i/view chart))))



(defn graph-protocol-distribution [dataset]
  (let [labels (distinct (i/$ :label dataset))
        data (for [label labels]
               (let [subset (i/$where {:label label} dataset)
                     n (i/nrow subset)]
                 {:class (str label)
                  :UDP (/ (reduce + (i/$ :udp-ratio subset)) n)
                  :TCP (/ (reduce + (i/$ :tcp-ratio subset)) n)
                  :ICMP (/ (reduce + (i/$ :icmp-ratio subset)) n)}))]

    (let [classes (map :class data)
          udp-vals (map :UDP data)
          tcp-vals (map :TCP data)
          icmp-vals (map :ICMP data)

          chart (c/bar-chart classes udp-vals
                             :title "Average protocl ratios by class"
                             :x-label "Attack type"
                             :y-label "Ratio"
                             :series-label "UDP"
                             :legend true)]

      (c/add-categories chart classes tcp-vals :series-label "TCP")
      (c/add-categories chart classes icmp-vals :series-label "ICMP")
      (i/view chart))))


(defn dataset-statistics [dataset]
  (println "________DATASET STATISTSICS__________\n")
  (println "$Total samples:" (i/nrow dataset))
  (println "$Classs distribution: \n")
  (doseq [[label count] (sort-by val > (frequencies (i/$ :label dataset)))]
    (println (format "  %-25s %5d (%.1f%%)"
                     label
                     count
                     (* 100.0 (/ count (i/nrow dataset))))))

  (println "$Feature satistics: \n")
  (println "________________________________________________")
  (doseq [col [:packet-rate :avg-packet-size :udp-ratio :tcp-ratio
               :unique-src-ips :src-ip-entropy]]
    (let [values (i/$ col dataset)
          ;; Mora da se uradi ovaj deo posto baci exception ako ne budu svi brojevi
          clean-values (keep #(try (Double/parseDouble (str %))
                                   (catch Exception _ nil))
                             values)]
      (if (seq clean-values)
        (do
          (println (format "\n%-20s" (name col)))
          (println (format "  Min:     %12.2f" (apply min clean-values)))
          (println (format "  Max:     %12.2f" (apply max clean-values)))
          (println (format "  Mean:    %12.2f" (s/mean clean-values)))
          (println (format "  Median:  %12.2f" (s/median clean-values)))
          (println (format "  StdDev:  %12.2f" (s/sd clean-values))))
        (println (format "\n%-20s [NO NUMERIC DATA]" (name col)))))))

;; Trebaces da dodas grafike za vremensku raspodelu napada


;; (def temp-analysis [filepath] 

;;   (let [data (load-csv filepath)])
;; )

(defn sumary-fn [filepath]
  (let [dataset (load-csv filepath)]
    (dataset-statistics dataset)
    (packet-size|rate-ratio  dataset)
    (graph-packet-rate-histogram dataset)
    (graph-protocol-distribution dataset)
    (graph-class-distribution dataset)
    dataset))

