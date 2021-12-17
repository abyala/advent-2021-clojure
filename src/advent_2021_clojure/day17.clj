(ns advent-2021-clojure.day17
  (:require [advent-2021-clojure.utils :refer [parse-int]]))

(defn parse-input [input]
  (->> input
       (re-seq #"target area: x=(.*)\.\.(.*), y=(.*)\.\.(.*)")
       first rest (map parse-int) vec))

(defn progress [target point]
  {:pre [(every? neg-int? (drop 2 target))
         (< (target 2) (target 3))]}
  (let [[tx1 tx2 ty1 ty2] target
        [px py] point]
    (cond
      (and (<= tx1 px tx2) (<= ty1 py ty2)) :hit
      (or (< tx2 px) (< py ty1)) :miss
      :else :approaching)))

(defn trajectory-from-origin [[dx0 dy0]]
  (->> [0 0 dx0 dy0]
       (iterate (fn [[x y dx dy]] [(+ x dx)
                                   (+ y dy)
                                   (max (dec dx) 0)
                                   (dec dy)]))
       (map (fn [[x y]] [x y]))))

(defn hits-target? [target trajectory]
  (->> (map #(progress target %) trajectory)
       (drop-while #(= % :approaching))
       (first)
       (= :hit)))

(defn x-values [dx] (->> (trajectory-from-origin [dx 0])
                         (map first)
                         (partition 2 1)
                         (take-while #(apply < %))
                         (map second)))

(defn y-values [dy] (->> (trajectory-from-origin [0 dy])
                         (map second)
                         rest))

(defn possible-dx0 [target]
  (let [[min-x max-x] target
        all-target-x (set (range min-x (inc max-x)))]
    (filter #(some all-target-x (x-values %))
            (range 1 (inc max-x)))))

(defn possible-dy0 [target]
  (let [[_ _ min-y max-y] target
        all-target-y (set (range min-y (inc max-y)))]
    (->> (range min-y (- min-y))
         (filter (fn [dy]
                   (let [ys (take-while #(<= min-y %) (y-values dy))]
                     (some all-target-y ys)))))))

(defn hit-trajectories [target]
  (let [all-x (possible-dx0 target)
        all-y (possible-dy0 target)
        all-velocities (for [x all-x, y all-y] [x y])]
    (->> (map trajectory-from-origin all-velocities)
         (filter #(hits-target? target %)))))

(defn apex [trajectory]
  (reduce (fn [acc [_ y]] (if (> y acc) y (reduced acc)))
          Long/MIN_VALUE
          trajectory))

(defn part1 [input] (->> input parse-input hit-trajectories (map apex) (apply max)))
(defn part2 [input] (->> input parse-input hit-trajectories count))
