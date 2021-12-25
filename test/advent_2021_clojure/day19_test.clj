(ns advent-2021-clojure.day19-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day19 :refer :all]))

(def puzzle-input (slurp "resources/day19_data.txt"))

#_(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        ;;45 test-input
                        3574 puzzle-input))

#_(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        4763 puzzle-input))
