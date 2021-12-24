(ns advent-2021-clojure.day23-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day23 :refer :all]))

(def test-input [:b :a :c :d :b :c :d :a])
(def puzzle-input [:b :d :b :c :d :a :a :c])

(deftest room-exiting?-test
  (let [burrow {:rooms {:a [:a :a] :b [:b :c] :c [] :d [:c :d]}}]
    (is (not (room-exiting? burrow :a)))
    (is (some? (room-exiting? burrow :b)))
    (is (not (room-exiting? burrow :c)))
    (is (some? (room-exiting? burrow :d)))))

; Don't run this; it takes about 100 minutes to run on the puzzle data set
#_(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        12521 test-input
                        15412 puzzle-input))

; Part 2 took 8 minutes for part 1, and 6 minutes for part 2
#_(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        44169 test-input
                        52358 puzzle-input))
