(ns ddos.visualization
  (:require [incanter.core :as i]
            [incanter.charts :as c]
            [incanter.stats :as s]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))


(defn load-csv [filepath]
  (with-open [reader (io/reader filepath)]
    (let [data (csv/read-csv reader)
          headers (map keyword (first data))
          rows (rest data)]
      (i/dataset headers rows))))

(defn save-graph [graph filename]
  (i/save graph filename :width 800)
  )

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




