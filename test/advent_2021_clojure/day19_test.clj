(ns advent-2021-clojure.day19-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day19 :refer :all]))

(def test-input (slurp "resources/day19_sample_data.txt"))
(def puzzle-input (slurp "resources/day19_data.txt"))

; Again, don't run the tests today. The puzzle data takes about 10 minutes to run each time.
#_(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        79 test-input
                        440 puzzle-input))

#_(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        3621 test-input
                        13382 puzzle-input))
