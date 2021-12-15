(ns advent-2021-clojure.day15
  (:require [advent-2021-clojure.utils :as u :refer [parse-int]]
            [advent-2021-clojure.point :as point]
            [advent-2021-clojure.sorted-value-map :as svm]))

;; Options to try:
;; 1. Use an a-star algorithm instead of just the lowest cost
;; 2. Create some sort of a value heap, based on a sorted map

(defn parse-cave [input]
  (reduce (fn [acc [point c]] (assoc acc point (parse-int (str c))))
          {}
          (point/parse-to-char-coords input)))

(def risk-offsets (vec (cons 0 (take 26 (cycle (range 1 10))))))
(defn cave-risk-wrap [n] (risk-offsets n))

(defn multiply-cave [cave n]
  (let [length (inc (apply max (map ffirst cave)))]
    (apply merge (for [grid-x (range 0 n)
                       :let [x-offset (* grid-x length)]
                       grid-y (range 0 n)
                       :let [y-offset (* grid-y length)
                             n-offset (+ grid-x grid-y)]]
                   (reduce (fn [acc [[x y] n]] (assoc acc [(+ x x-offset) (+ y y-offset)]
                                                          (cave-risk-wrap (+ n n-offset))))
                           {}
                           cave)))))

(defn shortest-path [cave]
  (let [cave-length (apply max (map ffirst cave))
        target [cave-length cave-length]]
    (loop [candidates (svm/assoc svm/empty-map [0 0] 0), seen #{}]
      (let [[point cost] (svm/first candidates)
            next-options (->> (point/neighbors point)
                              (filter cave)
                              (remove seen)
                              (map #(vector % (+ cost (cave %))))
                              (into {}))]
        (if (= point target)
          cost
          (recur (svm/merge-with min (svm/dissoc candidates point) next-options)
                 (conj seen point)))))))

(defn solve [num-multiples input]
  (-> input parse-cave (multiply-cave num-multiples) shortest-path))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 5 input))
