(ns advent-2021-clojure.day22
  (:require [clojure.string :as str]))

(def dimensions [:x :y :z])

(defn parse-instruction [line]
  (let [[instruction dim-str] (str/split line #" ")
        [x0 x1 y0 y1 z0 z1] (map read-string (re-seq #"\-?\d+" dim-str))]
    [(keyword instruction) {:x [x0 x1], :y [y0 y1], :z [z0 z1]}]))

(defn within-initialization-area? [cuboid]
  (letfn [(dim-ok? [dim] (let [[v0 v1] (dim cuboid)]
                           (and (>= v0 -50) (<= v1 50))))]
    (every? dim-ok? dimensions)))

(defn overlap?
  ([cuboid1 cuboid2] (every? (partial overlap? cuboid1 cuboid2) dimensions))
  ([cuboid1 cuboid2 dim] (let [[v0a v1a] (dim cuboid1)
                               [v0b v1b] (dim cuboid2)]
                           (and (<= v0a v1b) (<= v0b v1a)))))

(defn split-on-dimension [cuboid1 cuboid2 dim]
  (if-not (overlap? cuboid1 cuboid2 dim)
    {:safe [cuboid1]}
    (let [[v0a v1a] (dim cuboid1)
          [v0b v1b] (dim cuboid2)
          overlap0 (max v0a v0b)
          overlap1 (min v1a v1b)
          safe-regions (filter (partial apply <=) [[v0a (dec overlap0)] [(inc overlap1) v1a]])
          overlap-region [overlap0 overlap1]]
      {:safe   (map #(assoc cuboid1 dim %) safe-regions)
       :unsafe [(assoc cuboid1 dim overlap-region)]})))

(defn remove-overlaps [cuboid1 cuboid2]
  (if-not (overlap? cuboid1 cuboid2)
    [cuboid1]
    (first (reduce (fn [[acc-safe acc-unsafe] dim]
                     (let [{:keys [safe unsafe]}
                           (->> acc-unsafe
                                (map #(split-on-dimension % cuboid2 dim))
                                (apply merge-with conj))]
                       [(apply conj acc-safe safe) unsafe]))
                   [() [cuboid1]]
                   dimensions))))

(defn remove-all-overlaps [cuboids new-cuboid]
  (reduce (fn [acc c] (apply conj acc (remove-overlaps c new-cuboid)))
          ()
          cuboids))

(defn apply-instruction [cuboids [op new-cuboid]]
  (let [remaining (remove-all-overlaps cuboids new-cuboid)]
    (if (= op :on)
      (conj remaining new-cuboid)
      remaining)))

(defn apply-instructions [instructions]
  (reduce apply-instruction () instructions))

(defn cuboid-size [cuboid]
  (->> (map (fn [dim] (let [[v0 v1] (cuboid dim)]
                        (inc (- v1 v0)))) dimensions)
       (apply *)))

(defn solve [instruction-filter input]
  (->> (str/split-lines input)
       (map parse-instruction)
       (filter #(instruction-filter (second %)))
       apply-instructions
       (map cuboid-size)
       (apply +)))

(defn part1 [input] (solve within-initialization-area? input))
(defn part2 [input] (solve identity input))