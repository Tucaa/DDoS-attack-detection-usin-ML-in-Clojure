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


;; σ'(x) = σ(x) * (1 - σ(x))
(defn sigmoid-der! [n]
  (let [vec-size (cor/dim n)]
    (dotimes [i vec-size]
      (let [j (cor/entry n i)]
        (cor/entry! n i (* j (- 1 j))))))
  ;; (cor/entry! n i (* j (- 1 j))))))
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
        linear (cor/mv weights input)
        x (cor/axpy! 1.0 bias linear)
        ;; x (cor/axpy! 1.0 bias (cor/mv weights input))
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



;;  Kros entropija za jedan vektor
(defn cross-entropy [vec class-idx]
  (let [max-val (cor/amax vec)
        shifted (doto (cor/copy vec)
                  (#(dotimes [i (cor/dim %)]
                      (cor/entry! % i (- (cor/entry % i) max-val)))))
        ;; shifted (cor/axpy! -1 max-val (cor/copy vec))
        sum-exp (+ max-val (m/log (cor/sum (v/exp! shifted))))
        final-vec (cor/entry vec class-idx)]
    (- sum-exp final-vec)))


;;  Kros entropija za ceo batch
(defn cross-entropy-batch [vec all-class-idx]
  (let [batch-size (cor/mrows vec)
        losses (for [i (range batch-size)]
                 (cross-entropy (cor/row vec i)
                                (cor/entry all-class-idx i)))]
    (/ (reduce + 0.0 losses) batch-size)))


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


;; Racunanje inicijalnog gradijenta (izlaznog sloja: softmax + cross-entropy)
(defn init-gradient [output class-idx]
  (let [grad (cor/copy output)]
    (cor/entry! grad class-idx
                (- (cor/entry grad class-idx) 1.0))

    grad))


;; Fja koja izvlaci input odredjenog sloja (unazad!)
(defn reverse-layer-iput [act input layer-idx tot-layers]
  ;; Dodaj test za proveru argumenata kasnije !
  (if (= layer-idx (dec tot-layers))
    input
    (:y (nth (reverse (:act act)) (inc layer-idx)))))


;; Fja za racunanje gradijenta sloja
(defn layer-gradients [grad curr-ac layer-input]
  (let [act-der (sigmoid-der! (cor/copy (:y curr-ac)))
        ;; (let [act-der (sigmoid-der! (cor/copy (:y curr-ac)))
        grad-x (v/mul! grad act-der)
        ;; grad-x (cor/emul! grad act-der)
        ;; grad-x (cor/mul! grad act-der)
        ;; Spoljni proizvod!
        n (cor/dim grad-x)
        m (cor/dim layer-input)
        grad-weight (ntv/dge n m)]
    (doseq [i (range n)
            j (range m)]

      (cor/entry! grad-weight i j (* (cor/entry grad-x i)
                                     (cor/entry layer-input j))))
    ;; grad-x-col (ntv/dge n 1)
    ;; _ (cor/copy! grad-x-col grad-x)
    ;; layer-input-row (ntv/dge 1 m)
    ;; _ (cor/copy! layer-input-row layer-input)
    ;; grad-weight (cor/mm grad-x-col layer-input-row)
    ;; grad-weight (cor/mm
    ;;              (ntv/resh grad-x [(cor/dim grad-x) 1])
    ;;              (ntv/reshape! layer-input [1 (cor/dim layer-input)]))
    ;; grad-weight (cor/mm
    ;;              (cor/ge (cor/dim grad-x) 1 grad-x)
    ;;              (cor/ge 1 (cor/dim layer-input) layer-input))
    ;; grad-bias (cor/copy grad-x)]

    {:grad-x grad-x :grad-weight grad-weight :grad-bias (cor/copy grad-x)}))

;; Updejtovanje parametra layera
(defn update-layer! [layer grads learning-rate]
  (cor/axpy! (- learning-rate) (:grad-weight grads) (:weights layer))
  (cor/axpy! (- learning-rate) (:grad-bias grads) (:bias layer))
  layer)


;; Propagacija gradijanta unazad
(defn propagate-backwards [layer grad-x]
  (cor/mv (cor/trans (:weights layer)) grad-x))

;; Funkcija za backpropagaciju jednog sloja
(defn backpropagation-singular [layer act layer-input grad learing-rate]
  (let [grads (layer-gradients grad act layer-input)]

    (update-layer! layer grads learing-rate)
    (propagate-backwards layer (:grad-x grads))))


;; Backpropagacija cele mreze
(defn backpropagation [nn act class-idx learing-rate]
  (let [last-act (last (:act act))
        output (:y last-act)
        orig-input (:curr act)
        init-grad (init-gradient output class-idx)
        reversed-nn (reverse nn)
        ;; Vraca sve sem poslednjeg 
        layer-inputs (reverse (cons orig-input (map :y (butlast (:act act)))))
        reversed-act (reverse (:act act))]
    ;; reversed-act (reverse (:act act))
    ;; tot-layers (count nn)]

    (loop [layers reversed-nn
           act reversed-act
           inputs layer-inputs
           grad init-grad]
      ;;  grad init-grad
      ;;  idx 0]

      ;; Uslov za zavrsetak
      (if (empty? layers)
        nn

        (let [curr-layer (first layers)
              curr-act (first act)
              curr-input (first inputs)
              ;; layer-input (reverse-layer-iput act orig-input idx tot-layers)
              next-grad (backpropagation-singular curr-layer curr-act curr-input grad learing-rate)]
          ;; next-grad (backpropagation-singular curr-layer curr-act layer-input grad learing-rate)]

          (recur (rest layers)
                 (rest act)
                 (rest inputs)
                 ;;  (next-grad)
                 ;;  (inc idx)
                 next-grad))))))

