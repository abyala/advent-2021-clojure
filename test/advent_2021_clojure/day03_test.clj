(ns advent-2021-clojure.day03-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day03 :refer :all]))

(def test-input "00100\n11110\n10110\n10111\n10101\n01111\n00111\n11100\n10000\n11001\n00010\n01010")
(def puzzle-input (slurp "resources/day03_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        198 test-input
                        3277364 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        230 test-input
                        5736383 puzzle-input))