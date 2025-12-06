(ns main
  (:gen-class)) ;Za runovanje paketa


;Ovde ce da baci sintaksnu gresku ovo ti je samo da bi naucio da radis
;(map fn (+ % 50)
;     map
;     (fn [line]
;       (Integer/parseInt
;         (str (if (= \R (first line)) "+" "-")
;              (subs line 1))))
;     (clojure.string/split-lines (slurp "src/testnew.txt")))

(defn read-lines [loaded]
  (try
    (when loaded ;
      (clojure.string/split-lines loaded))
    (catch Exception e
      (println (str "Unexpected error occurred: " (.getMessage e)))
      nil)))


(defn safe-slurp [file-path]
    (slurp file-path))


(defn plusminus [s]
  ;
  (let [sign-adjusted-s (if (.startsWith s "L")
                          (str "-" (subs s 1))
                          ;Stavlja se empty stirng posto ne mora + posto je svakako pozitivan broj
                          (str "" (subs s 1)))]

    (Integer/parseInt sign-adjusted-s)))

(defn pwd_f [numbers]
  ;; Definiše se hash mapa sa početnom pozicijom i brojačem
  (let [initial {:start 50 :count 0 :count2 0}]
    (reduce (fn [acc num]
              ;; Stavlja se trenutna pozicija na akumulator
              (let [cur-pos (:start acc)
                    ;Racunanje nove pozicije
                    new-pos (mod (+ cur-pos num) 100)
                    ;Ako je nova pozicija 0 dodaje se u count
                    new-count (if (zero? new-pos)
                                (inc (:count acc))
                                (:count acc))
                    ;Drugi count kada predje preko nule racuna se ako je broj i trenutni veci od 100 ili je razlika brojeva manja od nule
                    new-count2 (cond
                                 ;Negativan broj proverava se da li je broj koji treba da se odume veci od broja od kojeg se oduzima
                                 (< num 0)
                                 (if (> (Math/abs num) cur-pos)
                                   (inc (:count2 acc))
                                   (:count2 acc))

                                 ;Pozitivan proverava se da li je zbir 2 broja veci od 100 (znaci presao je 1000)
                                 (> num 0)
                                 (if (> (+ cur-pos num) 100)
                                   (inc (:count2 acc))
                                   (:count2 acc))

                                 :else
                                 (:count2 acc))]

                (println "Trenutna:" cur-pos " num:" num " -> Sledeca:" new-pos "Acc:" acc)

                {:start new-pos :count new-count :count2 new-count2 }))

            ;Ovde je inicijalna vrednost brojaca
            initial
            ;Ovde je sequenca brojeva nad kojom se vrsi reduce
            numbers)))
(defn -main
  [& args]

  (let [test (pwd_f (map plusminus (read-lines (safe-slurp "src/test.txt"))))]
    (if test
      (println "Uspesno provaljena sifra:\n" test)
      (println "Niije ucitao fajl"))))


