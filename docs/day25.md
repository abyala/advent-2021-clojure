# Day Twenty-Five: Sea Cucumber

* [Problem statement](https://adventofcode.com/2021/day/25)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day25.clj)

---

## Preamble

As tends to happen, Day 25 is a fairly easy problem. I decided to have a little fun with it, just to make the solution
a little extra fun, but it wasn't strictly necessary.

For my solution I leveraged just a tiny bit of `defmulti` and `defmethod`, since I hadn't used them all year. The
`defmulti` macro defines a function that dispatches to one of several implementations, based on how the multi-method
dispatch is configured. Java sort of has two ways of doing this - there's polymorphism by inheritance (a method on an
interface or class gets implemented or overridden at runtime), and there are overloaded methods (several methods on
a class or interface with the same name but different argument types). In Clojure, we have full control over how to
dispatch a multi-method, based on the method arguments.

For instance, I'll make a multimethod called "describe arguments," which takes in a variable number of arguments, and
returns a String description of them. I'll implement four multi-method implementations for 0, 1, 2, or any other number
of arguments. Try doing _that_ in Java!

```clojure
(defmulti describe-arguments (fn [& args] (count args)))
(defmethod describe-arguments 0 [] "Nothing")
(defmethod describe-arguments 1 [v] (str "One item of type " (type v)))
(defmethod describe-arguments 2 [v0 v1] (str "Two items, whose equality is " (= v0 v1)))
(defmethod describe-arguments :default [& vs] (str (count vs) " items, beginning with " (first vs)))

(describe-arguments)
=> "Nothing"
(describe-arguments "Something")
=> "One item of type class java.lang.String"
(describe-arguments "Foo" "Bar")
=> "Two items, whose equality is false"
(describe-arguments "Foo" "Bar" "Baz")
=> "3 items, beginning with Foo"
```

So again, this is absolutely overkill, but I'll use a multimethod for fun today.

---

## Part 1

We are given a map of down and right arrows, representing the movement patterns of cucumbers. We need to detect how
many turns it will take until they don't move.

I originally made a simple data structure of `{[x y] value}` for every point in the map, but I made it faster by
storing separate sets of the right-facing coordinates and down-facing coordinates, plus the max `x` and `y` values in
the map. For convenience that we'll see later, I named my set keys `:right` and :down`.

```clojure
(defn parse-map [input]
  (let [points (point/parse-to-char-coords input)
        max-x (apply max (map ffirst points))
        max-y (apply max (map (comp second first) points))]
    (reduce (fn [acc [coords c]] (case c
                                   \> (update acc :right conj coords)
                                   \v (update acc :down conj coords)
                                   \. acc))
            {:right #{}, :down #{}, :max-x max-x, :max-y max-y}
            points)))
```

Next, we want to know what where each sea cucumber wants to move to, based on the direction it's facing, and that the
map wraps around the edges. So we'll make our over-designed multi-method take in the map, the coordinates, and the
direction we're facing, where we dispatch on the direction. The we create two implementation methods to provide the
target coordinates.

```clojure
(defmulti target-from (fn [_ _ dir] dir))
(defmethod target-from :right [m [x y] _]  (if (>= x (:max-x m)) [0 y] [(inc x) y]))
(defmethod target-from :down [m [x y] _]  (if (>= y (:max-y m)) [x 0] [x (inc y)]))
```

Overkill and bad design for the problem statement? Absolutely. Neat? I think so!

Now let's move all available sea cucumbers in a single direction, either `:right` or `:down`, which as usual we can
accomplish with a simple `reduce`. We'll reduce over `(dir m)` to get all coordinates with the cucumbers moving in the
desired direction, and we'll start with the current state of the map. For each cucumber, we'll determine the target
it's trying to reach, and make sure it's not occupied by checking if either `(:right m)` or `(:down m)`, both
returning sets, contain the target. If one of them does, don't change the accumulating map. If the spot is vacant, then
the set of coordinates in this direction loses the original coordinates and adds the target coordinates. Note that when
checking for an occupant, it's important to compare against the original map `m` instead of the accumulated map `acc`,
since all cucumbers in one direction move at the same time, politely.

```clojure
(defn move-all-in-direction [m dir]
  (reduce (fn [acc coords]
            (let [target (target-from m coords dir)]
              (if ((some-fn (:right m) (:down m)) target)
                acc
                (-> acc
                    (update dir conj target)
                    (update dir disj coords)))))
          m
          (dir m)))
```

Now we can put it all together.  First, we'll create `take-turn`, which moves all cucumbers in the map first right and
then down. We could use the thread-first macro as always, but I think that another `reduce` that goes `[:right :down]`
is a neat way of looking at things. This also shows that if a reducing function takes in two arguments, the
accumulated value and the next value, we can use the function by name instead of needing a lambda. And since the
arguments for `move-all-in-direction` are `[m dir]`, look how neat `take-turn` can look!

```clojure
; I think this structure is neat, in case the cucumbers start moving in stranger patterns in the future.
(defn take-turn [m]
  (reduce move-all-in-direction m [:right :down]))

; Currently it's equivalent to the more explicit code:
(defn take-turn [m]
  (-> m
      (move-all-in-direction :right)
      (move-all-in-direction :down)))
```

Then `sea-cucumber-seq` returns the sequence of all distinct maps of sea cucumbers. I opted to use a `lazy-seq` instead
of my more typical solution of `iterate`, `(partition 2 1)`, and `take-while`, because this makes it easier to include
both the initial map value and the final one. The function returns the current map and lazily evaluates the next map
if it has changed. Then all that `part1` has to do is parse the map, create the sequence, and count the distinct number
of maps.

```clojure
(defn sea-cucumber-seq [m]
  (let [next-map (take-turn m)]
    (if (= m next-map)
      (lazy-seq [m])
      (lazy-seq (cons m (sea-cucumber-seq next-map))))))

(defn part1 [input] (->> input parse-map sea-cucumber-seq count))
```

That's it! Now, once I finish up day 19's problem, this will be another successful year of Advent Of Code!