(ns advent-2021-clojure.day01-windowed
  (:require [clojure.string :as str]
            [advent-2021-clojure.utils :refer [parse-int]]))

(defn parse-measurements [input]
  (->> (str/split-lines input)
       (map parse-int)))

(defn solve [input window-size]
  (->> (parse-measurements input)
       (partition window-size 1)
       (map (juxt first last))
       (filter (partial apply <))
       count))

(defn part1 [input] (solve input 2))
(defn part2 [input] (solve input 4))