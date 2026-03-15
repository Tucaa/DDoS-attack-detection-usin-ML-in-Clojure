(ns ddos.deepdnn
  (:require
   [uncomplicate.diamond.dnn :as dnn]
   ;; cudnn runuje na gpu
   [uncomplicate.diamond.internal.cudnn.factory :refer [cudnn-factory]]
   ;;dnnl factory je za dense runuje na cpu
   [uncomplicate.diamond.internal.dnnl.factory :refer [dnnl-factory]]
   [uncomplicate.diamond.tensor :as tensor]
   [uncomplicate.neanderthal.core :as cor]
   [uncomplicate.neanderthal.native :as ntv]

   [ddos.dataprep :as dp]))

(def nn-architecture [(dnn/dense [128] :relu)
                      (dnn/dropout 0.3)
                      (dnn/dense [64] :relu)
                      (dnn/dropout 0.3)
                      (dnn/dense [32] :relu)
                      (dnn/dropout 0.2)
                      (dnn/dense [9] :softmax)])

;; Nvidia cuda
(defonce gpu (cudnn-factory))
;; Na cpu
(defonce cpu (dnnl-factory))

;; Inicijalno stavljeno 128 batchova sa po 25 uzoraka
(def feedforward-nn (dnn/network cpu (tensor/desc [128 25] :float :nc) nn-architecture))

;Iniciajlizacija tezina i definisanje optimizacije
(defonce nn (dnn/init! (feedforward-nn :adam)))


;; Morao je tenzor y da se knovertuje u matricu u [n 9]
(defn output-matrix [labels classes]
  (let [n (cor/dim labels)
        matrix (ntv/fge n classes)]
    (dotimes [i n]
      (let [class-idx (int (cor/entry labels i))]
        (cor/entry! matrix i class-idx 1.0)))
    matrix))

(defn train-nn []
  (try
    ;; 
    (let [data (dp/prepare-data-dnn "new_ddos_dataset.csv")
          n (* (quot (:n data) 128) 128)


          x-sub (cor/submatrix (:x data) 0 0 n 25)
          y-sub (cor/submatrix (:y data) 0 0 n 9)

          ;; Kreiraj prazne tenzore pa prebaci podatke
          x     (tensor/tensor cpu [n 25] :float :nc)
          y     (tensor/tensor cpu [n 9]  :float :nc)
          _     (cor/transfer! x-sub x)
          _     (cor/transfer! y-sub y)]

      (println "Total samples:" (:n data))
      (println "Training samples:" n)
      (println "x tensor:" (tensor/desc x))
      (println "y tensor:" (tensor/desc y))
      (println "_________Starting training_______")

      ;; Staro
      ;;       modified-data (* (quot (:n-samples data) 128) 128)
      ;;       x-initial (cor/submatrix (:features data) 0 0 modified-data 25)
      ;;       labels-sub (cor/subvector (:labels data) 0 modified-data)
      ;;       y-initial (output-matrix labels-sub 9)
      ;;       x (tensor/tensor cpu (tensor/desc [modified-data 25] :float :nc) x-initial)
      ;;       y (tensor/tensor cpu (tensor/desc [modified-data 9]  :float :nc) y-initial)


      ;;x (tensor/tensor cpu (cor/submatrix (:features data) 0 0 modified-data 25)) 
      ;;y (tensor/tensor cpu (cor/subvector (:labels data) 0 modified-data))

      ;; (println "Loaded data:" (:data data))
      ;; (println "Modified data:" modified-data)
      ;; (println "Feature matrix dimensions:" (cor/mrows (:features data)) "x" (cor/ncols (:features data)))
      ;; (println "Label type:" (type (:labels data)))
      ;; (println "Labels matrix length:" (cor/dim (:labels data)))
      ;; (println "x tensor:" (tensor/desc x))
      ;; (println "y tensor:" (tensor/desc y))


      (time (dnn/train! nn x y :crossentropy 12 []))


      ;; (time
      ;;  (dnn/train! nn x y :crossentropy 10
      ;;              (fn [epoch loss]
      ;;                (println
      ;;                 (format "Epoch %d | Loss: %.6f" epoch (float loss))))))
                      )
;; 
    (catch Exception e
      (let [ste (first (.getStackTrace e))]
        (println "Exception deepdnn | train-nn" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))

(train-nn)
