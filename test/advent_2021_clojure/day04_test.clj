(ns advent-2021-clojure.day04-test
  (:require
    [clojure.test :refer :all]
    [advent-2021-clojure.day04 :refer :all]))

(def test-input (slurp "resources/day04_sample_data.txt"))
(def puzzle-input (slurp "resources/day04_data.txt"))

(deftest part1-test
    (are [expected input] (= expected (part1 input))
                          4512 test-input
                          49686 puzzle-input))

(deftest part2-test
    (are [expected input] (= expected (part2 input))
                          1924 test-input
                          26878 puzzle-input))