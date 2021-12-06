(ns advent-2021-clojure.day06-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day06 :refer :all]))

(def test-input "3,4,3,1,2")
(def puzzle-input (slurp "resources/day06_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (day1 input))
                        5934 test-input
                        358214 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (day2 input))
                        26984457539 test-input
                        1622533344325 puzzle-input))