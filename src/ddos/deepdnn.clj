(ns ddos.deepdnn
  (:require [uncomplicate.diamond.core :as diamond]
            [uncomplicate.diamond.dnn :as dnn]
            [uncomplicate.diamond.tensor :as tensor]
            [uncomplicate.neanderthal.core :as cor]
            [ddos.importexport :refer :all]
            [ddos.simplenn :refer [prepare-data features labels-idx]]))


(def nn-architecture [(dnn/dense [128] :relu)
                      (dnn/dropout 0.3)
                      (dnn/dense [64] :relu)
                      (dnn/dropout 0.3)
                      (dnn/dense [32] :relu)
                      (dnn/dropout 0.2)
                      (dnn/dense [9] :softmax)])

;; Inicijalno stavljeno 128 batchova sa po 25 uzoraka
(def feedforward-nn (dnn/network (dnn/desc [128 25] :float :nc) nn-architecture))

;Iniciajlizacija tezina i definisanje optimizacije
(defonce nn (dnn/init! (feedforward-nn :adam)))


(defn train-nn []
  (let [data (prepare-data "new_ddos_dataset.csv" features labels-idx)

        ;; Mora da se podesi da bi odgovaralo dimenzijama neuralne mreze (prvog sloja)
        modified-data (* (quot (:n-samples data) 128) 128)

        ;; Prebacivanje na engine i isecanje na tacnu dimenziju
        dev (diamond/engine nn)
        x (diamond/transfer dev (cor/submatrix (:features data) 0 0 25 modified-data))
        y (diamond/transfer dev (cor/subvector (:labels data) 0 modified-data))]

    (println "Starting training" data)

    (time (dnn/train! nn x y :crossentropy 10 []))))