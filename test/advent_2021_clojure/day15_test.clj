(ns advent-2021-clojure.day15-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day15 :refer :all]))

(def test-input (slurp "resources/day15_sample_data.txt"))
(def puzzle-input (slurp "resources/day15_data.txt"))

(deftest part1-test
    (are [expected input] (= expected (part1 input))
                          40 test-input
                          398 puzzle-input))

(deftest part2-test
    (are [expected input] (= expected (part2 input))
                          315 test-input
                          2817 puzzle-input))
