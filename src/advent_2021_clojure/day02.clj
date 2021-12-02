(ns advent-2021-clojure.day02
  (:require [clojure.string :as str]))

(def initial-submarine {:pos 0 :depth 0 :aim 0})

(defn parse-instruction [s]
  (let [[a b] (str/split s #" ")]
    [(keyword a) (Integer/parseInt b)]))

(defn final-position [{:keys [pos depth]}]
  (* pos depth))

(defn create-mover [forward-fn down-fn up-fn]
  (fn [submarine [dir amount]]
    (let [op (dir {:forward forward-fn, :down down-fn, :up up-fn})]
      (op submarine amount))))

(def part1-mover (create-mover (fn [submarine amount] (update submarine :pos + amount))
                               (fn [submarine amount] (update submarine :depth + amount))
                               (fn [submarine amount] (update submarine :depth - amount))))
(def part2-mover (create-mover (fn [{aim :aim :as submarine} amount] (-> (update submarine :pos + amount)
                                                                         (update :depth + (* aim amount))))
                               (fn [submarine amount] (update submarine :aim + amount))
                               (fn [submarine amount] (update submarine :aim - amount))))

(defn solve [mover input]
  (->> (str/split-lines input)
       (map parse-instruction)
       (reduce mover initial-submarine)
       final-position))

(defn part1 [input] (solve part1-mover input))
(defn part2 [input] (solve part2-mover input))