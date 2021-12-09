(ns advent-2021-clojure.day09
  (:require
    [advent-2021-clojure.point :as point]
    [advent-2021-clojure.utils :as utils :refer [parse-int]]
    [clojure.string :as str]))

(defn parse-cave [input] (->> (str/split-lines input)
                              (mapv (partial mapv utils/char->int))))

(defn lowest-point? [cave point]
  (let [height (get-in cave point)]
    (->> (point/neighbors point)
         (keep (partial get-in cave))
         (every? (partial < height)))))

(defn all-coords [cave] (for [y (-> cave count range)
                              x (-> cave first count range)]
                          [y x]))

(defn lowest-points [cave]
  (filter (partial lowest-point? cave) (all-coords cave)))

(defn part1 [input]
  (let [cave (parse-cave input)]
    (->> (lowest-points cave)
         (map (partial get-in cave))
         (map inc)
         (apply +))))

(def max-height 9)
(defn high-point? [cave point] (= max-height (get-in cave point)))

(defn basin-around [cave lowest-point]
  (loop [candidates #{lowest-point}, found #{}]
    (if-some [point (first candidates)]
      (recur (reduce conj (rest candidates) (->> (point/neighbors point)
                                                 (filter (partial get-in cave))
                                                 (remove (some-fn found (partial high-point? cave)))))
             (conj found point))
      found)))

(defn part2 [input]
  (let [cave (parse-cave input)]
    (->> (lowest-points cave)
         (map (comp count (partial basin-around cave)))
         (sort-by -)
         (take 3)
         (apply *))))