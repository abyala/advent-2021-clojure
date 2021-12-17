(ns advent-2021-clojure.day17-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day17 :refer :all]))

(def test-input "target area: x=20..30, y=-10..-5")
(def puzzle-input "target area: x=70..125, y=-159..-121")

(deftest progress-test
  (let [target [10 20, -15 -5]]
    (are [expected point] (= expected (progress target point))
                          :hit [10 -5]
                          :hit [19 -14]
                          :hit [20 -15]
                          :miss [21 -4]
                          :miss [30 -5]
                          :miss [5 -16]
                          :miss [20 -20]
                          :approaching [0 0]
                          :approaching [10 0]
                          :approaching [9 -15])))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        45 test-input
                        12561 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        112 test-input
                        3785 puzzle-input))
