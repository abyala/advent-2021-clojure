(ns advent-2021-clojure.day12
  (:require
    [advent-2021-clojure.utils :as utils]
    [clojure.string :as str]))

(def start-cave "start")
(def end-cave "end")

(defn parse-connections [input]
  (let [every-path (->> (str/split-lines input)
                        (map #(str/split % #"-"))
                        (reduce (fn [acc [from to]] (-> acc
                                                        (update from conj to)
                                                        (update to conj from))) {}))]
    (utils/update-values every-path #(remove (partial = start-cave) %))))

(defn start-cave? [cave] (= start-cave cave))
(defn end-cave? [cave] (= end-cave cave))
(defn intermediate-cave? [cave] ((complement (some-fn start-cave? end-cave?)) cave))
(defn small-cave? [cave] ((every-pred intermediate-cave? utils/lower-case?) cave))
(defn big-cave? [cave] ((every-pred intermediate-cave? utils/upper-case?) cave))

(defn has-repeats? [seen] (some (partial <= 2) (vals seen)))

(defn approachable? [allow-repeat-visit? seen cave]
  (or ((some-fn end-cave? big-cave? (complement seen)) cave)
      (and allow-repeat-visit? (not (has-repeats? seen)))))

(defn find-paths
  ([allow-repeat-visit? connections]
   (find-paths allow-repeat-visit? connections [start-cave] {}))

  ([allow-repeat-visit? connections path small-caves-seen]
   (let [cave (last path)]
     (if (end-cave? cave)
       [path]
       (->> (connections cave)
            (filter (partial approachable? allow-repeat-visit? small-caves-seen))
            (mapcat (fn [cave]
                      (find-paths allow-repeat-visit?
                                  connections
                                  (conj path cave)
                                  (if (small-cave? cave)
                                    (update small-caves-seen cave #(inc (or % 0)))
                                    small-caves-seen)))))))))

(defn part1 [input] (->> input parse-connections (find-paths false) count))
(defn part2 [input] (->> input parse-connections (find-paths true) count))
