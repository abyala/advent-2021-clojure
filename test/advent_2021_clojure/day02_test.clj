(ns advent-2021-clojure.day02-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day02 :refer :all]))

(def test-input "forward 5\ndown 5\nforward 8\nup 3\ndown 8\nforward 2")
(def puzzle-input (slurp "resources/day02_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        150 test-input
                        1484118 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        900 test-input
                        1463827010 puzzle-input))