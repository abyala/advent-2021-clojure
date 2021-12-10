(ns advent-2021-clojure.day10-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day10 :refer :all]))

(def test-input (slurp "resources/day10_sample_data.txt"))
(def puzzle-input (slurp "resources/day10_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        26397 test-input
                        388713 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        288957 test-input
                        3539961434 puzzle-input))