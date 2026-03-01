(ns ddos.simplenn
  ;; Kasnije refraktorisi sa :refer all
  (:require [uncomplicate.neanderthal.core :as cor]
            [uncomplicate.neanderthal.native :as ntv]
            [uncomplicate.neanderthal.random :refer [rand-uniform! rand-normal!]]
            [uncomplicate.neanderthal.vect-math :as v]
            [uncomplicate.neanderthal.math :as m]
            [ddos.importexport :refer :all]))


;; ____________________FUNKCIJE_____________________
;; Kasnije optimizovati
(defn sigmoid! [n]
  (try
    (let [vec-size (cor/dim n)]
      (dotimes [i vec-size]
        (let [j (cor/entry n i)]
          (cor/entry! n i (/ 1 (+ 1 (Math/exp (- j))))))))

    n
    (catch Exception e
      (let [ste (first (.getStackTrace e))]
        (println "Exception sigmoid!" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))


;; σ'(x) = σ(x) * (1 - σ(x))
(defn sigmoid-der! [n]
  (try
    (let [vec-size (cor/dim n)]
      (dotimes [i vec-size]
        (let [j (cor/entry n i)]
          (cor/entry! n i (* j (- 1 j))))))
    ;; (cor/entry! n i (* j (- 1 j))))))
    n
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception sigmoid-der!" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))


;; Ovo je fja za enkodiranje timestampa u ciklicnu repezentaciju vrenma
;; Pomocu sinusa i kosinus, ovo je najjednostavniji pristup!
(defn cyclic-timestamp [timestamp]
  (try
    (let [seconds (mod timestamp 86400)
          angle (* 2 Math/PI (/ seconds 86400))]
      [(Math/sin angle) (Math/cos angle)])

    (catch Exception e
      (let [ste (first (.getStackTrace e))]
        (println "Exception cyclic-timestamp!" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))

(defn parse-feature [col-name value]
  (if (= col-name "attack-active")
    (if (= value "true") 1.0 0.0)
    (Double/parseDouble value)))

(defn relu! [n]
  (try
    (let [vec-size (cor/dim n)]
      (dotimes [i vec-size]
        (let [j (cor/entry n i)]
          (cor/entry! n i (max 0.0 j))))
      n)
    (catch Exception e
      (let [ste (first (.getStackTrace e))]
        (println "Exception relu!" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))



;; relu(x) = 1 ako x > 0 u suprotnom 0
(defn relu-der! [n]
  (try
    (let [vec-size (cor/dim n)]
      (dotimes [i vec-size]
        (let [j (cor/entry n i)]
          (cor/entry! n i (if (> j 0.0) 1.0 0.0))))
      n)
    (catch Exception e
      (let [ste (first (.getStackTrace e))]
        (println "Exception relu-der!" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))

(defn create-layer [input-size output-size activation-fn]
  ;; Moraces da napises testove koji ce proveravati da li unos validan
  (try
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
       :activation activation-fn})
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception create-layer" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))


;; He inicilajizacija koja se koristi kod relu aktivacione funkcije za sprecavanje nestajucih ili eksplozivnih gradijenata
;; Wraper create layer f-je
(defn create-layer-he [input-size output-size activation-fn]
  (let [layer (create-layer input-size output-size activation-fn)
        std (Math/sqrt (/ 2.0 input-size))]
    (rand-normal! 0.0 std (:weights layer))
    (cor/scal! 0.0 (:bias layer))
    layer))

(defn forward [layer input]
  ;; Uradi test za validaciju
  ;;  Za linearnu funkciju formula x = weight * input + bias | Nelinearna je fja aktivacije na linearni deo = y activation(x)
  (try
    ;; Dodat je uslov za linearnost kod poslednjeg sloja 
    (let [{:keys [weights bias activation linear?]} layer
          linear (cor/mv weights input)
          x (cor/axpy! 1.0 bias linear)
          ;; x (cor/axpy! 1.0 bias (cor/mv weights input))
          y (if linear? x (activation x))]
      {:x x :y y})
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception forward" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))

(defn nn-forward [input network]
  ;; Trebaces da dodas test za kompatibilnost slojeva
  (try
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
      final)
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception nn-forward" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))


;;  Kros entropija za jedan vektor
(defn cross-entropy [vec class-idx]
  (try
    (let [max-val (cor/amax vec)
          shifted (doto (cor/copy vec)
                    (#(dotimes [i (cor/dim %)]
                        (cor/entry! % i (- (cor/entry % i) max-val)))))
          ;; shifted (cor/axpy! -1 max-val (cor/copy vec))
          sum-exp (+ max-val (m/log (cor/sum (v/exp! shifted))))
          final-vec (cor/entry vec class-idx)]
      (- sum-exp final-vec))
    (catch Exception e
      (let [ste (first (.getStackTrace e))]
        (println "Exception cross-entropy" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))



;;  Kros entropija za ceo batch
(defn cross-entropy-batch [vec all-class-idx]
  (try
    (let [batch-size (cor/mrows vec)
          losses (for [i (range batch-size)]
                   (cross-entropy (cor/row vec i)
                                  (cor/entry all-class-idx i)))]
      (/ (reduce + 0.0 losses) batch-size))
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception cross-entropy-batch" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))



;; Normalizacija vektora u raspodelu verovatnoce
(defn softmax! [v]
  (try
    (let [m (cor/amax v)
          n (cor/dim v)]
      ;; v = v - m
      (dotimes [i n]
        (let [x (cor/entry v i)]
          (cor/entry! v i (- x m))))
      (v/exp! v)
      (let [s (cor/sum v)]
        (cor/scal! (/ 1.0 s) v))

      v)
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception softmax!" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))



;; Racunanje inicijalnog gradijenta (izlaznog sloja: softmax + cross-entropy)
(defn init-gradient [output class-idx]
  (try
    (let [grad (cor/copy output)]
      (cor/entry! grad class-idx
                  (- (cor/entry grad class-idx) 1.0))

      grad)
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception init-gradient" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))



;; Fja koja izvlaci input odredjenog sloja (unazad!)
(defn reverse-layer-iput [act input layer-idx tot-layers]
  (try
    (if (= layer-idx (dec tot-layers))
      input
      (:y (nth (reverse (:act act)) (inc layer-idx))))
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception reverse-layer-input" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))
;; Dodaj test za proveru argumenata kasnije !



;; Fja za racunanje gradijenta sloja
(defn layer-gradients [grad curr-ac layer-input]
  (try
    (let [act-der (relu-der! (cor/copy (:y curr-ac)))
          ;; (let [act-der (sigmoid-der! (cor/copy (:y curr-ac)))
          grad-x (v/mul! grad act-der)
          ;; grad-x (cor/emul! grad act-der)
          ;; grad-x (cor/mul! grad act-der)
          ;; Spoljni proizvod!
          n (cor/dim grad-x)
          m (cor/dim layer-input)
          ;; Vidi da unapred definises da li ce se koristiti double ili float (moze i neki test)
          ;;grad-weight (ntv/dge n m)
          grad-weight (ntv/fge n m)]
      ;Spoljni proizvod sa doseq je jako sporo radilo!
      (cor/rk! 1.0 grad-x layer-input grad-weight)
      ;; (cor/ger! 1.0 grad-x layer-input grad-weight)
      ;; 
      ;; (doseq [i (range n)
      ;;         j (range m)]
      ;; 
      ;;   (cor/entry! grad-weight i j (* (cor/entry grad-x i)
      ;;                                  (cor/entry layer-input j))))
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

      {:grad-x grad-x :grad-weight grad-weight :grad-bias (cor/copy grad-x)})

    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception layer-gradients" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))


;; Updejtovanje parametra layera
(defn update-layer! [layer grads learning-rate]
  (try
    (println "weight-dims:" (cor/mrows (:weights layer)) "x" (cor/ncols (:weights layer)))
    (println "grad-weight dims:" (cor/mrows (:grad-weight grads)) "x" (cor/ncols (:grad-weight grads)))
    (println "bias dim:" (cor/dim (:bias layer)))
    (cor/axpy! (- learning-rate) (:grad-weight grads) (:weights layer))
    (cor/axpy! (- learning-rate) (:grad-bias grads) (:bias layer))
    layer

    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception update-layer" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))



;; Propagacija gradijanta unazad
(defn propagate-backwards [layer grad-x]
  (try
    (cor/mv (cor/trans (:weights layer)) grad-x)
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception propagate-backwards" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))

;; Funkcija za backpropagaciju jednog sloja
(defn backpropagation-singular [layer act layer-input grad learing-rate]
  (try
    (let [grads (layer-gradients grad act layer-input)]

      (update-layer! layer grads learing-rate)
      (propagate-backwards layer (:grad-x grads)))


    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception backpropagation-singular" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))


;; Backpropagacija cele mreze
(defn backpropagation [nn act orig-input class-idx learing-rate]
  (try

    (let [last-act (last (:act act))
          output (:y last-act)
          ;;orig-input (:curr act)
          init-grad (init-gradient output class-idx)
          reversed-nn (reverse nn)
          ;; Vraca sve sem poslednjeg  layer-inputs (reverse (cons orig-input (map :y (butlast (:act act)))))
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
                   next-grad)))))

    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception backpropagation" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))

;; Jedan korak u treniranju nn. 
;; Radi se forward pass, racunanje lossa (cross-entropije)
;; I na kraju se radi backpropagation
(defn single-step [nn input class-idx learing-rate]
  (try
    (println "Parameters single-step" nn input class-idx learing-rate)

    (let [act (nn-forward input nn)
          output (:y (last (:act act)))
          prob (softmax! (cor/copy output))
          ;; prob (softmax! (cor/copy! output))
          loss (cross-entropy output class-idx)]
      ;;loss (cross-entropy prob class-idx)

      (backpropagation nn act input class-idx learing-rate)

      ;; Za predvidjenu klasnu se uzima index maksimalne verovatnoce
      {:loss loss :prob prob :pred-class (cor/imax prob)})
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception single-step" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))



;; Treniranje vise parametara 
;; Treniranje vise parametara 
(defn train-batch [nn inputs all-class-idx learning-rate]
  (try
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

      {:avg-loss (/ @tot-loss samples) :accuracy (/ @accurate-pred samples)})

    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception train-batch" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))



(defn train-nn [nn inputs all-class-idx learing-rate epochs]
  (try

    (println "Parameters" nn inputs all-class-idx learing-rate epochs)
    (dotimes [e epochs]
      (let [stats (train-batch nn inputs all-class-idx learing-rate)]

        (println "$Epoch" (inc e))
        (println "$Avg loss" (:avg-loss stats))
        (println "$Accuraccy" (* 100 (:accuracy stats)))))

    nn
    (catch Exception e
      (let [ste (first (.getStackTrace e))]

        (println "Exception train-nn" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))


(defn normalize-data [data]
  (try
    ;; Max-min normalizacija po kolonama

    (let [data-features (count (first data))
          max-vals (vec (for [i (range data-features)]
                          (apply max (map #(nth % i) data))))
          min-vals (vec (for [i (range data-features)]
                          (apply min (map #(nth % i) data))))
          normalized
          (mapv (fn [row]
                  (mapv (fn [i]
                          (let [mx (nth max-vals i)
                                mn (nth min-vals i)
                                diff (- mx mn)]
                            (if (< (Math/abs diff) 1e-8)
                              0.0
                              (/ (- (nth row i) mn) diff))))
                        (range data-features)))
                data)]
      {:normalized normalized
       :max max-vals
       :min min-vals})

    (catch Exception e
      (let [ste (first (.getStackTrace e))]
        (println "Exception normalize-data" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))


(defn prepare-data [file features input-labels]
  (try
    (let [raw-data (load-csv file)
          header (:header raw-data)
          data (:data raw-data)
          col-map (zipmap header (range))
          label-idx (get col-map "label")
          timestamp-idx (get col-map "timestamp")
          other-feat-idx (mapv (fn [name] (get col-map name)) features)

          processed-data
          (vec (for [row data]
                 (let [base-features (mapv (fn [idx] (nth row idx)) other-feat-idx)
                       parsed-base (mapv (fn [name val]
                                           (parse-feature name val))
                                         features
                                         base-features)
                       ts-raw (nth row timestamp-idx)
                       timestamp (Double/parseDouble ts-raw)
                       time-encoded (cyclic-timestamp timestamp)
                       sin-t (first time-encoded)
                       cos-t (second time-encoded)]
                   (conj parsed-base sin-t cos-t))))

          labels
          (vec (for [row data]
                 (let [label-val (nth row label-idx)
                       idx (get input-labels label-val)]
                   (when (nil? idx)
                     (println "Unknown class:" label-val))
                   (or idx -1))))

          norm-res (normalize-data processed-data)
          normalized-data (:normalized norm-res)

          n-samples (count normalized-data)
          n-features (count (first normalized-data))
          all-values (float-array (flatten normalized-data))
          feature-matrix (ntv/fge n-features n-samples all-values)
          ;; all-values (flatten normalized-data)
          ;; feature-matrix (ntv/fge n-features n-samples all-values)
          final-matrix (cor/trans feature-matrix)
          label-vector (let [n (count labels)
                             lv (ntv/iv n)]
                         (dotimes [i n]
                           (cor/entry! lv i (nth labels i)))
                         lv)]
      ;; label-vector (ntv/iv (count labels) (int-array labels))
      ;; ;;label-vector (apply ntv/iv labels)
      ;; label-vector (ntv/iv (int-array labels)) 


      {:features final-matrix
       :labels label-vector
       :n-samples n-samples
       :n-features n-features})
    (catch Exception e
      (let [ste (first (.getStackTrace e))]
        (println "Exception prepare-data" (.getMessage e) "Line:" (.getLineNumber ste)))
      (throw e))))


(defn predict [nn input]
  (let [act (nn-forward input nn)
        output (:y (last (:act act)))
        prob (softmax! (cor/copy output))]
    (cor/imax prob)))


(defn evaluate [nn features labels]
  (let [samples (cor/dim labels)]

    (dotimes [i samples]
      (let [input (cor/row features i)
            actual (int (cor/entry labels i))
            pred (predict nn input)]

        ;; Kasnije dodaj confusion matricu i ostlau metriku tancosti modela (precision recall)
        (println "Sample" i
                 "| Actual class:" actual
                 "| Prediction:" pred
                 "| True:" (= pred actual))))))


;; ___________________TRENIRANJE MREZE___________________________

(def features
  ["top-dst-port-share" "src-ip-entropy" "udp-ratio" "dst-ip-entropy"
   "dns-query-ratio" "unique-dst-ips" "unique-src-ips" "tcp-syn-ratio"
   "top-src-ip-packet-share" "tcp-fin-ratio" "packet-rate" "tcp-ratio"
   "unique-flows" "std-packet-size" "icmp-ratio" "dst-subnet-spread"
   "byte-rate" "avg-packet-size" "tcp-ack-ratio" "dns-response-ratio"
   "top-src-ip-byte-share" "dst-port-entropy" "attack-active"])

(def labels
  {"ack-flood" 0
   "icmp-flood" 1
   "ntp-amplification" 2
   "udp-flood-large" 3
   "udp-flood-mixed" 4
   "dns-amplification" 5
   "normal" 6
   "subnet-carpet-bombing" 7
   "syn-flood" 8})



;; A ovde ces da napises test za komatibilnost sloja i unosa moraju da se gadjaju dimenzije matrica!!!!!!!
;; (defn network [layer input]
;;   (let [w (:weights layer)
;;         b (:bias layer)

;;         output (cor/copy! b (ntv/fv (:output-size layer)))]
;;     (cor/mv! w input output)))


(def nn [(create-layer-he 25 64 relu!)
         (create-layer-he 64 32 relu!)
         (assoc (create-layer-he 32 9 nil) :linear? true)])

;; (def nn [(create-layer 4 8 relu!)
;;          (create-layer 8 6 relu!)
;;          (assoc (create-layer 6 9 nil) :linear? true)])

;; (def nn [(create-layer 4 8 sigmoid!)
;;          (create-layer 8 6 sigmoid!)
;;          (create-layer 6 2 sigmoid!)])

(def input (ntv/fv [1.1 2.2 3.3 4.4]))

;; (println "Testing single layer fn" (single-step nn input 1 0.1))

(def train-data (ntv/fge 9 4 [1.0 0.0 1.0 0.0
                              2.0 1.0 2.0 1.0
                              3.0 2.0 1.0 0.0
                              0.0 1.0 2.0 3.0
                              1.5 0.5 1.5 0.5
                              2.5 1.5 0.5 1.0
                              0.5 2.0 3.0 1.0
                              3.0 3.0 0.0 1.0
                              1.0 2.0 0.0 3.0]))


(println "Test normalize data" normalize-data train-data)
;; (println "Work dir" (System/getProperty "user.dir"))
;; 

(def dataset (prepare-data "new_ddos_dataset.csv" features labels))
(def train-features (:features dataset))
(def train-labels (:labels dataset))

(def trained-nn (train-nn nn train-features train-labels 0.01 10))

(evaluate trained-nn train-features train-labels)
;; (let [result1 (prepare-data "new_ddos_dataset.csv" features labels)]
;;   (evaluate nn (:features result1) (:labels result1)))



;; (let [result (prepare-data "new_ddos_dataset.csv" features labels)
;;       train-features (:features result)
;;       train-labels (:labels result)
;;       nn [(create-layer-he 25 64 relu!)
;;           (create-layer-he 64 32 relu!)
;;           (assoc (create-layer-he 32 9 nil) :linear? true)]]
;;   (train-nn nn train-features train-labels 0.01 10))

;; (let [result (prepare-data "new_ddos_dataset.csv" features labels)]
;;   (println "Num rows:" (:n-samples result))
;;   (println "Num features:" (:n-features result))
;;   (println "Labels:" (take 5000 (:labels result)))
;;   
;;   )
;; (def train-data (ntv/fge 4 4  [1.0 0.0 1.0 0.0
;;                                2.0 1.0 2.0 1.0
;;                                3.0 2.0 1.0 0.0
;;                                0.0 1.0 2.0 3.0]))


(def classes (ntv/iv 0 1 2 3 4 5 6 7 8))


;; (def classes (ntv/iv 1 2 3 4))
;; 
;; (println "Testing nn traingin" (train-nn nn train-data classes 0.1 100))
;; 
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

