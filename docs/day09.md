# Day Nine: Smoke Basin

* [Problem statement](https://adventofcode.com/2021/day/9)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day09.clj)

---

## Preamble

This year, I seem to be on a quest to replace anonymous functions of the form `#(foo %)` with partial functions and
the use of `juxt` and `comp`. I'm not sure why that is, but it seems to be a pattern. I have discussed `partial`, 
`juxt`, and `comp` in previous problems this season, so I won't be explaining them anymore, so feel free to look back
if things are starting to look funny.

---

## Part 1

When exploring underwater caves, one does run the risk of running into
[liquid hot magma](https://www.youtube.com/watch?v=UNJU-5vCrJc), which apparently we have done today. We need to
navigate the cave carefully. In part 1, we need to identify all points within the cave that are local minima (smaller
than all cardinal neighbors), increment their heights and add the results together.

In most such problems, I tend to convert the entire input into one giant map of coordinates to values, in the shape of
`{[x y] v}`, but for a change of pace I decided to go old-school and use nested vectors. So my core data structure
is simply `[[values]]`. Thus to parse the cave from the initial input, I split the input string into a sequence of
strings for each line, and then we have a fun line `(mapv (partial mapv (comp parse-int str)) line)`. So let's piece
this together. The outer `mapv` says we're going to create a vector from mapping each line to the internal function;
we need `mapv` to ensure we get an outer vector, since we'll be accessing it by index.  The inner function can be
partial because we're calling `(mapv (comp parse-int str) line)` where `line` is fed in from the outer `mapv`. Each
inner line is a numeric string, so we need to convert each character to its integer value. I implemented a `char->int`
function in the `utils` package to perform this.

```clojure
; advent-2021-clojure.utils namespace
(defn char->int [c] (- (int c) 48))

; advent-2021-clojure.day09 namespace
(defn parse-cave [input] (->> (str/split-lines input)
                              (mapv (partial mapv utils/char->int))))
```

Next, we need to find all of the lowest points in the cave, so we'll need four functions to get there. First, the
`neighbors` function in the `advent-2021-clojure.point` namespace will return the coordinates of the four points
adjacent to a single point. This is one of those instances of using `(mapv f coll1 coll2)` instead of the normal
`(mapv f coll)`, where the mapping function gets applied to the nth instance of multiple collections. This lets us
add the `x` and `y` coordinates of two points together.

The `lowest-point?` function checks if a single point in a cave is a local low point. This is the first of several
instances where we'll be calling `(get-in cave point)`. `get-in` takes in a collection and a vector of nested values
to apply to get to a sub-value in a collection, or `nil` if that value does not exist in the collection.  So
`(get-in {:name {:first "Franklin" :middle "Delano" :last "Roosevelt"}} [:name :middle])` returns `"Delano"`, and for
vectors, `(get-in [:a :b [:c :d :e]] [2 2])` returns `:e`.  Thus we get the value of a point (which is a vector) within
the cave by calling `(get-in cave point)`. Then we find all neighbors to the given point, keep the ones that exist
within the cave by calling `keep`, and then ensure that the point's height is lower than all of its neighbors.

With that function, it's easy to find all of the lowest points. `all-coords` will return all `[y x]` coordinates in
the cave; note that we use `[y x]` instead of `[x y]` because the outer vector of our `[[values]]` data structure is
the row, which is indexed by `y`, and the inner vector is the column, which is indexed by `x`. Finally, in
`lowest-points`, we take all of the coordinates in the cave, and filter them to the ones that pass `lowest-point?`.

```clojure
; advent-2021-clojure.point namespace
(defn neighbors [point]
  (map (partial mapv + point) [[0 1] [0 -1] [-1 0] [1 0]]))

; advent-2021-clojure.day09 namespace
(defn lowest-point? [cave point]
  (let [height (get-in cave point)]
    (->> (neighbors point)
         (keep (partial get-in cave))
         (every? (partial < height)))))

(defn all-coords [cave] (for [y (-> cave count range)
                              x (-> cave first count range)]
                          [y x]))

(defn lowest-points [cave]
  (filter (partial lowest-point? cave) (all-coords cave)))
```

Finally, we're ready to write `part1`. Starting with the coordinates of all of the lowest points in the cave, we map
them to their actual values, increment each one, and add them together.

```clojure
(defn part1 [input]
  (let [cave (parse-cave input)]
    (->> (lowest-points cave)
         (map (partial get-in cave))
         (map inc)
         (apply +))))
```

Have we reached the low point of our problem today?  Let's find out.

---

## Part 2

Now we need to find all of the "basins" in the cave, a section whose phrasing I found incredibly misleading. The
instructions say, in part, "A basin is all locations that eventually flow downward <u>to a single low point</u>"
(emphasis added). That last part is untrue. What the problem seems to mean is "a basin is all locations that are 
fully enclosed by either the outer wall or a 9." It appears that a basin can happily contain two or more low points.

So the way I want to find the basins is to go to each lowest point, and find the basin around it by moving outward
until we hit either a wall or a 9. For this, we'll first start with a `high-point?` function, which just returns
whether a coordinate in the cave equals the max value of 9.

Then, the `basin-around` function takes in a cave and a low point, and returns the set of all coordinates within that
basin. We'll use the recursive `loop-recur` construct, in which I often use the bindings of `candidates` (what I'm
looping over) and `found` (the accumulated values). If there are candidate points to inspect, which we can tell by
calling `(if-some [point (first candidates)])`, then we continue to the loop, and if not then we return the set of
found points. When looping, we remove the current point from the `candidates` with `(rest candidates)`, and then add
the all neighbors of that point that are in the cave, but which are not either already found or are themselves high
points.

```clojure
(def max-height 9)
(defn high-point? [cave point] (= max-height (get-in cave point)))

(defn basin-around [cave lowest-point]
  (loop [candidates #{lowest-point}, found #{}]
    (if-some [point (first candidates)]
      (recur (reduce conj (rest candidates) (->> (point/neighbors point)
                                                 (filter (partial get-in cave))
                                                 (remove (some-fn found (partial high-point? cave)))))
             (conj found point))
      found)))
```

And then we're ready to build `part2`.  Starting from each of the lowest points, we map each one first to its basin
(expressed as its set of points), which we then convert to the number of points within the basin. Then
`(sort-by -)` puts them in reverse size order, from which we take the top three and multiply those values together.

```clojure
(defn part2 [input]
  (let [cave (parse-cave input)]
    (->> (lowest-points cave)
         (map (comp count (partial basin-around cave)))
         (sort-by -)
         (take 3)
         (apply *))))
```