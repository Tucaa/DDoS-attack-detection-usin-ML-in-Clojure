(ns ddos.dataprep
  (:require
   [clojure.data.csv   :as csv]
   [clojure.java.io    :as io]
   [uncomplicate.neanderthal.core   :as cor]
   [uncomplicate.neanderthal.native :as ntv]))


(def labels-idx
  {"ack-flood" 0
   "icmp-flood" 1
   "ntp-amplification" 2
   "udp-flood-large" 3
   "udp-flood-mixed" 4
   "dns-amplification" 5
   "normal" 6
   "subnet-carpet-bombing" 7
   "syn-flood" 8})

;; Ove 2 kolone kolone se ne gledaju kod treniranja!
(def skip-columns #{"ts_formated" "label"})


;; U Sustini vraca drugaciji format podatakaa od funkcije za csv iz importexport
;; Vidi da je prebacis tamo 
(defn read-csv [filename]
  (with-open [reader (io/reader filename)]
    (let [raw    (csv/read-csv reader)
          header (first raw)
          rows   (rest raw)]
      (mapv #(zipmap header %) rows))))


(defn single-row [row features]
  (mapv (fn [col]
          (let [val (get row col)]
            (if (= val "true")
              1.0
              (if (= val "false")
                0.0
                (Float/parseFloat val)))))
        features))

(defn lable-getter [row]
  (get labels-idx (get row "label")))

;; Ovo je bitno za generisanje vektora na izlazu (iliti one hot vec)
;; Posto imamo 9 klasa vektor za prvu klasu je [1.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0] (za drugu je na drugom mestu 1 itd)
(defn output-vec [label-idx classes]
  (assoc (vec (repeat classes 0.0)) label-idx 1.0))

(defn prepare-data-dnn [filename]
  (try

    (let [rows (read-csv filename)
          n (count rows)
          header (keys (first rows))
          features (filterv #(not (contains? skip-columns %)) header)
          all-features (mapv #(single-row % features) rows)
          all-labels (mapv lable-getter rows)
          output-vectors (mapv #(output-vec % 9) all-labels)
          x-matrix (ntv/fge n 25)
          y-matrix (ntv/fge n 9)]


      (doseq [[i feature-row] (map-indexed vector all-features)]
        (doseq [[j val] (map-indexed vector feature-row)]
          (cor/entry! x-matrix i j val)))

      (doseq [[i one-hot-row] (map-indexed vector output-vectors)]
        (doseq [[j val] (map-indexed vector one-hot-row)]
          (cor/entry! y-matrix i j val)))

      {:x x-matrix
       :y y-matrix
       :n n})
  (catch Exception e
   (let [ste (first (.getStackTrace e))]
     (println "Exception dataprep | prepare-data-dnn" (.getMessage e) "Line:" (.getLineNumber ste)))
   (throw e))))

  
(def data (prepare-data-dnn "new_ddos_dataset.csv"))

(println "x dimensions" (cor/mrows (:x data)) "x" (cor/ncols (:x data)))
(println "y dimensions" (cor/mrows (:y data)) "x" (cor/ncols (:y data)))
(println "One hot vector" (cor/row (:y data) 1))
