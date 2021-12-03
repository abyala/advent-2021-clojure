(ns advent-2021-clojure.day03
  (:require
    [advent-2021-clojure.utils :refer [parse-binary]]
    [clojure.string :as str]))

(defn parse-nums [input] (->> input str/split-lines (map parse-binary)))
(defn range-down-from [n] (range n -1 -1))

(defn most-significant-bit [nums]
  (let [n (apply max nums)]
    (int (/ (Math/log n) (Math/log 2)))))

(defn bit-diff [nums bit]
  (reduce #(if (bit-test %2 bit) (inc %1) (dec %1))
          0
          nums))

(defn most-common-bit [nums bit] (if (neg-int? (bit-diff nums bit)) 0 1))
(defn least-common-bit [nums bit] (if (neg-int? (bit-diff nums bit)) 1 0))

(defn one-pass-bit-check [nums bit-fn]
  (->> (range-down-from (most-significant-bit nums))
       (map (partial bit-fn nums))
       (apply str)
       parse-binary))

(defn multi-pass-bit-check [nums bit-fn]
  (first (reduce (fn [remaining bit] (if (= 1 (count remaining))
                                       (reduced remaining)
                                       (let [test (= 1 (bit-fn remaining bit))]
                                         (filter #(= test (bit-test % bit)) remaining))))
                 nums
                 (range-down-from (most-significant-bit nums)))))

(defn gamma-rate [nums] (one-pass-bit-check nums most-common-bit))
(defn epsilon-rate [nums] (one-pass-bit-check nums least-common-bit))
(defn oxygen-generator-rating [nums] (multi-pass-bit-check nums most-common-bit))
(defn co2-scrubber-rating [nums] (multi-pass-bit-check nums least-common-bit))

(defn multiply-rates [input & rate-fns]
  (->> (parse-nums input)
       ((apply juxt rate-fns))
       (apply *)))

(defn part1 [input] (multiply-rates input gamma-rate epsilon-rate))
(defn part2 [input] (multiply-rates input oxygen-generator-rating co2-scrubber-rating))
