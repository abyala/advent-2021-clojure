(ns advent-2021-clojure.day08-joepittsy
  (:require
    [advent-2021-clojure.day08 :refer [parse-input]]
    [advent-2021-clojure.utils :refer [parse-int]]))

; Assume we know the digits for 1 and 4 because they are obvious
; {[digit-length number-of-segments-in-one number-of-segments-in-four] digit}
(def digit-finder {[6 2 3] 0
                   [2 2 2] 1
                   [5 1 2] 2
                   [5 2 3] 3
                   [4 2 4] 4
                   [5 1 3] 5
                   [6 1 3] 6
                   [3 2 2] 7
                   [7 2 4] 8
                   [6 2 4] 9})

(defn digits [patterns outputs]
  (let [one (first (filter #(= 2 (count %)) patterns))
        four (first (filter #(= 4 (count %)) patterns))
        digit-keys (juxt count
                         (comp count (partial set/intersection one))
                         (comp count (partial set/intersection four)))]
    (->> outputs
         (map (comp digit-finder digit-keys))
         (apply str)
         parse-int)))

(defn part2 [input]
  (->> (parse-input input)
       (map (partial apply digits))
       (apply +)))