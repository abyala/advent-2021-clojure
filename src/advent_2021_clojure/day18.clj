(ns advent-2021-clojure.day18
  (:require [clojure.string :as str]))

(defn parse-trees [input] (map read-string (str/split-lines input)))

(defn explode-pos
  ([tree] (explode-pos tree []))
  ([tree pos] (if (>= (count pos) 4)
                pos
                (->> (get-in tree pos)
                     (keep-indexed (fn [idx leaf] (when (vector? leaf)
                                                    (explode-pos tree (conj pos idx)))))
                     first))))

(defn regular-numbers
  ([tree] (regular-numbers tree []))
  ([tree pos] (->> (get-in tree pos)
                   (map-indexed (fn [idx leaf] (let [leaf-pos (conj pos idx)]
                                                 (if (number? leaf)
                                                   [[leaf-pos leaf]]
                                                   (regular-numbers tree leaf-pos)))))
                   (apply concat))))

(defn regular-number-positions [tree] (->> tree regular-numbers (map first)))

(defn pos-before-and-after [coll v]
  (->> (concat [nil] coll [nil])
       (partition 3 1)
       (keep (fn [[a b c]] (when (= b v) [a c])))
       first))

(defn add-if-present [tree pos v]
  (if pos (update-in tree pos + v) tree))

(defn explode-tree [tree]
  (when-some [pos (explode-pos tree)]
    (let [[a b] (get-in tree pos)
          zeroed-out (assoc-in tree pos 0)
          [left-pos right-pos] (-> (regular-number-positions zeroed-out)
                                   (pos-before-and-after pos))]
      (-> zeroed-out
          (add-if-present left-pos a)
          (add-if-present right-pos b)))))

(defn split-pos [tree] (->> (regular-numbers tree)
                            (keep (fn [[p v]] (when (>= v 10) p)))
                            first))

(defn split-val [n] (let [a (quot n 2)
                          b (- n a)]
                      [a b]))

(defn split-tree [tree]
  (when-some [pos (split-pos tree)]
    (update-in tree pos split-val)))

(defn reduce-tree [tree]
  (->> tree
       (iterate (partial (some-fn explode-tree split-tree)))
       (take-while some?)
       last))

(defn add-trees [trees]
  (reduce #(reduce-tree (vector %1 %2)) trees))

(defn magnitude [tree]
  (->> (mapv #(if (number? %) % (magnitude %)) tree)
       (mapv * [3 2])
       (apply +)))

(defn part1 [input]
  (->> input parse-trees add-trees magnitude))

(defn part2 [input]
  (let [trees (parse-trees input)]
    (->> (for [t0 trees, t1 trees, :when (not= t0 t1)] [t0 t1])
         (map (comp magnitude add-trees))
         (apply max))))