;; Jedan korak u treniranju nn. 
;; Radi se forward pass, racunanje lossa (cross-entropije)
;; I na kraju se radi backpropagation
(defn single-step [nn input class-idx learing-rate]
  (println "Parameters single-step" nn input class-idx learing-rate)

  (let [act (nn-forward input nn)
        output (:y (last (:act act)))
        prob (softmax! (cor/copy output))
        ;; prob (softmax! (cor/copy! output))
        loss (cross-entropy prob class-idx)]
    (backpropagation nn act class-idx learing-rate)

    ;; Za predvidjenu klasnu se uzima index maksimalne verovatnoce
    {:loss loss :prob prob :pred-class (cor/imax prob)}))


;; Treniranje vise parametara 
;; Treniranje vise parametara 
(defn train-batch [nn inputs all-class-idx learning-rate]
  (println "Parameters train-batch" nn inputs all-class-idx learning-rate)
  (let [samples (cor/dim all-class-idx)
        tot-loss (atom 0.0)
        accurate-pred (atom 0)]

    (dotimes [i samples]
      (let [input (cor/row inputs i)
            class (cor/entry all-class-idx i)
            res (single-step nn input class learning-rate)
            loss (:loss res)
            pred (:pred-class res)]

        (swap! tot-loss + loss)
        ;; Kada prediktuje dobro
        (when (= pred class)
          (swap! accurate-pred inc))))

    {:avg-loss (/ @tot-loss samples) :accuracy (/ @accurate-pred samples)}))


(defn train-nn [nn inputs all-class-idx learing-rate epochs]

  (println "Parameters" nn inputs all-class-idx learing-rate epochs)
  (dotimes [e epochs]
    (let [stats (train-batch nn inputs all-class-idx learing-rate)]

      (println "$Epoch" (inc e))
      (println "$Avg loss" (:avg-loss stats))
      (println "$Accuraccy" (* 100 (:accuracy stats)))))

  nn)



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

(println "Testing single layer fn" (single-step nn input 1 0.1))

(def train-data (ntv/fge 4 4  [1.0 0.0 1.0 0.0
                               2.0 1.0 2.0 1.0
                               3.0 2.0 1.0 0.0
                               0.0 1.0 2.0 3.0]))


(def classes (ntv/iv 1 2 3))

