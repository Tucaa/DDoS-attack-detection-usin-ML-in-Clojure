(ns ddos.visualization
  (:require [incanter.core :as i]
            [incanter.charts :as c]
            ;; [incanter.io :as inc-io]
            [incanter.stats :as s]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))


(defn parse-value [s]
  (cond
    (nil? s) nil
    (= s "true") true
    (= s "false") false
    :else (try (Double/parseDouble s)
               (catch Exception _ s))))

(defn load-csv [filepath]
  (with-open [reader (io/reader filepath)]
    ;; Loaduje se celokupni csv u memoriju
    (let [data (doall (csv/read-csv reader))
          headers (map keyword (first data))
          rows (rest data)]
      (i/dataset headers
                 (map (fn [row] (map parse-value row)) rows)))))

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


(defn packet-rate-boxplot [dataset]
  (let [labels (distinct (i/$ :label dataset))]
    (doto (c/box-plot (i/$ :packet-rate (i/$where {:label (first labels)} dataset))
                      :title "Packet rate distribution"
                      :y-label "Packet rate"
                      :series-label (str (first labels))
                      :legend true)
      (#(do
          (doseq [label (rest labels)]
            (c/add-box-plot % (i/$ :packet-rate (i/$where {:label label} dataset))
                            :series-label (str label)))
          %))
      i/view)))




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
        ;; Vidi da ovaj deo malo elegantinje sredis (ili da napises test za funkcije za grafike)
        clean-labels (remove nil? labels)
        data (for [label clean-labels]
               (let [subset (i/$where {:label label} dataset)]
                 {:class (str label)
                  :UDP (or (s/mean (i/$ :udp-ratio subset)) 0)
                  :TCP (or (s/mean (i/$ :tcp-ratio subset)) 0)
                  :ICMP (or (s/mean (i/$ :icmp-ratio subset)) 0)}))]

    (let [classes (map :class data)
          udp-vals (map :UDP data)
          tcp-vals (map :TCP data)
          icmp-vals (map :ICMP data)

          chart (c/bar-chart classes udp-vals
                             :title "Average protocol ratios by clas"
                             :x-label "Attack type"
                             :y-label "Ratio"
                             :series-label "UDP"
                             :legend true)]

      (c/add-categories chart tcp-vals classes :series-label "TCP")
      (c/add-categories chart icmp-vals classes :series-label "ICMP")

      (i/view chart))))


;; Ista logika kao prethodni vidi da namestis univerzalnu f-ju
(defn graph-tcp-flags [dataset]
  (let [labels (distinct (i/$ :label dataset))
        ;; Vidi da ovaj deo malo elegantinje sredis (ili da napises test za funkcije za grafike)
        clean-labels (remove nil? labels)
        data (for [label clean-labels]
               (let [subset (i/$where {:label label} dataset)]
                 {:class (str label)
                  :SYN (or (s/mean (i/$ :tcp-syn-ratio subset)) 0)
                  :ACK (or (s/mean (i/$ :tcp-ack-ratio subset)) 0)
                  :FIN (or (s/mean (i/$ :tcp-fin-ratio subset)) 0)}))]

    (let [classes (map :class data)
          syn-vals (map :SYN data)
          ack-vals (map :ACK data)
          fin-vals (map :FIN data)

          chart (c/bar-chart classes syn-vals
                             :title "Average protocol ratios by tcp flags"
                             :x-label "Attack type"
                             :y-label "Ratio"
                             :series-label "UDP"
                             :legend true)]

      (c/add-categories chart ack-vals classes :series-label "ACK")
      (c/add-categories chart fin-vals classes :series-label "FIN")

      (i/view chart))))



(defn graph-entropy|unique-ips [dataset]
  (doto (c/scatter-plot
         (i/$ :unique-src-ips dataset)
         (i/$ :src-ip-entropy dataset)
         :title "Source IP entropy vs unique source ips"
         :x-label "Unique source IP"
         :y-label "IP entropy"
         :group-by (i/$ :label dataset)
         :legend true)
    i/view))



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

(defn sumary-fn [filepath]
  (let [dataset (load-csv filepath)]
    (dataset-statistics dataset)
    (packet-size|rate-ratio  dataset)
    (graph-packet-rate-histogram dataset)
    (graph-protocol-distribution dataset)
    (graph-tcp-flags dataset)
    (graph-entropy|unique-ips dataset)
    ;; (graph-class-distribution dataset)
    (packet-rate-boxplot dataset)
    dataset))

