(ns advent-2021-clojure.day25
  (:require [advent-2021-clojure.point :as point]))

(defn parse-map [input]
  (let [points (point/parse-to-char-coords input)
        max-x (apply max (map ffirst points))
        max-y (apply max (map (comp second first) points))]
    (reduce (fn [acc [coords c]] (case c
                                   \> (update acc :right conj coords)
                                   \v (update acc :down conj coords)
                                   \. acc))
            {:right #{}, :down #{}, :max-x max-x, :max-y max-y}
            points)))

(defmulti target-from (fn [_ _ dir] dir))
(defmethod target-from :right [m [x y] _] (if (>= x (:max-x m)) [0 y] [(inc x) y]))
(defmethod target-from :down [m [x y] _] (if (>= y (:max-y m)) [x 0] [x (inc y)]))

(defn move-all-in-direction [m dir]
  (reduce (fn [acc coords]
            (let [target (target-from m coords dir)]
              (if ((some-fn (:right m) (:down m)) target)
                acc
                (-> acc
                    (update dir conj target)
                    (update dir disj coords)))))
          m
          (dir m)))

(defn take-turn [m]
  (reduce move-all-in-direction m [:right :down]))

(defn sea-cucumber-seq [m]
  (let [next-map (take-turn m)]
    (if (= m next-map)
      (lazy-seq [m])
      (lazy-seq (cons m (sea-cucumber-seq next-map))))))

(defn part1 [input] (->> input parse-map sea-cucumber-seq count))
