(ns advent-2021-clojure.day13
  (:require
    [advent-2021-clojure.utils :as utils :refer [parse-int]]
    [clojure.string :as str]))

(defn parse-instruction [line]
  (let [[_ axis amount] (re-matches #"fold along ([xy])=(\d+)" line)]
    [(keyword axis) (parse-int amount)]))

(defn parse-paper [input]
  (let [[dots folds] (utils/split-blank-line input)]
    [(->> (str/split-lines dots)
          (map #(mapv parse-int (str/split % #",")))
          set)
     (map parse-instruction (str/split-lines folds))]))

(defn fold [dots [dir fold-line]]
  (let [coord-idx ({:x 0 :y 1} dir)]
    (->> dots
         (map (fn [dot]
                 (let [v (dot coord-idx)]
                   (if (<= v fold-line)
                     dot
                     (update dot coord-idx (partial - (* 2 fold-line)))))))
         set)))

(defn part1 [input]
  (let [[dots [instruction]] (parse-paper input)]
    (count (fold dots instruction))))

(defn print-dots [dots]
  (let [min-x (apply min (map first dots))
        min-y (apply min (map second dots))
        max-x (apply max (map first dots))
        max-y (apply max (map second dots))]
    (run! println (map (fn [y]
                         (apply str (map #(if (dots [% y]) \# \space)
                                         (range min-x (inc max-x)))))
                       (range min-y (inc max-y))))))

(defn part2 [input]
  (let [[dots instructions] (parse-paper input)]
    (print-dots (reduce fold dots instructions))))