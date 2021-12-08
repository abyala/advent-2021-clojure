(ns advent-2021-clojure.day08
  (:require [advent-2021-clojure.utils :as utils :refer [parse-int]]
            [clojure.set :as set]
            [clojure.string :as str]))

(defn parse-component [component]
  (map set (str/split component #" ")))

(defn parse-line [line]
  (->> (str/split line #"\|")
       (map (comp parse-component str/trim))))

(defn parse-input [input]
  (map parse-line (str/split-lines input)))

(def unique-lengths #{2 3 4 7})
(defn part1 [input]
  (->> (parse-input input)
       (mapcat second)
       (filter #(-> % count unique-lengths))
       count))

(defn identify-signals [signal-patterns]
  (letfn [(only-subset [s coll] (first (filter (partial set/subset? s) coll)))
          (only-superset [s coll] (first (filter (partial set/superset? s) coll)))
          (only-not-subset [s coll] (first (remove (partial set/subset? s) coll)))
          (leftover [vals coll] (first (remove vals coll)))]
    (let [patterns (group-by count signal-patterns)
          ; Unique lengths
          one (first (patterns 2))
          four (first (patterns 4))
          seven (first (patterns 3))
          eight (first (patterns 7))

          ; 0, 6, and 9 all have length of 6
          zero-six-nine (patterns 6)
          six (only-not-subset one zero-six-nine)
          nine (only-subset four zero-six-nine)
          zero (leftover #{six nine} zero-six-nine)

          ; 2, 3, and 5 all have length of 5
          two-three-five (patterns 5)
          five (only-superset six two-three-five)
          three (only-subset one two-three-five)
          two (leftover #{three five} two-three-five)]
      {zero 0, one 1, two 2, three 3, four 4, five 5, six 6, seven 7, eight 8, nine 9})))

(defn find-digits [signals outputs]
  (->> (map signals outputs)
       (apply str)
       (parse-int)))

(defn part2 [input]
  (->> (parse-input input)
       (map (fn [[patterns outputs]] (-> (identify-signals patterns)
                                         (find-digits outputs))))
       (apply +)))