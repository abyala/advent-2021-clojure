(ns advent-2021-clojure.day18-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day18 :refer :all]))

(def puzzle-input (slurp "resources/day18_data.txt"))

(deftest explode-tree-test
  (is (= (explode-tree [[[[[9,8],1],2],3],4])
         [[[[0,9],2],3],4]))
  (is (= (explode-tree [7,[6,[5,[4,[3,2]]]]])
         [7,[6,[5,[7,0]]]]))
  (is (= (explode-tree [[6,[5,[4,[3,2]]]],1])
         [[6,[5,[7,0]]],3]))
  (is (= (explode-tree [[3,[2,[1,[7,3]]]],[6,[5,[4,[3,2]]]]])
         [[3,[2,[8,0]]],[9,[5,[4,[3,2]]]]]))
  (is (= (explode-tree [[3,[2,[8,0]]],[9,[5,[4,[3,2]]]]])
         [[3,[2,[8,0]]],[9,[5,[7,0]]]])))

(deftest split-tree-test
  (is (= (split-tree [[[[0,7],4],[15,[0,13]]],[1,1]])
         [[[[0,7],4],[[7,8],[0,13]]],[1,1]]))
  (is (= (split-tree [[[[0,7],4],[[7,8],[0,13]]],[1,1]])
         [[[[0,7],4],[[7,8],[0,[6,7]]]],[1,1]])))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        ;;45 test-input
                        3574 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        4763 puzzle-input))
