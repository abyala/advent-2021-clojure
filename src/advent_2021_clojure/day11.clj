(ns advent-2021-clojure.day11
  (:require
    [advent-2021-clojure.point :as point]
    [advent-2021-clojure.utils :as utils]))

(defn parse-grid [input]
  (->> (point/parse-to-char-coords input)
       (reduce (fn [acc [p c]] (assoc acc p (utils/char->int c))) {})))

(defn ready-to-flash? [v] (> v 9))
(def flashed? zero?)

(defn- coordinates-where [f grid] (keep (fn [[k v]] (when (f v) k)) grid))
(defn coordinates-ready-to-flash [grid] (coordinates-where ready-to-flash? grid))
(defn coordinates-flashed [grid] (coordinates-where flashed? grid))

(defn increment-all [grid] (utils/update-values grid inc))

(defn cascade-flashes [grid]
  (if-some [p (->> grid coordinates-ready-to-flash first)]
    (recur (->> (point/surrounding p)
                (filter grid)
                (remove (comp flashed? grid))
                (reduce #(update %1 %2 inc) (assoc grid p 0))))
    grid))

(defn take-turn [grid] (-> grid increment-all cascade-flashes))

(defn octopus-flash-seq [input]
  (->> (parse-grid input)
       (iterate take-turn)
       (map (comp count coordinates-flashed))))

(defn part1 [input]
  (->> (octopus-flash-seq input)
       (take 101)
       (apply +)))

(defn part2 [input]
  (->> (octopus-flash-seq input)
       (keep-indexed #(when (= 100 %2) %1))
       first))