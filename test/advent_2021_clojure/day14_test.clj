(ns advent-2021-clojure.day14-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day14 :refer :all]))

(def test-input (slurp "resources/day14_sample_data.txt"))
(def puzzle-input (slurp "resources/day14_data.txt"))

(deftest part1-test
    (are [expected input] (= expected (part1 input))
                          1588 test-input
                          3058 puzzle-input))

(deftest part2-test
    (are [expected input] (= expected (part2 input))
                          2188189693529 test-input
                          3447389044530 puzzle-input))
