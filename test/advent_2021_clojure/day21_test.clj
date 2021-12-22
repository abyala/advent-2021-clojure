(ns advent-2021-clojure.day21-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day21 :refer :all]))

(def test-input [4 8])
(def puzzle-input [10 8])

(deftest part1-test
  (are [expected players] (= expected (part1 (first players) (second players)))
                        739785 test-input
                        752247 puzzle-input))

(deftest board-pos-test
  (are [expected n] (= expected (board-pos n))
                    1 1
                    2 2
                    9 9
                    10 10
                    1 11
                    9 19
                    10 20
                    1 21))

(deftest move-player-test
  (is (= (move-player {:players [{:pos 1, :score 3} {:pos 2, :score 4}], :next-player 0} 5)
         {:players [{:pos 6, :score 9} {:pos 2, :score 4}], :next-player 1}))
  (is (= (move-player {:players [{:pos 1, :score 3} {:pos 2, :score 4}], :next-player 1} 5)
         {:players [{:pos 1, :score 3} {:pos 7, :score 11}], :next-player 0})))

(deftest winner-test
  (is (nil? (winner {:players [{:pos 1, :score 3} {:pos 2, :score 4}], :next-player 0} 5)))
  (is (nil? (winner {:players [{:pos 1, :score 3} {:pos 2, :score 4}], :next-player 1} 5)))
  (is (= 0 (winner {:players [{:pos 1, :score 3} {:pos 2, :score 2}], :next-player 0} 3)))
  (is (= 1 (winner {:players [{:pos 1, :score 3} {:pos 2, :score 4}], :next-player 0} 4))))

(deftest part2-test
  (are [expected players] (= expected (part2 (first players) (second players)))
                          444356092776315 test-input
                          221109915584112 puzzle-input))
