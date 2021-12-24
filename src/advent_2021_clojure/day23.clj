(ns advent-2021-clojure.day23
  (:require [advent-2021-clojure.utils :refer [abs]]))

(def hallway-length 11)
(def room-pos {:a 2, :b 4, :c 6, :d 8})
(def all-rooms (keys room-pos))
(def room-entrances (-> room-pos vals set))
(defn move-cost [amphipod amount] (* amount ({:a 1 :b 10 :c 100 :d 1000} amphipod)))

(defn create-burrow [a0 a1, b0 b1, c0 c1, d0 d1]
  {:hallway (vec (repeat hallway-length nil))
   :rooms   {:a [a0 a1]
             :b [b0 b1]
             :c [c0 c1]
             :d [d0 d1]}
   :cost    0})

(defn hallway-at [burrow n] (get-in burrow [:hallway n]))
(defn room-occupants [burrow room] (get-in burrow [:rooms room]))

(defn room-exiting? [burrow room] (let [values (room-occupants burrow room)]
                                    (and (not-every? nil? values)
                                         (not-every? #(= room (or % room)) values))))
(defn room-entering? [burrow room] (let [values (room-occupants burrow room)]
                                     (and (not-every? #(= room %) values)
                                          (every? #(= room (or % room)) values))))
(defn room-complete? [burrow room] (every? #(= room %) (room-occupants burrow room)))

(defn room-mover [burrow room] (->> (room-occupants burrow room)
                                    (keep-indexed (fn [idx v] (when v [idx v])))
                                    first))

(defn exiting-rooms [burrow] (filter #(room-exiting? burrow %) all-rooms))

(defn entering-rooms [burrow] (filter #(room-entering? burrow %) all-rooms))

(defn success? [burrow] (every? #(room-complete? burrow %) all-rooms))

(defn left-right-hallways [room]
  (let [pos (room-pos room)]
    [(remove room-entrances (range (dec pos) -1 -1))
     (remove room-entrances (range (inc pos) hallway-length))]))

(defn hallway-destinations-from [burrow room]
  (->> (left-right-hallways room)
       (map (fn [rng] (take-while #(nil? (hallway-at burrow %)) rng)))
       (apply concat)))

(defn leave-room [burrow room hallway-pos]
  {:pre [(room-exiting? burrow room)
         (nil? (hallway-at burrow hallway-pos))]}
  (let [[depth amphipod] (room-mover burrow room)
        cost (move-cost amphipod (-> room room-pos (- hallway-pos) abs (+ depth) inc))]
    (-> burrow
        (assoc-in [:hallway hallway-pos] amphipod)
        (assoc-in [:rooms room depth] nil)
        (update :cost + cost))))

(defn leave-room-options [burrow]
  (mapcat (fn [room] (map #(leave-room burrow room %)
                          (hallway-destinations-from burrow room)))
          (exiting-rooms burrow)))

(defn nearest-neighbors [burrow room]
  (keep (fn [spaces] (->> spaces
                          (keep #(when-some [v (hallway-at burrow %)]
                                   [% v]))
                          first))
        (left-right-hallways room)))

(defn enter-room-depth [burrow room]
  (->> (room-occupants burrow room)
       (keep-indexed (fn [idx v] (when-not v idx)))
       last))

(defn enter-room [burrow room hallway-pos]
  {:pre [(= room (hallway-at burrow hallway-pos))
         (room-entering? burrow room)]}
  (let [depth (enter-room-depth burrow room)
        cost (move-cost room (-> room room-pos (- hallway-pos) abs (+ depth) inc))]
    (-> burrow
        (assoc-in [:hallway hallway-pos] nil)
        (assoc-in [:rooms room depth] room)
        (update :cost + cost))))

(defn enter-room-options [burrow]
  (mapcat (fn [room] (keep (fn [[pos v]] (when (= v room)
                                           (enter-room burrow room pos)))
                           (nearest-neighbors burrow room)))
          (entering-rooms burrow)))

(def burrow-sorter
  (letfn [(burrow-comparator [b] [(:cost b) (.toString b)])]
    (fn [b1 b2] (compare (burrow-comparator b1) (burrow-comparator b2)))))

(defn solve [burrow]
  (loop [options (sorted-set-by burrow-sorter burrow)]
    (let [b (first options)]
      (if (success? b)
        (:cost b)
        (let [enter (enter-room-options b)]
          (recur (apply conj (disj options b) (if (seq enter)
                                                enter
                                                (leave-room-options b)))))))))

(defn add-header-row [burrow]
  (letfn [(insert-middle [[v0 v1] v0' v1'] [v0 v0' v1' v1])]
    (-> burrow
        (update-in [:rooms :a] insert-middle :d :d)
        (update-in [:rooms :b] insert-middle :c :b)
        (update-in [:rooms :c] insert-middle :b :a)
        (update-in [:rooms :d] insert-middle :a :c))))

(defn part1 [input] (->> input (apply create-burrow) solve))
(defn part2 [input] (->> input (apply create-burrow) add-header-row solve))
