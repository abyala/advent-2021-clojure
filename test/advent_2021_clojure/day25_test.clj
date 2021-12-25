(ns advent-2021-clojure.day25-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day25 :refer :all]))

(def test-input (slurp "resources/day25_sample_data.txt"))
(def puzzle-input (slurp "resources/day25_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        58 test-input
                        414 puzzle-input))