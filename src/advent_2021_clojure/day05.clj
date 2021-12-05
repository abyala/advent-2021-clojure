(ns advent-2021-clojure.day05
  (:require
    [advent-2021-clojure.point :as p]
    [advent-2021-clojure.utils :refer [parse-int]]
    [clojure.string :as str]))

(defn parse-line [line]
  (let [[x1 y1 x2 y2] (->> (re-matches #"(\d+),(\d+) -> (\d+),(\d+)" line)
                           rest
                           (map parse-int))]
    [[x1 y1] [x2 y2]]))

(defn parse-vents [input] (->> input str/split-lines (map parse-line)))

(defn solve [input pred]
  (->> (parse-vents input)
       (filter pred)
       (map p/inclusive-line-between)
       (apply concat)
       frequencies
       (filter #(> (val %) 1))
       count))

(defn part1 [input] (solve input (some-fn p/horizontal-line? p/vertical-line?)))
(defn part2 [input] (solve input identity))
