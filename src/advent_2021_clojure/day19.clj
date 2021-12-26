(ns advent-2021-clojure.day19
  (:require
    [advent-2021-clojure.utils :as utils :refer [parse-int]]
    [clojure.set :as set]
    [clojure.string :as str]))

(defn parse-scanner [input]
  (let [[header & beacons] (str/split-lines input)]
    {:id       (->> header (re-seq #"\d+") first parse-int)
     :beacons  (->> beacons
                    (map (fn [line] (mapv parse-int (str/split line #","))))
                    set)
     :scanners #{[0 0 0]}}))

(defn parse-input [input]
  (reduce #(assoc %1 (:id %2) %2)
          {}
          (map parse-scanner (utils/split-blank-line input))))

(def orientation-fns [; Face straight
                      identity
                      (fn [[x y z]] [(- z) y x])
                      (fn [[x y z]] [(- x) y (- z)])
                      (fn [[x y z]] [z y (- x)])

                      ; Face right
                      (fn [[x y z]] [(- y) x z])
                      (fn [[x y z]] [(- z) x (- y)])
                      (fn [[x y z]] [y x (- z)])
                      (fn [[x y z]] [z x y])

                      ; Face behind
                      (fn [[x y z]] [(- x) (- y) z])
                      (fn [[x y z]] [(- z) (- y) (- x)])
                      (fn [[x y z]] [x (- y) (- z)])
                      (fn [[x y z]] [z (- y) x])

                      ; Face left
                      (fn [[x y z]] [y (- x) z])
                      (fn [[x y z]] [(- z) (- x) y])
                      (fn [[x y z]] [(- y) (- x) (- z)])
                      (fn [[x y z]] [z (- x) (- y)])

                      ; Face up
                      (fn [[x y z]] [x z (- y)])
                      (fn [[x y z]] [y z x])
                      (fn [[x y z]] [(- x) z y])
                      (fn [[x y z]] [(- y) z (- x)])

                      ; Face down
                      (fn [[x y z]] [x (- z) y])
                      (fn [[x y z]] [(- y) (- z) x])
                      (fn [[x y z]] [(- x) (- z) (- y)])
                      (fn [[x y z]] [y (- z) (- x)])])

(defn path-to [from to] (mapv - to from))
(defn follow-path [point path] (mapv + point path))

(defn combine-beacons [beacons0 beacons1]
  (->> (for [b0 (drop 11 beacons0) #_beacons0, b1 beacons1] (path-to b1 b0))
       (filter (fn [path] (>= (->> (map #(follow-path % path) beacons1)
                                   set
                                   (set/intersection beacons0)
                                   count)
                              12)))
       first))

(defn combine-scanners [scanner0 scanner1]
  (let [[beacons0 beacons1] (map :beacons [scanner0 scanner1])]
    (first (keep (fn [f]
                   (let [beacons1' (map f beacons1)]
                     (when-some [path (combine-beacons beacons0 beacons1')]
                       (-> scanner0
                           (update :beacons set/union (set (map #(follow-path % path) beacons1')))
                           (update :scanners (fn [s] (apply conj s (map #(follow-path (f %) path) (:scanners scanner1)))))))))
                 orientation-fns))))

(defn combine-all-scanners [scanners]
  (println "Examining" (count scanners) "scanners")
  (if (= (count scanners) 1)
    (scanners 0)
    (recur (reduce (fn [acc [id0 id1]] (if-not (and (acc id0) (acc id1))
                                         acc
                                         (if-some [scanner' (combine-scanners (acc id0) (acc id1))]
                                           (do (println "Combining" id0 "with" id1 "from keys" (keys acc))
                                               (-> acc
                                                   (dissoc id1)
                                                   (assoc id0 scanner')))
                                           acc)))
                   scanners
                   (for [id0 (keys scanners), id1 (keys scanners), :when (> id1 id0)] [id0 id1])))))

(defn manhattan-distance [p1 p2]
  (->> (path-to p1 p2)
       (map utils/abs)
       (apply +)))

(defn greatest-distances [s]
  (->> (for [v1 s, v2 s, :when (not= v1 v2)] [v1 v2])
       (map (partial apply manhattan-distance))
       (apply max)))

(defn part1 [input] (->> input parse-input combine-all-scanners :beacons count))
(defn part2 [input] (->> input parse-input combine-all-scanners :scanners greatest-distances))
