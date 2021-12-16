(ns advent-2021-clojure.day16-test
  (:require [clojure.test :refer :all]
            [advent-2021-clojure.day16 :refer :all]))

(def puzzle-input (slurp "resources/day16_data.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        16 "8A004A801A8002F478"
                        12 "620080001611562C8802118E34"
                        23 "C0015000016115A2E0802F182340"
                        31 "A0016C880162017C3686B18A3D4780"
                        883 puzzle-input))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        3 "C200B40A82"
                        54 "04005AC33890"
                        7 "880086C3E88112"
                        9 "CE00C43D881120"
                        1 "D8005AC2A8F0"
                        0 "F600BC2D8F"
                        0 "9C005AC2F8F0"
                        1 "9C0141080250320F1802104A08"
                        1675198555015 puzzle-input))
