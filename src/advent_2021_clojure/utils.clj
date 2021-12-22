(ns advent-2021-clojure.utils
  (:require [clojure.string :as str]))

(defn parse-int [v] (if (char? v)
                      (Character/digit ^char v 10)
                      (Integer/parseInt v)))

(defn parse-binary [s] (Integer/parseInt s 2))
(defn binary-to-decimal [s] (Long/parseLong s 2))

(defn split-blank-line
  "Given an input string, returns a sequence of sub-strings, separated by a completely
  blank string. This function preserves any newlines between blank lines, and it filters
  out Windows' \"\r\" characters."
  [input]
  (-> (str/replace input "\r" "")
      (str/split #"\n\n")))

(defn abs [^long n] (Math/abs n))

(defn summation [n]
  (-> (* n (inc n))
      (/ 2)))

(defn char->int [c] (- (int c) 48))

(defn update-values
  "Thank you to Jay Fields' post for this awesome way to apply a function
  to every element of a map.
  http://blog.jayfields.com/2011/08/clojure-apply-function-to-each-value-of.html"
  [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn lower-case? [s] (every? #(Character/isLowerCase ^char %) s))
(defn upper-case? [s] (every? #(Character/isUpperCase ^char %) s))

(defn update-add [m k v] (update m k #(+ (or % 0) v)))