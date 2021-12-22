(ns advent-2021-clojure.day21
  (:require [advent-2021-clojure.utils :as u]))

(defn new-player [start] {:pos start, :score 0})
(defn new-game [players] {:players (vec players), :next-player 0})

(defn board-pos [n] (if (> n 10) (board-pos (- n 10)) n))
(defn move-pos [roll pos] (board-pos (+ roll pos)))
(defn swap-players [n] (- 1 n))

(def deterministic-die {:values (cycle (range 1 101)), :num-rolls 0})
(defn roll-die [die n]
  (let [{:keys [values]} die
        sum (apply + (take n values))]
    [(-> die
         (update :values #(drop n %))
         (update :num-rolls + n))
     sum]))

(defn move-player [game roll]
  (let [{:keys [players next-player]} game
        player (players next-player)
        moved (update player :pos (partial move-pos roll))
        scored (update moved :score + (:pos moved))]
    (-> game
        (assoc-in [:players next-player] scored)
        (update :next-player swap-players))))

(defn take-turn [game]
  (let [[rolled-die sum] (-> game :die (roll-die 3))]
    (-> game
        (move-player sum)
        (assoc :die rolled-die))))

(defn winner [game target-score]
  (->> (:players game)
       (keep-indexed (fn [idx {:keys [score]}] (when (>= score target-score) idx)))
       first))

(defn play-until [game target-score]
  (->> (iterate take-turn game)
       (filter #(winner % target-score))
       first))

(defn final-score [game]
  (let [{:keys [die players next-player]} game]
    (* (:num-rolls die)
       (:score (get players next-player)))))

(defn part1 [player1 player2]
  (-> (new-game (map new-player [player1 player2]))
      (assoc :die deterministic-die)
      (play-until 1000)
      final-score))

(def dirac-rolls {3 1, 4 3, 5 6, 6 7, 7 6, 8 3, 9 1})

(def game-option-sorter
  (letfn [(game-compare [g] (let [{:keys [players next-player]} g
                                  [{pos1 :pos, score1 :score} {pos2 :pos, score2 :score}] players]
                              [(+ score1 score2) score1 score2 next-player pos1 pos2]))]
    (fn [g1 g2] (compare (game-compare g1) (game-compare g2)))))

(defn roll-dirac-dice [game]
  (reduce (fn [acc [roll n]]
            (let [next-game (move-player game roll)]
              (u/update-add acc next-game n)))
          {}
          dirac-rolls))

(defn part2 [player1 player2]
  (let [target 21
        initial-game (new-game (map new-player [player1 player2]))]
    (loop [game-options (sorted-set-by game-option-sorter initial-game), universes {initial-game 1}]
      (if-let [game (first game-options)]
        (let [paths-to-game (universes game)
              rolled-universes (roll-dirac-dice game)
              next-game-options (->> (keys rolled-universes)
                                     (remove #(winner % target))
                                     (apply conj (disj game-options game)))
              next-universes (reduce-kv (fn [m k v] (u/update-add m k (* v paths-to-game)))
                                        (dissoc universes game)
                                        rolled-universes)]
          (recur next-game-options next-universes))
        (->> universes
             (map (fn [[game n]] [(winner game target) n]))
             (group-by first)
             (map (comp #(apply + (map second %)) second))
             (apply max))))))