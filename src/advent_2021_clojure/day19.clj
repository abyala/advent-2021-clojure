(ns advent-2021-clojure.day19
  (:require
    [advent-2021-clojure.utils :as utils :refer [parse-int]]
    [clojure.string :as str]))

(defn parse-scanner [input]
  (let [[header & others] (str/split-lines input)]
    {:id (->> header (re-seq #"\d+") first parse-int)
     :beacons (map (fn [line] (mapv parse-int (str/split line #",")))
                   others)}))

(defn parse-input [input]
  (map parse-scanner (utils/split-blank-line input)))

(defn self-pairs [coll]
  (let [c (count coll)]
    (for [i (range c)
          :let [a (nth coll i)]
          coll-b (drop (inc i) coll)]
      [a coll-b])))

(defn three-d-distance [p1 p2]
  (mapv #(Math/abs ^long %) (mapv - p1 p2)))

(defn beacon-distances [scan]
  (map (fn [[p1 p2]] (three-d-distance p1 p2))) (:beacons scan))