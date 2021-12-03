(ns advent-2021-clojure.day01-windowed-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day01-windowed :refer :all]))

(def test-input "199\n200\n208\n210\n200\n207\n240\n269\n260\n263\n")
(def puzzle-input (slurp "resources/day01_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        7 test-input
                        1233 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        5 test-input
                        1275 puzzle-input))