# Day Twelve: Passage Pathing

* [Problem statement](https://adventofcode.com/2021/day/12)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day12.clj)

---

## Preamble

In today's puzzle, I decided to use, or rather overuse, some functions I've only played with a bit in the past, namely
`some-fn` and `every-pred`. I freely admit that I sacrificed some readability for the sake of trying a new approach,
so even though the code is clear enough, I'm not sure I necessarily like it. Today's solution was a fun experiment!

---

## Part 1

We're navigating through underwater caves, looking for every possible path from the start to the end, such that we
never visit the same small cave twice. A small cave has an all-lowercase name, while a big cave has an all-uppercase
name. Both `"start"` and `"end"` are special cases, where we never return to the start, and we're done as soon as we
reach the end.

The data comes in as a sequence of lines of cave names, separated by a dash. We observe that if you can navigate from
cave A to B, then you can also go from B to A, so we'll want to create a map from every cave to every cave it connects
to. Since we never want to return to the start, we'll explicitly remove any such mapping once we're done constructing
the original map; we do this at the end so that we don't have to check both the left- and right-hand-side of each line.
Since "start" and "end" are magic strings we'll refer to several times, we'll define them as vars right away.

```clojure
(def start-cave "start")
(def end-cave "end")

(defn parse-connections [input]
  (let [every-path (->> (str/split-lines input)
                        (map #(str/split % #"-"))
                        (reduce (fn [acc [from to]] (-> acc
                                                        (update from conj to)
                                                        (update to conj from))) {}))]
    (utils/update-values every-path #(remove (partial = start-cave) %))))
```

Since we're talking about simple vars, let's define a few other helper functions so we can remove some lambdas later
on. And to do this, we'll revisit our old friend `some-fn` and introduce a new one named `every-pred`. Recall from 
days 5 and 9 that we use `(some-fn pred1 pred2... predn)` to take in a number of predicates, and return a new one that
returns the first predicate which returns a truthy value. It's our way of saying "do any of these predicates pass?"
We'll now use a similar function called `every-pred`, which has a similar constructions and returns a boolean value
for whether every predicate returns a truthy value. Essentially, `some-fn` is an aggregate `or`, while `every-pred` is
an aggregate `and`. Why isn't the first named `some-pred`, or the second named `every-fn`? And more relevant here,
where isn't there a `no-pred` or `not-any-fn` to aggregate `not`?

Anyway, we'll use `start-cave?` and `end-cave?` to look for those special values; anything that isn't a start or end
cave is an intermediate cave, so we'll use the `complement` (opposite) of `some-fn` to create `intermediate-cave?`.
Finally, `small-cave?` and `big-cave?` are just intermediate caves whose string values are all lowercase or uppercase.
I made two helper functions here in the `utils` package, which are the String equivalent of `Character/isUpperCase`
and `Character/isLowerCase`.

```clojure
; advent-2021-clojure.utils namespace
(defn lower-case? [s] (every? #(Character/isLowerCase ^char %) s))
(defn upper-case? [s] (every? #(Character/isUpperCase ^char %) s))

; advent-2021-clojure.day12 namespace
(defn start-cave? [cave] (= start-cave cave))
(defn end-cave? [cave]   (= end-cave cave))
(defn intermediate-cave? [cave] ((complement (some-fn start-cave? end-cave?)) cave))
(defn small-cave? [cave] ((every-pred intermediate-cave? utils/lower-case?) cave))
(defn big-cave? [cave]   ((every-pred intermediate-cave? utils/upper-case?) cave))
```

Let's think about the `find-paths` function we're about to make. It will need to take in the `connections` map, and
should return all paths we can take from the start to the end, as a sequence of path vectors. We don't explicitly need
the paths themselves, but it seems intuitive to structure it that way. We'll make a simple recursive function,
and we'll provide both a 1-arity implementation (just the `connections`) and a 3-arity implementation (`connections`,
the path to the current cave, and the set of caves we've already seen). We'll look at the last point in the path, and
check to see if it's the end; if so, we return a single-element vector with the current path, since we made it to the
end. If not, we'll look at all connections out from that cave, filter out for the ones that are approachable
(to be implemented later), and then mapcat the recursive call from each approachable cave onto the path ending with 
that cave. We'll also add the latest cave to the set of `seen` caves, so we know where we've already been.

```clojure
(defn find-paths
  ([connections]
   (find-paths connections [start-cave] #{}))

  ([connections path seen]
   (let [cave (last path)]
     (if (end-cave? cave)
       [path]
       (->> (connections cave)
            (filter (partial approachable? seen))
            (mapcat (fn [cave]
                      (find-paths connections (conj path cave) (conj seen cave)))))))))
```

So what makes a cave approachable?  There are three reasons you can go into a cave:
1. This is the end cave, because that finishes the path.
2. This is a big cave, because we can visit big caves multiple times.
3. This is a cave we haven't seen yet, suggesting it's a small cave.

To implement `approachable?`, we'll use `some-fn` to check if any of those predicates apply to the cave.

```clojure
(defn approachable11 [seen cave]
  ((some-fn end-cave? big-cave? (complement seen)) cave))
```

Finally, we can implement `part1` by parsing the input, finding all paths from the start to the end, and then counting
the number of paths returned.

```clojure
(defn part1 [input] (->> input parse-connections find-paths count))
```

---

## Part 2

In part 2, we learn that we can still visit big caves any number of times, but we can also revisit a single small cave
once. We'll have to make a small modification to the existing functions to get this to work, but overall our structure
should hold.

I think the easiest way to think about this is to go back to `approachable?` before rewriting `find-paths`. The
`approachable?` function can now allow entry into a small cave if there hasn't already been a double-entry cave.
So now, the `approachable?` function takes in three arguments - an `allow-repeat-visit?` flag that's false for part 1
and true for part 2, a _map_ (not set) of caves already seen to the number of visits, and the cave in question. The
`((some-fn end-cave? big-cave? (complement seen)) cave)` code from Part 1 still works just fine. But now, if none of
those predicates apply to the cave, we can allow the cave if both `allow-repeat-visit?` is true and we have not seen
any repeat visitors. The `has-repeats?` function will check if any cave has been visited at least twice already.

```clojure
(defn has-repeats? [seen] (some (partial <= 2) (vals seen)))

(defn approachable? [allow-repeat-visit? seen cave]
  (or ((some-fn end-cave? big-cave? (complement seen)) cave)
      (and allow-repeat-visit? (not (has-repeats? seen)))))
```

Now that that works, we need to make some small changes to `find-paths`. First of all, its arity will switch to a
2-arity `(allow-repeat-visit?` and `connections`) and a 4-arity (`allow-repeat-visit?`, `connections`, the `path`, and
the set of only the small caves we've already seen). Most of the logic is the same, other than passing in an empty map
instead of an empty set to the 4-arity implementation. But when we're ready to recursively call `find-paths`, we need
to only increment the mapping of the current `cave` if it's small. Unfortunately, `(update {} :a inc)` will throw a
`NullPointerException` because you can't increment a value that's not in the map yet, so to accommodate for the first
instances of visiting a small cave, we'll need to execute `(update small-caves-seen #(inc (or % 0)))`; the `or`
function is a great way to handle nulls, since `#(inc (or % 0))` means we increment the current value in the map, or 
0 if it's not in the map.

```clojure
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
```

Finally, we can refactor `part1` and implement `part2` by calling the new `find-paths` function, passing in the
appropriate values for `allow-repeat-visit?`.

```clojure
(defn part1 [input] (->> input parse-connections (find-paths false) count))
(defn part2 [input] (->> input parse-connections (find-paths true) count))
```
