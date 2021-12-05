(ns advent-2021-clojure.day05-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day05 :refer :all]))

(def test-input "0,9 -> 5,9\n8,0 -> 0,8\n9,4 -> 3,4\n2,2 -> 2,1\n7,0 -> 7,4\n6,4 -> 2,0\n0,9 -> 2,9\n3,4 -> 1,4\n0,0 -> 8,8\n5,5 -> 8,2")
(def puzzle-input (slurp "resources/day05_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        5 test-input
                        4993 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        12 test-input
                        21101 puzzle-input))