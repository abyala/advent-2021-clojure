(ns advent-2021-clojure.day11-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day11 :refer :all]))

(def test-input (slurp "resources/day11_sample_data.txt"))
(def puzzle-input (slurp "resources/day11_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        1656 test-input
                        1686 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        195 test-input
                        360 puzzle-input))