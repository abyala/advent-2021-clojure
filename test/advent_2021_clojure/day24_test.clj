(ns advent-2021-clojure.day24-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day24 :refer :all]))

(def puzzle-input (slurp "resources/day24_data.txt"))

(deftest magic-function-test
  (is (= 112 (magic-function [1 2 3] 4 5)))
  (is (= 4 (magic-function [1 2 3] 4 6)))
  (is (= 268 (magic-function [3 2 3] 30 5)))
  (is (= 10 (magic-function [3 2 3] 30 6))))

(deftest part1-test
  (is (= 36969794979199 (part1 puzzle-input))))

(deftest part2-test
  (is (= 11419161313147 (part2 puzzle-input))))

