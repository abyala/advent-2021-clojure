(ns advent-2021-clojure.day16
  (:require [advent-2021-clojure.utils :refer [parse-binary binary-to-decimal]]
            [clojure.string :as str]))

(defn literal-value? [n] (= 4 n))
(def total-length-type? zero?)

(defn left-pad-to-multiple [s mult c]
  (let [rem (mod (count s) mult)]
    (if (zero? rem) s (reduce #(str %2 %1) s (repeat (- mult rem) c)))))

(defn hex-packet-to-binary-string [hex-packet]
  (apply str (map #(-> (Character/digit ^char % 16)
                       (Integer/toBinaryString)
                       (left-pad-to-multiple 4 \0))
                  hex-packet)))

(defn literal-value-portion [s]
  (let [[ones [first-zero]] (split-with #(-> % first (= \1)) (partition 5 s))]
    (->> (concat ones first-zero) flatten (apply str))))

(defn padded-binary-to-decimal [s]
  (->> s (partition 5) (map rest) flatten (apply str) binary-to-decimal))

(defn parse-literal-packet [packet input]
  (let [portion (literal-value-portion input)]
    [(assoc packet :value (padded-binary-to-decimal portion)) (subs input (count portion))]))

(declare parse-sub-packet)

(defn parse-sub-packets [finished? packet input]
  (loop [p packet, remainder input]
    (if (finished? p remainder)
      [p remainder]
      (let [[sub-packet leftovers] (parse-sub-packet remainder)]
        (recur (update p :sub-packets conj sub-packet) leftovers)))))

(defn parse-total-length-operator [packet input]
  (let [sub-packet-length (parse-binary (subs input 0 15))
        sub-packet-payload (subs input 15 (+ 15 sub-packet-length))
        non-sub-packet-payload (subs input (+ 15 sub-packet-length))]
    [(first (parse-sub-packets (fn [_ remainder] (str/blank? remainder)) packet sub-packet-payload))
     non-sub-packet-payload]))

(defn parse-num-sub-packets-operator [packet input]
  (let [num-sub-packets (parse-binary (subs input 0 11))]
    (parse-sub-packets (fn [p _] (= num-sub-packets (-> p :sub-packets count))) packet (subs input 11))))

(defn parse-operator-packet [packet input]
  (let [length-type-id (binary-to-decimal (subs input 0 1))
        remainder (subs input 1)
        op-packet (assoc packet :sub-packets [])]
    (if (total-length-type? length-type-id)
      (parse-total-length-operator op-packet remainder)
      (parse-num-sub-packets-operator op-packet remainder))))

(defn parse-sub-packet [input]
  (let [version (binary-to-decimal (subs input 0 3))
        packet-type (binary-to-decimal (subs input 3 6))
        packet {:version version, :packet-type packet-type}
        payload (subs input 6)]
    (if (literal-value? packet-type)
      (parse-literal-packet packet payload)
      (parse-operator-packet packet payload))))

(defn parse-packet [input] (first (parse-sub-packet input)))

(defn add-versions [packet]
  (reduce #(+ %1 (add-versions %2))
          (:version packet)
          (:sub-packets packet)))

(defn part1 [input]
  (-> input hex-packet-to-binary-string parse-packet add-versions))

(defn value-of [packet]
  (or (:value packet)
      (let [[sub-a sub-b :as sub-values] (map value-of (:sub-packets packet))]
        (case (:packet-type packet)
          0 (apply + sub-values)
          1 (apply * sub-values)
          2 (apply min sub-values)
          3 (apply max sub-values)
          5 (if (> sub-a sub-b) 1 0)
          6 (if (< sub-a sub-b) 1 0)
          7 (if (= sub-a sub-b) 1 0)))))

(defn part2 [input]
  (-> input hex-packet-to-binary-string parse-packet value-of))