;; (println "Testing nn traingin" (train-nn nn train-data classes 0.1 100))

;; (def training-test (train-nn nn train-data classes 0.1 50))


;; (def result (nn-forward input nn))

;; (def test-data
;;   (ntv/dge 3 4 [1.0 2.0 3.0 4.0
;;                 0.1 0.2 0.3 0.4
;;                 -1.0 -2.0 -3.0 -4.0]))

;; (defn test-softmax [data]
;;   (doseq [j (range (cor/mrows data))]
;;     (softmax! (cor/row data j)))
;;   data)

;; (println "Cross entropy test" (cross-entropy-batch test-data classes))


;; (def grd-data (ntv/dv [0.1 0.5 0.2]))

;; (println "Testing of initial grad fn" (init-gradient grd-data 2))

;; (def act-test
;;   {:act [{:y (ntv/dv [1 2])}
;;          {:y (ntv/dv [3 4])}
;;          {:y (ntv/dv [5 6])}
;;          {:y (ntv/dv [7 8])}]})

;; (def i (ntv/dv [9 9]))

;; (println "Testing of reverse-layer-input" (reverse-layer-iput act-test i 2 4))


;; (def grad-test (ntv/dv [0.1 0.2]))
;; (def cur-ac-test {:y (ntv/dv [0.4 0.6])})
;; (def layer-input-test (ntv/dv [1.0 2.0]))
;; ;; (def layer-input-test (ntv/dv [1.0 2.0 3.0]))


;; (def test-layer-gradients (layer-gradients grad-test cur-ac-test layer-input-test))

;; (println "x-grad" (:grad-x test-layer-gradients))
;; (println "weight grad" (:grad-weight test-layer-gradients))
;; (println "bias grad" (:grad-bias test-layer-gradients))


;; (def test-layer {:weights (ntv/dge 2 2 [0.1 0.2
;;                                         0.7 -0.5])
;;                  :bias (ntv/dv [0.0 0.0])})

;; (def new-grad-test {:grad-weight (ntv/dge 2 2 [0.01 0.02
;;                                                -0.03 0.04])
;;                     :grad-bias (ntv/dv [0.1 -0.1])})


;; (def class-idx 1)
;; (def learing-rate 0.1)

;; (def test-updated-layer (update-layer! test-layer new-grad-test learing-rate))
;; (println "updated weights" (:weights test-updated-layer))
;; (println "updated bias" (:bias test-updated-layer))

;; (def poropagated (propagate-backwards test-layer (ntv/dv [0.5 -0.2])))
;; (println "propagated" poropagated)
;; (doseq [i (range (cor/dim poropagated))]
;;   (println "prop[" i "]" (cor/entry poropagated i)))

;; (def test-backpropagate-singular (backpropagation-singular test-layer cur-ac-test layer-input-test grad-test learing-rate))
;; (println "propagated" test-backpropagate-singular)
;; (doseq [i (range (cor/dim test-backpropagate-singular))]
;;   (println "prev-grad[" i "]" (cor/entry test-backpropagate-singular i)))

;; (def nn-test
;;   [{:weights (ntv/dge 2 2 [0.1 0.2
;;                            0.3 0.4])
;;     :bias (ntv/dv [0.0 0.0])}
;;    {:weights (ntv/dge 2 2 [0.5 0.6
;;                            0.7 0.8])
;;     :bias (ntv/dv [0.0 0.0])}])

;; ;; Aktivacije
;; (def act-test
;;   {:curr (ntv/dv [1.0 2.0])
;;    :act [{:y (ntv/dv [0.1 0.2])}
;;          {:y (ntv/dv [0.3 0.7])}]})



;; (def updated-nn (backpropagation nn-test act-test class-idx learing-rate))
;; (doseq [layer updated-nn
;;         :let [W (:weights layer)
;;               b (:bias layer)]]
;;   (println "pdated weights")
;;   (doseq [i (range (cor/mrows W))
;;           j (range (cor/ncols W))]
;;     (println "W[" i "," j "] =" (cor/entry W i j)))
;;   (println "updated bias" b))

