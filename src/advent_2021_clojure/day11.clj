(ns advent-2021-clojure.day11
  (:require
    [advent-2021-clojure.point :as point]
    [advent-2021-clojure.utils :as utils]))

(defn parse-grid [input]
  (->> (point/parse-to-char-coords input)
       (reduce (fn [acc [p c]] (assoc acc p (utils/char->int c))) {})))

(defn flashes? [v] (> v 9))

(defn flashing-coordinates [grid]
  (keep (fn [[k v]] (when (flashes? v) k)) grid))

(defn increment-all [grid] (utils/update-values grid inc))

(defn cascade-flashes
  ([grid] (cascade-flashes grid #{}))
  ([grid flashed] (if-some [p (->> grid flashing-coordinates (remove flashed) first)]
                    (recur (->> (point/surrounding p)
                                (filter grid)
                                (reduce #(update %1 %2 inc) grid))
                           (conj flashed p))
                    grid)))

(defn reset-flashes [grid]
  (reduce #(assoc %1 %2 0) grid (flashing-coordinates grid)))

(defn take-turn [[grid _]]
  (let [grid' (-> grid increment-all cascade-flashes reset-flashes)
        num-flashed (->> grid' (map second) (filter zero?) count)]
    [grid' num-flashed]))

(defn octopus-flash-seq [input]
  (->> (iterate take-turn [(parse-grid input) 0])
       (map second)))

(defn part1 [input]
  (->> (octopus-flash-seq input)
       (take 101)
       (apply +)))

(defn part2 [input]
  (->> (octopus-flash-seq input)
       (keep-indexed #(when (= 100 %2) %1))
       first))
