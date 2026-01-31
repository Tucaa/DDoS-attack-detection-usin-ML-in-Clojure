(ns ddos.simplenn
  (:require [uncomplicate.neanderthal.core :as cor]
            [uncomplicate.neanderthal.native :as ntv] 
            [uncomplicate.neanderthal.random :refer [rand-uniform!]]))

(defn sigmoid! [n]
  (let [vec-size (cor/dim n)]
    (dotimes [i vec-size]
      (let [j (cor/entry n i)]
        (cor/entry! n i (/ 1 (+ 1 (Math/exp (- j))))))))
  n)

(defn create-layer [input-size output-size]
  ;; Moraces da napises testove koji ce proveravati da li unos validan
  {:pre [(pos-int? input-size) (pos-int? output-size)]}
  (let [weights (ntv/fge output-size input-size)
        bias  (ntv/fv output-size)]

    ;; Za pocetak ovako posle dodaj nesto bolje
    (rand-uniform! -1.0 1.0 weights)
    (rand-uniform! -1.0 1.0 bias)

    {:weights weights
     :bias  bias
     :input-size  input-size
     :output-size output-size}))

;; A ovde ces da napises test za komatibilnost sloja i unosa moraju da se gadjaju dimenzije matrica!!!!!!!
(defn network [layer input]
  (let [w (:weights layer)
        b (:bias layer)

        output (cor/copy! b (ntv/fv (:output-size layer)))]
    (cor/mv! w input output))
  )

  