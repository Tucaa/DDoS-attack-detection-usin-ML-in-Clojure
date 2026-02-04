(ns ddos.simplenn
  ;; Kasnije refraktorisi sa :refer all
  (:require [uncomplicate.neanderthal.core :as cor]
            [uncomplicate.neanderthal.native :as ntv]
            [uncomplicate.neanderthal.random :refer [rand-uniform!]]
            [uncomplicate.neanderthal.vect-math :as v]
            [uncomplicate.neanderthal.math :as m]))


;; Kasnije optimizovati
(defn sigmoid! [n]
  (let [vec-size (cor/dim n)]
    (dotimes [i vec-size]
      (let [j (cor/entry n i)]
        (cor/entry! n i (/ 1 (+ 1 (Math/exp (- j))))))))
  n)


(defn sigmoid-der! [n]
  (let [vec-size (cor/dim n)]
    (dotimes [i vec-size]
      (let [j (cor/entry n i)]
        (cor/entry! n i (* j (- 1 j))))))
  n)

(defn create-layer [input-size output-size activation-fn]
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
     :output-size output-size
     ;;  Elegantnije uradi
     :activation activation-fn}))

(defn forward [layer input]
  ;; Uradi test za validaciju
  ;;  Za linearnu funkciju formula x = weight * input + bias | Nelinearna je fja aktivacije na linearni deo = y activation(x)
  (let [{:keys [weights bias activation]} layer
        x (cor/axpy! 1.0 bias (cor/mv weights input))
        y (activation x)]
    {:x x :y y}))

(defn nn-forward [input network]
  ;; Trebaces da dodas test za kompatibilnost slojeva
  (let [initial {:act [] :curr input}
        final
        (reduce
         (fn [state layer]
           (let [curr (:curr state)
                 layer-res (forward layer curr)
                 new-act (conj (:act state) layer-res)]
             {:act new-act
              :curr (:y layer-res)}))
         initial
         network)]
    final))

;; x 
;; (defn cross-entropy [x y])


;; Normalizacija vektora u raspodelu verovatnoce
(defn softmax! [v]
  (let [m (cor/amax v)
        n (cor/dim v)]
    ;; v = v - m
    (dotimes [i n]
      (let [x (cor/entry v i)]
        (cor/entry! v i (- x m))))
    (v/exp! v) 
    (let [s (cor/sum v)]
      (cor/scal! (/ 1.0 s) v))

    v))

;; (defn backpropagation [input output network]
;;   (let))


;; A ovde ces da napises test za komatibilnost sloja i unosa moraju da se gadjaju dimenzije matrica!!!!!!!
;; (defn network [layer input]
;;   (let [w (:weights layer)
;;         b (:bias layer)

;;         output (cor/copy! b (ntv/fv (:output-size layer)))]
;;     (cor/mv! w input output)))

(def nn [(create-layer 4 8 sigmoid!)
         (create-layer 8 6 sigmoid!)
         (create-layer 6 2 sigmoid!)])

(def input (ntv/fv [1.1 2.2 3.3 4.4]))



(def result (nn-forward input nn))


(def test-data
  (ntv/dge 3 4 [1.0 2.0 3.0 4.0
                0.1 0.2 0.3 0.4
                -1.0 -2.0 -3.0 -4.0]))

(defn test-softmax [data]
  (doseq [j (range (cor/mrows data))]
    (softmax! (cor/row data j)))
  data)

