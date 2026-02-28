(ns ddos.importexport
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn write-csv [rows path]
  (let [headers (-> rows first keys)]
    (with-open [w (io/writer path)]
      (csv/write-csv w
                     (cons (map name headers)
                           (map #(map % headers) rows))))))

(defn parse-value [s]
  (cond
    (nil? s) nil
    (= s "true") true
    (= s "false") false
    :else (try (Double/parseDouble s)
               (catch Exception _ s))))

(defn load-csv [file-path]
  (try
    (with-open [reader (io/reader file-path)]
      (let [rows (doall (csv/read-csv reader))
            header (first rows)
            data (vec (rest rows))]
        (println "Num rows:" (count rows))
        (println "Header:" header)
        {:header header
         :data data}))
    (catch Exception e
      (let [ste (first (.getStackTrace e))]
        (println "Exception load-csv" (.getMessage e) "Line:" (.getLineNumber ste))
        (throw e)))))
;; (defn load-csv [filepath]
;;   (with-open [reader (io/reader filepath)]
;;     ;; Loaduje se celokupni csv u memoriju
;;     (let [data (doall (csv/read-csv reader))
;;           headers (map keyword (first data))
;;           rows (rest data)]
;;       (i/dataset headers
;;                  (map (fn [row] (map parse-value row)) rows)))))

;; (defn save-graph [graph filename]
;;   (i/save graph filename :width 800 :height 600))
