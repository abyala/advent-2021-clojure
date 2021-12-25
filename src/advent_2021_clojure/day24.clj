(ns advent-2021-clojure.day24
  (:require [clojure.string :as str]))

; I am not proud of this code, since I was not even slightly interested in the puzzle.
; Please read the original source code that I simply ported into Clojure.
; https://todd.ginsberg.com/post/advent-of-code/2021/day24/

(defn parse-digit-instruction [lines]
  (letfn [(last-number [n] (->> (lines n) (re-seq #"-?\d+") last read-string))]
    (map last-number [4 5 15])))

(defn parse-input [input]
  (->> (str/split-lines input)
       (partition 18)
       (map (comp parse-digit-instruction vec))))

(defn magic-function [[a b c] z w]
  (if (not= (-> z (mod 26) (+ b)) w)
    (-> z (quot a) (* 26) (+ w c))
    (quot z a)))

(defn run-instruction-digit [z-values-this-round [a b c :as instruction] z min-max digit]
  (let [new-value-for-z (magic-function instruction z digit)]
    (if (or (= a 1)
            (and (= a 26) (< new-value-for-z z)))
      (assoc z-values-this-round new-value-for-z [(min (get-in z-values-this-round [new-value-for-z 0] Long/MAX_VALUE)
                                                       (-> (first min-max) (* 10) (+ digit)))
                                                  (max (get-in z-values-this-round [new-value-for-z 1] Long/MIN_VALUE)
                                                       (-> (second min-max) (* 10) (+ digit)))])
      z-values-this-round)))

(defn run-instruction [z-values instruction]
  (reduce (fn [z-values-this-round [z min-max]]
            (reduce (fn [zvtr digit] (run-instruction-digit zvtr instruction z min-max digit))
                    z-values-this-round
                    (range 1 10)))
          {}
          z-values))

(defn solve [input]
  (let [result-map (reduce (fn [z-values instruction] (run-instruction z-values instruction))
                           {0 [0 0]}
                           (parse-input input))]
    (get result-map 0)))

(defn part1 [input] (-> input solve second))
(defn part2 [input] (-> input solve first))
