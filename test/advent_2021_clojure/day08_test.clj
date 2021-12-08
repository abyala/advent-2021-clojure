(ns advent-2021-clojure.day08-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day08 :refer :all]))

(def test-input (slurp "resources/day08_sample_data.txt"))
(def puzzle-input (slurp "resources/day08_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        26 test-input
                        470 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        61229 test-input
                        989396 puzzle-input))