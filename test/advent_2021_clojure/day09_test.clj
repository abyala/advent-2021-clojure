(ns advent-2021-clojure.day09-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day09 :refer :all]))

(def test-input "2199943210\n3987894921\n9856789892\n8767896789\n9899965678")
(def puzzle-input (slurp "resources/day09_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        15 test-input
                        444 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        1134 test-input
                        1168440 puzzle-input))