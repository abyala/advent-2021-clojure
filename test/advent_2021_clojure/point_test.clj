(ns advent-2021-clojure.point-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.point :refer :all]))

(deftest inclusive-line-between-test
  (are [p1 p2 expected] (= expected (inclusive-line-between p1 p2))
                        [0 0] [0 0] '([0 0])
                        [2 3] [2 6] '([2 3] [2 4] [2 5] [2 6])
                        [2 6] [2 3] '([2 6] [2 5] [2 4] [2 3])
                        [2 3] [5 3] '([2 3] [3 3] [4 3] [5 3])
                        [5 3] [2 3] '([5 3] [4 3] [3 3] [2 3])
                        [2 3] [4 5] '([2 3] [3 4] [4 5])
                        [2 3] [4 1] '([2 3] [3 2] [4 1])
                        [2 3] [0 5] '([2 3] [1 4] [0 5])
                        [2 3] [0 1] '([2 3] [1 2] [0 1])))