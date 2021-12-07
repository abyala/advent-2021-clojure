(ns advent-2021-clojure.day07-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day07 :refer :all]))

(def test-input "16,1,2,0,4,2,7,1,2,14")
(def puzzle-input (slurp "resources/day07_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        37 test-input
                        349769 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        168 test-input
                        99540554 puzzle-input))