# Day Ninteen: Beacon Scanner

* [Problem statement](https://adventofcode.com/2021/day/19)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day19.clj)

---

## Preamble

I'll just come out and say that this problem was too hard for a single day. I could see this having been split into 2
or even 3 different problems, given the complexities of both the background and the puzzle itself. I have a working
solution that's pretty slow, but I got there.

One of my biggest struggles with this problem was understanding the nature of the beacons and the scanners. I
improperly spent a large amount of time having created the space of beacons around the scanner, and then attempted to
rotate the entire space. This is not how the problem is intended to be interpretted. Instead, one needs to hold the
beacons steady, and then change the way that the scanner is pointing. So the trick was to think of the beacons as
normal points within a cube, and to imagine the scanner as a six-sided die with only a single point having dots.

Once I got that, I was at least closer to a working solution. But teasing it apart piece by piece, the puzzle is
doable.

---

## Part 1

First, let's parse our input.  Splitting the data into groups of lines, I'll transform each block of text lines into a
scanner of form `{:id n, :beacons #{[x y z]}, :scanners #{[0 0 0]}}`. The ID comes from the header line, the beacons
from the rest of the data, and the scanners will be a set of a three-dimensional origin, since the scanners begin not
having any context to each other. Finally, the parser will map each ID to its scanner, thus ending with a total
structure of `{n {:id n, :beacons #{[x y z]}, :scanners #{[0 0 0]}}`.

```clojure
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
```

Next, we'll make a vector of 24 transformational functions, which represent the different ways that the _scanner_ can
face. Again, this has nothing to do with the beacons; it's all about the scanner. To that end, there are six groups of
four functions, signifying the six directions the scanner can face, and the four rotations it can make within each
direction. Each of these functions takes in an `[x y z]` point, and returns how the scanner would transform that point
based on its orientation. I was only able to come up with this vector by placing and rotating objects within my house,
so I could see how all 3 dimensions work together.

```clojure
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
```

Next, I'll make two helper functions for relating two points to each other. `path-to` takes in a `from` point and a
`to` point, and returns the different between the two, representing the values to add to `from` to end up at `to`.
Then `follow-path` takes in a point and a path from `path-to`, and moves the point along that path.

```clojure
(defn path-to [from to] (mapv - to from))
(defn follow-path [point path] (mapv + point path))
```

Next, I have three similar functions - `combine-beacons`, `combine-scanners`, and `combine-all-scanners`. Let's handle
them one at a time.

`combine-beacons` takes in two sets of beacons, from two separate scanners. Assuming the first set of beacons is the
fixed set, it attempts to return the first path that would move at least 12 points from the second set onto the first.
For this, we'll use `(for [b0 beacons0, b1 beacons1] (path-to b1 b0))` to return _almost_ all possible vectors for the
paths from the second set of beacons onto the first. We know that at least points need to overlap, but we don't know
which ones. Regardless, instead of testing all of `beacons1` against all of `beacons0`, we can test against all but any
11 arbitrary values from `beacons0` instead; if we know we need at least 12 points to match, then one working path
should imply at least 12 ways to get to it. Finally, to see if the path is acceptable, we call `follow-path` on all
beacons in the second set, and test if the intersection against the first set if at leats 12; if not, then that path
does not lead to the necessary overlap. The function then returns either the first workable path, or else `nil`.

```clojure
(defn combine-beacons [beacons0 beacons1]
  (->> (for [b0 (drop 11 beacons0), b1 beacons1] (path-to b1 b0))
       (filter (fn [path] (>= (->> (map #(follow-path % path) beacons1)
                                   set
                                   (set/intersection beacons0)
                                   count)
                              12)))
       first))
```

Peeling up a layer, we get to `combine-scanners`. This looks at two scanners and their beacons, and checks to see if
there is any orientation of `scanner1` such that its beacons overlap with those from `scanner0`. If so, then return
what `scanner0` would look like if we merged in all of the data from `scanner1`. This means applying the orientation
function to all of the points, finding the working path, moving all reoriented points along that path, and adding them
to `scanner0`'s beacons. Similarly, since both scanners think themselves the origin, we'll reorient the `:scanners` in
`scanner1` and send them along the path, adding the resulting values ot the set of `:scanners` in `scanner0`. If the
two scanners can't overlap, again return `nil`.

```clojure
(defn combine-scanners [scanner0 scanner1]
  (let [[beacons0 beacons1] (map :beacons [scanner0 scanner1])]
    (first (keep (fn [f]
                   (let [beacons1' (map f beacons1)]
                     (when-some [path (combine-beacons beacons0 beacons1')]
                       (-> scanner0
                           (update :beacons set/union (set (map #(follow-path % path) beacons1')))
                           (update :scanners (fn [s] (apply conj s (map #(follow-path (f %) path) (:scanners scanner1)))))))))
                 orientation-fns))))
```

Finally, `combine-all-scanners` takes in the map of parsed scanners, and keeps combining them until there's only a
single scanner remaining. It starts by looking at all unique pairs of IDs in incrementing value, since there's no need
to join scanner 2 to scanner 3 if we can't join scanner 3 to scanner 2. Then for each pair, if they can be joined, then
remove the second scanner and update the first one to have the combined data. After going through all possible pairs,
recurse back through the function again until there's only one scanner remaining. Note that since this function takes a
long time to run, I left in some `println` statements to show progress.

```clojure
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
```

Finally, for `part1`, we just have to count the number of beacons remaining in the merged scanner. So parse the
data, combine the scanners, and get the count of beacons.

```clojure
(defn part1 [input] (->> input parse-input combine-all-scanners :beacons count))
```

---

## Part 2

For part 2, we need to again combine all of the scanners, but then calculate the maximum distance between any two
scanners. For this, we'll implement a three-dimensional `manhattan-distance` function, which computes the path between
two points, and adds together the sum of each path's dimension.  Then we pair together each set of scanner points,
calculate the `manhattan-distance`, and return the largest value seen.

```clojure
(defn manhattan-distance [p1 p2]
  (->> (path-to p1 p2)
       (map utils/abs)
       (apply +)))

(defn greatest-distances [s]
  (->> (for [v1 s, v2 s, :when (not= v1 v2)] [v1 v2])
       (map (partial apply manhattan-distance))
       (apply max)))

(defn part2 [input] (->> input parse-input combine-all-scanners :scanners greatest-distances))
```

So yeah - it's not a whole lot of code once we're all said and done, but I found the puzzle very difficult to
understand. Still, it's done! The code is slow and verbose, but it's done!