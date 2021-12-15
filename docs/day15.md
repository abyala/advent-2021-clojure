# Day Fifteen: Chiton

* [Problem statement](https://adventofcode.com/2021/day/15)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day15.clj)
* [Sorted Value Map code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/sorted_value_map.clj)

---

## Preamble

I thoroughly enjoyed today's problem because I got to create my very own little Clojure data structure! I'm excited to
share the experience below.

---

## Part 1

Today we have a pretty standard shortest-path algorithm. We're wandering through a two-dimensional cave, finding the
shortest road from the top-left corner to the bottom right. I've found these sorts of problems easiest to manage with
a map of coordinates instead of nested arrays, so let's parse the data into a map. This is easily done with some
tools I've shown multiple times this year already -- we `reduce` the sequence of `parse-to-char-coords` into a map
by parsing each numeric character into an int. To make this simpler, we'll expand `utils/parse-int` to support both
characters and Strings.

```clojure
; advent-2021-clojure.utils namespace
(defn parse-int [v] (if (char? v)
                      (Character/digit ^char v 10)
                      (Integer/parseInt v)))

; advent-2021-clojure.day15 namespace
(defn parse-cave [input]
  (reduce (fn [acc [point c]] (assoc acc point (parse-int c)))
          {}
          (point/parse-to-char-coords input)))
```

Now that we have a workable cave, let's create a `shortest-path` function to walk through it. Spoiler alert - this
algorithm won't scale to part 2, and I'll make mention of the very few changes we'll need to make later on. But let's
proceed without this foreknowledge.

To start with, we need to determine the `target` of the cave; since we start at the `origin` of `[0 0]`, we just need
to find the largest value in the cave and make a vector out of it. Then we'll make use of a recursive `loop-recur`
to sift through every possible next step we can take, until we get to the target. `candidates` will bind every
reachable point to its cost, meaning the cost it took to get to the previous points plus the risk level of that point;
`seen` is a set of points we have already visited, so avoid infinite loops. Each time through the loop, we want to
select the cheapest available point, so we'll sort the candidates by the value (`second`) and take the first option.
Then at each point, we'll look to the four cardinal neighbors, keep only the ones that are in the cave, remove any
point we've already visited, and then map those coordinates to the new cost to get there. Assuming we haven't reached
the target yet, when we loop back around, we call `(merge-with min m1 m2)` to merge the map of remaining candidates
with any new candidates revealed at the current point; since it's possible that the newest path to a candidate point
isn't the cheapest option, we use `merge-with` instead of `merge` to intelligently keep the lower value. 

```clojure
(defn shortest-path [cave]
  (let [max-ordinate (apply max (map ffirst cave))
        target [max-ordinate max-ordinate]]
    (loop [candidates {point/origin 0}, seen #{}]                         ; Remember me
      (let [[point cost] (first (sort-by second candidates))              ; and me
            next-options (->> (point/neighbors point)
                              (filter cave)
                              (remove seen)
                              (map #(vector % (+ cost (cave %))))
                              (into {}))]
        (if (= point target)
          cost
          (recur (merge-with min (dissoc candidates point) next-options)  ; and me
                 (conj seen point)))))))
```

Finally, we can create a simple solution for the `part1` function to get our star.

```clojure
(defn part1 [input]
  (-> input parse-cave shortest-path))
```

---

## Intermission

Before we get to the details for part 2, we're going to find that the cave is about to get a lot bigger, and within
that larger cave, inspecting every possible option at every point, re-sorting them by lowest value, just isn't going 
to cut it anymore. Clojure does have nice built-in functions for sorted maps and sorted sets, but they sort on the
keys, not the values. In this case, we want to map each coordinate pair to its cost, but we want to pick the smallest
value (cost) each time through. There are some libraries out to do this, but I don't want to use other libraries in my
Advent problems, so I built one myself.

Introducing, the Priority Map!

### Sorted Value Map

The idea behind the sorted map is that it's a normal associate key-value map, but the entries are sorted not by value
instead of by key. Note that this isn't going to be a complete implementation of either Java's `Map` interface, nor
Clojure's `Associative` interface, because, well, I don't need to! The only functions I want to support here are
`assoc`, `dissoc`, `first`, and `merge-with`. I deliberately chose to use core function names because they look
familiar, but that does mean that we'll have collisions between the core functions and my namespaced functions. So
for once, the namespace directive is actually somewhat interesting. I'm not 100% sure this is the best way to
implement this, but it's how I approached it. I used `:refer-clojure` to modify the references to the base
language. First, I exclude `assoc` and `merge-with` since I don't need the core implementations at all. Then I rename
the functions `dissoc` and `first` to `c-dissoc` and `c-first`, so I can still make use of them without having a
collision.

```clojure
(ns advent-2021-clojure.sorted-value-map
  (:refer-clojure :exclude [assoc merge-with]
                  :rename {dissoc c-dissoc, first c-first}))
```

To supported a sorted value map, I need to maintain two internal maps. First is the normal map of each key to its
value, recognizing that multiple keys can map to the same value. Second is a sorted map of each value to a sorted set
of the keys that point to it. So for example, if we had a normal map of `{:a 1, :b 3, :c 1}`, the sorted value map
would be represented as `{:key-to-value {:a 1, :b 3, :c 1}, :value-to-keys {1 #{:a :c}, 3 #{:b}}}`.

From here on out, I will refer to a sorted value map as an "svm."

To start, we'll create the value `empty-map` which sets up our structure, and two private helper functions that return
the value for a key, and the set of keys for a value.

```clojure
(def empty-map {:key-to-value {}, :value-to-keys (sorted-map)})
(defn- value-of [svm k] (get-in svm [:key-to-value k]))
(defn- keys-of [svm v] (get-in svm [:value-to-keys v]))

```

Before we get to `assoc`, let's implement `dissoc`, which takes in an svm and the key to remove. If the key isn't in
the svm, just return the svm unchanged. Otherwise, check to see the key is the _only_ one associated to that value in
the map. If so, we want to clean up the `:value-to-keys` map by removing the mapping entirely.  Otherwise, we just
remove that one key from the sorted set for that value. Either way, we remove the mapping from that key in the
`:key-to-value` map.

```clojure
(defn dissoc [svm k]
  (if-some [old-value (value-of svm k)]
    (if (= #{k} (keys-of svm old-value))
      (-> svm
          (update :key-to-value c-dissoc k)
          (update :value-to-keys c-dissoc old-value))
      (-> svm
          (update :key-to-value c-dissoc k)
          (update-in [:value-to-keys old-value] disj k)))
    svm))
```

Now let's implement `assoc`. If the svm already maps that key to its value, just return the svm unchanged. Otherwise,
we first dissoc the key from the map (if it's even in there), then associate the key to its value, and then update
the sorted set of keys mapped to the value. We want to make sure the `:value-to-keys` map stores the keys for a value
in order (not really for any valid reason other than "cleanliness"), so we'll create the sorted set if the key being
added to the svm is the first mapping to that value.

```clojure
(defn assoc [svm k v]
  (if (= v (value-of svm k))
    svm
    (-> svm
        (dissoc k)
        (assoc-in [:key-to-value k] v)
        (update-in [:value-to-keys v] #(if % (conj % k) (sorted-set k))))))
```

To find the "first" or lowest value in the map, we start with the `:value-to-keys` map, which is sorted, and call
`c-first` to get that lowest entry of `[value set-of-keys]`. If we have such an entry, meaning that the map isn't
empty, we then pull out the first key from the `set-of-keys` (again, it's sorted), and provide that as a tuple vector
of the key and its value. Theoretically, if I were to provide a `seq` function for an svm, it would return a sequence
of these tuples.

```clojure
(defn first [svm]
  (when-some [[v ks] (-> svm :value-to-keys c-first)]
    [(c-first ks) v]))
```

Finally, we implement `merge-with`, which takes in a comparison function, an svm, and another map, with the goal of
merging every entry from the other map into the svm, based on whichever has the lowest value from the comparison
function; in our case, we will expect to be passed in the function `min`, so we only store the cheaper path into svm.
(Technically, this shouldn't be necessary, but it was fun to write.) We'll use `reduce-kv` to pull in every value
from the map, and we'll `assoc` it in only if the svm doesn't have a mapping for that key yet, or if the new value is
"better" than the previous one.

```clojure
(defn merge-with [f svm other-map]
  (reduce-kv (fn [acc k v] (if-some [curr-v (value-of svm k)]
                             (if (= curr-v (f v curr-v))
                               acc
                               (assoc acc k v))
                             (assoc acc k v)))
             svm
             other-map))
```

None of this was strictly necessary; we could have used a library for a heap, or perhaps even a prioritized queue, but
I couldn't find something convenient to use. Besides, this was fun!

### Refactoring Part 1

Refactoring part 1 is really simple, because we only need to update a few lines in the `shortest-path` function to
leverage the svm instead of the simple map. In fact, I'll quickly show the three pairs of lines that changed.

```clojure
; The candidates loop changes from a map to an svm.
(loop [candidates {point/origin 0}, seen #{}]                         
(loop [candidates (svm/assoc svm/empty-map point/origin 0), seen #{}]

; The code to get the first candidate now uses the svm implementation of first, instead of sorting every time.
(let [[point cost] (first (sort-by second candidates))
(let [[point cost] (svm/first candidates)

; The recur logic uses the svm implementation of merge-with instead of the core implementaiton.
(recur (merge-with min (dissoc candidates point) next-options)
(recur (svm/merge-with min (svm/dissoc candidates point) next-options)
```

So that makes this our new `shortest-path` function.

```clojure
(defn shortest-path [cave]
  (let [cave-length (apply max (map ffirst cave))
        target [cave-length cave-length]]
    (loop [candidates (svm/assoc svm/empty-map point/origin 0), seen #{}]
      (let [[point cost] (svm/first candidates)
            next-options (->> (point/neighbors point)
                              (filter cave)
                              (remove seen)
                              (map #(vector % (+ cost (cave %))))
                              (into {}))]
        (if (= point target)
          cost
          (recur (svm/merge-with min (svm/dissoc candidates point) next-options)
                 (conj seen point)))))))
```

---

## Part 2

Alright, we're armed with everything we need to efficiently find our way through a cave, but now we need a bigger cave!
We'll need to take our incoming cave and line up 25 instances of them, in a 5x5 meta-square. The only tricky thing is
that we'll need to increment every value by the distance of the current mini-cave from the initial cave, wrapping
values from 10 down to 1.

So first, let's figure out how to calculate a cave's risk level. I'll assume that the calling code will pass in the
risk level we would think we want, based on `(+ listed-risk-level distance-to-initial-cave)`, which we then convert
down to a number between 1 and 9. Rather than play with mods, I used `(cycle (range 1 10))` to give an infinite
sequence of values from 1 to 10, with a `(cons 0)` in the front since the 0th value, I suppose, should be a 0. Then we
grab the `nth` value out of it. For a tiny hint of efficiency, I chose to `memoize` the result, since there's no reason
to recalculate the value every time.

```clojure
(def cave-risk
  (memoize (fn [n] (nth (cons 0 (cycle (range 1 10))) n))))
```

The `multiply-cave` function takes in a cave and returned that cave extended out into a meta-cave of length `n`; for
part 1, `n` will be 1, and in part 2 it will be 5. We'll use the `for` macro to look at x- and y- distances from 0 to
`n`, representing the distance of the mini cave from the initial cave. For each value `[x y]` in the original cave,
we'll need to add `x-offset` and `y-offset` to it, representing the location of the "origin" of the new mini-cave.
This value is just the length of the cave times the `x` or `y` value of the mini-cave, and `p-offset` is the `[x y]`
coordinate of this new origin. Finally, `n-offset` represents the grid-distance of the mini-grid, since the risk level
of every point in the mini-cave increases by this amount.

With all of those little calculations out of the way, `multiply-cave` takes each value in the original mini-cave and
reduces it into a new map, where the key is the mini-origin `p-offset` added to the coordinates of the point, and the
new value is the calculated `cave-risk` by adding the original value `n` to the `n-offset` of this new mini-cave.
Once we have built all of those mini-caves (1 for part 1 and 25 for part 2), we simply `(apply merge mini-caves)` to
assemble one giant cave.

```clojure
(defn multiply-cave [cave n]
  (let [length (inc (apply max (map ffirst cave)))]
    (apply merge (for [grid-x (range 0 n) :let [x-offset (* grid-x length)]
                       grid-y (range 0 n) :let [y-offset (* grid-y length)
                                                p-offset [x-offset y-offset]
                                                n-offset (+ grid-x grid-y)]]
                   (reduce (fn [acc [p n]] (assoc acc (mapv + p p-offset)
                                                      (cave-risk (+ n n-offset))))
                           {} cave)))))
```

Now we're ready to finish the problem. Solving a puzzle involves parsing the input into a cave, multiplying it out
by the correct number of multiples, and then returning the shortest path. For part 2, this algorithm reduced the
running time of my original algorithm from about 100 seconds to about 4, which is fast enough for me!

```clojure
(defn solve [num-multiples input]
  (-> input parse-cave (multiply-cave num-multiples) shortest-path))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 5 input))
```

---

## Epilogue

I suspected I could make my algorithm even more efficient by making use of an A* algorithm. This involved rewriting
the SVM into a Priority Map, in which I stored a key mapped to both its value and its priority; the priority was used
in the sorting, by getting the first value out of the Priority Map would return the value instead. The goal was to
help pick the most efficient path by always pushing closer to the target point, rather than just picking the cheapest
point overall. Oddly enough, this wasn't any more efficient than the simple SVM, so I didn't bother sharing that code.
I must have written it poorly, though, because that _should_ have shaved more time off.  Oh well; maybe in AoC 2022!