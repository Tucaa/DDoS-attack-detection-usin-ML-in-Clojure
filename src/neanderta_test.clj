(ns neanderthal_test  
  (:require [uncomplicate.neanderthal.core :refer :all]
            [uncomplicate.neanderthal.native :refer :all]))

(let [x (dv (range 100000))
      y (copy x)]
  (dot x y))