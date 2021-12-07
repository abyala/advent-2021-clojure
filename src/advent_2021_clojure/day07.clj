(ns advent-2021-clojure.day07
  (:require [advent-2021-clojure.utils :as utils :refer [parse-int]]
            [clojure.string :as str]))

(defn parse-crabs [input]
  (map parse-int (str/split input #",")))

(defn total-crab-distance-to [f crabs pos]
  (->> crabs
       (map #(-> % (- pos) utils/abs f))
       (apply +)))

(defn solve [f input]
  (let [crabs (parse-crabs input)
        [first-crab last-crab] (apply (juxt min max) crabs)]
    (->> (range first-crab (inc last-crab))
         (map (partial total-crab-distance-to f crabs))
         (apply min))))

(defn part1 [input] (solve identity input))
(defn part2 [input] (solve utils/summation input))
