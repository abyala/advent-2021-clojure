(ns advent-2021-clojure.day10
  (:require [clojure.string :as str]))

(def close-delimiters {\( \), \[ \], \{ \}, \< \>})
(def delimiters (set (keys close-delimiters)))
(def corrupt-char-points {\) 3, \] 57, \} 1197, \> 25137})
(def autocomplete-char-points {\) 1, \] 2, \} 3, \> 4})

(defn process-line [line]
  (loop [[c & xc] line, [s & xs :as stack] ()]
    (if c
      (cond (delimiters c) (recur xc (conj stack (close-delimiters c))) ;push
            (= c s)        (recur xc xs)                                ; pop
            :else          [:corrupt c])
      [:incomplete stack])))

(defn- error-chars [error line]
  (let [[status c] (process-line line)]
    (when (= status error) c)))

(defn first-corrupt-char [line] (error-chars :corrupt line))
(defn missing-chars [line]      (error-chars :incomplete line))

(defn part1 [input]
  (->> (str/split-lines input)
       (keep first-corrupt-char)
       (map corrupt-char-points)
       (apply +)))

(defn incomplete-char-bounty [chars]
  (reduce (fn [acc c] (+ (* acc 5)
                         (autocomplete-char-points c)))
          0
          chars))

(defn middle [coll]
  (nth coll (/ (count coll) 2)))

(defn part2 [input]
  (->> (str/split-lines input)
       (keep missing-chars)
       (map incomplete-char-bounty)
       sort
       middle))