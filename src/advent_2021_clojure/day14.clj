(ns advent-2021-clojure.day14
  (:require
    [advent-2021-clojure.utils :as u :refer [parse-int]]
    [clojure.string :as str]))

(defn parse-input [input]
  (let [[template rules] (u/split-blank-line input)]
    [template
     (->> (re-seq #"\w+" rules) (partition 2) (map vec) (into {}))]))

(defn update-add [m k n] (update m k #(+ (or % 0) n)))

(defn apply-rules [rules freqs]
  (reduce (fn [acc [[a b :as word] n]] (let [c (rules word)]
                                         (-> acc
                                             (update-add (str a c) n)
                                             (update-add (str c b) n))))
          {} freqs))

(defn score [initial-template freqs]
  (let [char-freqs (update (reduce (fn [acc [[a] n]] (update-add acc a n))
                                         {} freqs)
                                 (last initial-template) inc)
        sorted-instances (sort-by - (vals char-freqs))]
    (apply - ((juxt first last) sorted-instances))))

(defn solve [input step]
  (let [[template rules] (parse-input input)
        initial-freqs (->> template (partition 2 1) (map (partial apply str)) frequencies)]
    (->> (iterate (partial apply-rules rules) initial-freqs)
         (drop step)
         first
         (score template))))

(defn part1 [input] (solve input 10))
(defn part2 [input] (solve input 40))
