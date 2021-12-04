(ns advent-2021-clojure.day04
  (:require
    [advent-2021-clojure.utils :refer [parse-int] :as utils]
    [clojure.set :as set]
    [clojure.string :as str]))

(defn parse-numbers-to-draw [line]
  (->> (str/split line #",")
       (map parse-int)))

(defn parse-board [lines]
  {:marked #{}
   :unmarked (->> (str/split-lines lines)
                  (map #(map parse-int (re-seq #"\d+" %)))
                  (map-indexed (fn [y nums]
                                 (map-indexed (fn [x n] [n [x y]]) nums)))
                  (apply concat)
                  (into {}))})

(defn parse-game [input]
  (let [[drawn & boards] (utils/split-blank-line input)]
    {:numbers-to-draw (parse-numbers-to-draw drawn)
     :numbers-drawn   ()
     :boards          (map parse-board boards)}))

(defn coords-of [board n] (get-in board [:unmarked n]))
(defn unmarked-values [board] (-> board :unmarked keys))

(def winning-combos (let [horizontal (for [y (range 5)] (map #(vector % y) (range 5)))
                          vertical (for [x (range 5)] (map #(vector x %) (range 5)))]
                      (concat horizontal vertical)))

(defn check-for-bingo [{:keys [marked] :as board}]
  (if (some (partial set/superset? marked) winning-combos)
    (assoc board :bingo? true)
    board))

(defn place-number [board n]
  (if-let [c (coords-of board n)]
    (-> board
        (update :marked conj c)
        (update :unmarked dissoc n)
        (check-for-bingo))
    board))

(defn take-turn [{:keys [numbers-to-draw boards] :as game}]
  (let [drawn (first numbers-to-draw)]
    (-> game
        (update :numbers-to-draw rest)
        (update :numbers-drawn conj drawn)
        (assoc :boards (map #(place-number % drawn) boards)))))

(defn final-score [{:keys [boards numbers-drawn] :as _game}]
  (when-some [winner (first (filter :bingo? boards))]
    (* (apply + (unmarked-values winner))
       (first numbers-drawn))))

(defn remove-winners [game]
  (update game :boards (partial remove :bingo?)))

(defn play-until-winner [game]
  (->> (iterate take-turn game)
       (keep final-score)
       first))

(defn play-until-solitaire [game]
  (->> (iterate (comp remove-winners take-turn) game)
       (drop-while #(> (count (:boards %)) 1))
       first))

(defn part1 [input] (-> input parse-game play-until-winner))
(defn part2 [input] (-> input parse-game play-until-solitaire play-until-winner))
