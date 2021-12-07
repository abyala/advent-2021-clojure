(ns advent-2021-clojure.utils
  (:require [clojure.string :as str]))

(defn parse-int [s] (Integer/parseInt s))
(defn parse-binary [s] (Integer/parseInt s 2))

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