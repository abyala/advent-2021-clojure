(ns advent-2021-clojure.day06
  (:require [advent-2021-clojure.utils :refer [parse-int]]))

(def delivery-timer 0)
(def post-delivery-timer 6)
(def new-fish-timer 8)
(def no-fish {0 0, 1 0, 2 0, 3 0, 4 0, 5 0, 6 0, 7 0, 8 0})

(defn parse-fish [input]
  (->> (re-seq #"\d" input)
       (map parse-int)
       frequencies
       (merge no-fish)))

(defn next-timer [timer]
  (if (= timer delivery-timer) post-delivery-timer (dec timer)))

(defn next-generation [fish]
  (let [deliveries (fish delivery-timer)]
    (-> (reduce-kv (fn [m k v] (update m (next-timer k) + v)) no-fish fish)
        (assoc new-fish-timer deliveries))))

(defn nth-generation [n fish]
  (-> (iterate next-generation fish)
      (nth n)))

(defn solve [input num-days]
  (->> (parse-fish input)
       (nth-generation num-days)
       vals
       (apply +)))

(defn day1 [input] (solve input 80))
(defn day2 [input] (solve input 256))