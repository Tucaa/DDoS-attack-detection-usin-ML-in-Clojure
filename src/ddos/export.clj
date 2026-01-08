(ns ddos.export
    (:require [clojure.data.csv :as csv]
             [clojure.java.io :as io]))

(defn write-csv [rows path]
  (let [headers (-> rows first keys)]
    (with-open [w (io/writer path)]
      (csv/write-csv w
                     (cons (map name headers)
                           (map #(map % headers) rows))))))