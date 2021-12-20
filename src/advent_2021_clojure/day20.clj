(ns advent-2021-clojure.day20
  (:require
    [advent-2021-clojure.point :as point]
    [advent-2021-clojure.utils :as utils]))

(def dark-pixel 0)
(def light-pixel 1)

(defn parse-input [input]
  (let [pixel-map {\. dark-pixel, \# light-pixel}
        [alg image] (utils/split-blank-line input)]
    [(mapv pixel-map alg)
     (reduce (fn [m [coord c]] (assoc m coord (pixel-map c)))
             {}
             (point/parse-to-char-coords image))]))

(defn border-seq [alg]
  (let [block-of {dark-pixel 0, light-pixel 511}]
    (iterate #(-> % block-of alg) 0)))

(defn next-value-at [alg border image coords]
  (->> (point/surrounding true coords)
       (map #(or (image %) border))
       (apply str)
       utils/parse-binary
       alg))

(defn migrate-image [alg border image]
  (reduce-kv (fn [m coord _] (assoc m coord (next-value-at alg border image coord)))
             {} image))

(defn expand-image [image border]
  (let [min-max (juxt (partial apply min) (partial apply max))
        [min-x max-x] (min-max (map ffirst image))
        [min-y max-y] (min-max (map (comp second first) image))]
    (reduce #(assoc %1 %2 border)
            image
            (point/perimeter-points [(dec min-x) (dec min-y)]
                                    [(inc max-x) (inc max-y)]))))

(defn image-seq
  ([alg image] (image-seq alg image (border-seq alg)))
  ([alg image borders] (let [border (first borders)
                             image' (->> (expand-image image border)
                                         (migrate-image alg border))]
                         (lazy-seq (cons image' (image-seq alg image' (rest borders)))))))

(defn solve [input enhance-count]
  (->> (parse-input input)
       (apply image-seq)
       (drop (dec enhance-count))
       first
       (filter #(= light-pixel (second %)))
       count))

(defn part1 [input] (solve input 2))
(defn part2 [input] (solve input 50))
