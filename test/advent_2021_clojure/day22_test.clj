(ns advent-2021-clojure.day22-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day22 :refer :all]))

(def test-input1 (slurp "resources/day22_sample_data1.txt"))
(def test-input2 (slurp "resources/day22_sample_data2.txt"))
(def test-input3 (slurp "resources/day22_sample_data3.txt"))
(def puzzle-input (slurp "resources/day22_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        39 test-input1
                        590784 test-input2
                        590467 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        2758514936282235 test-input3
                        1225064738333321 puzzle-input))
