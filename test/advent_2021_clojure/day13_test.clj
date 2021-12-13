(ns advent-2021-clojure.day13-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day13 :refer :all]))

(def test-input (slurp "resources/day13_sample_data.txt"))
(def puzzle-input (slurp "resources/day13_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        17 test-input
                        684 puzzle-input))

(deftest fold-up-test
  ; Three lines, blank in the middle
  ; ##...      ###.#
  ; #####   ->
  ; #.#.#
  (is (= (fold #{[0 0] [1 0] [0 2] [2 2] [4 2]} [:y 1])
         #{[0 0] [1 0] [2 0] [4 0]}))

  ; Four lines
  ; ##...      ##...
  ; .#.#.   -> ####.
  ; .....
  ; #.#..
  (is (= (fold #{[0 0] [1 0] [1 1] [ 3 1] [0 3] [2 3] } [:y 2])
         #{[0 0] [1 0] [0 1] [1 1] [2 1] [3 1]})))

(deftest part2-test
  (println "This is not a test case, but requires visual inspection")

  ; The output should look like JRZBLGKH
  (part2 puzzle-input))
