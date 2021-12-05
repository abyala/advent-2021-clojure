(ns advent-2021-clojure.day04-all-solo
  (:require [advent-2021-clojure.day04 :as day04]))

(defn solo-results [game]
  (->> (iterate day04/take-turn game)
       (keep-indexed (fn [idx g] (when-let [score (day04/final-score g)]
                                   {:turns idx :score score})))
       first))

(defn solve-all [game]
  (let [solo-games (map #(assoc game :boards [%]) (:boards game))]
    (->> solo-games
         (map solo-results)
         (sort-by :turns))))

(defn part1 [input] (-> input day04/parse-game solve-all first :score))
(defn part2 [input] (-> input day04/parse-game solve-all last :score))