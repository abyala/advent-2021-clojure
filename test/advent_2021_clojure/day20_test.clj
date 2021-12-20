(ns advent-2021-clojure.day20-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day20 :refer :all]))

(def test-input (slurp "resources/day20_sample_data.txt"))
(def puzzle-input (slurp "resources/day20_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        35 test-input
                        5571 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        3351 test-input
                        17965 puzzle-input))

(defn print-image [image]
  (->> (group-by (comp second first) image)
       sort
       (map (fn [[_ points]] (apply str (map (comp {1 light-pixel, 0 dark-pixel} second)
                                             (sort-by ffirst points)))))
       (run! println)))