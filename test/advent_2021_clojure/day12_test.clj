(ns advent-2021-clojure.day12-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day12 :refer :all]))

(def simple-test "start-A\nstart-b\nA-c\nA-b\nb-d\nA-end\nb-end")
(def test-input "dc-end\nHN-start\nstart-kj\ndc-start\ndc-HN\nLN-dc\nHN-end\nkj-sa\nkj-HN\nkj-dc")
(def large-test "fs-end\nhe-DX\nfs-he\nstart-DX\npj-DX\nend-zg\nzg-sl\nzg-pj\npj-he\nRW-he\nfs-DX\npj-RW\nzg-RW\nstart-pj\nhe-WI\nzg-he\npj-fs\nstart-RW")
(def puzzle-input (slurp "resources/day12_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        10 simple-test
                        19 test-input
                        226 large-test
                        4885 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        36 simple-test
                        103 test-input
                        3509 large-test
                        117095 puzzle-input))