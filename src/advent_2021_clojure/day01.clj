(ns advent-2021-clojure.day01
  (:require [clojure.string :as str]
            [advent-2021-clojure.utils :refer [parse-int]]))

(defn parse-measurements [input]
  (->> (str/split-lines input)
       (map parse-int)))

(defn solve [input mapping]
  (->> (parse-measurements input)
       (mapping)
       (partition 2 1)
       (filter (partial apply <))
       count))

(defn three-measurement-sliding-window [measurements]
  (->> (partition 3 1 measurements)
       (map (partial apply +))))

(defn part1 [input] (solve input identity))
(defn part2 [input] (solve input three-measurement-sliding-window))