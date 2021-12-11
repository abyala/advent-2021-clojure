# Day Eleven: Dumbo Octopus

* [Problem statement](https://adventofcode.com/2021/day/11)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day11.clj)

---

## Preamble

Today's puzzle deals with watching bioluminescent dumbo octopuses flash in the water. When most of us are doing the
problems each day, we just want to get to it. However, if you're reading this and didn't watch the
[video clip of dumbo octopuses](https://www.youtube.com/watch?v=eih-VSaS2g0), now is a good time to do it!

One other thing - according to both [this Grammarly article](https://www.grammarly.com/blog/octopi-octopuses/) and
[this Merriam-Webster article](https://www.merriam-webster.com/words-at-play/the-many-plurals-of-octopus-octopi-octopuses-octopodes), while the
plural form for "octopus" can be both "octopuses" or "octopi," neither is exclusively correct and the former is more
English-sounding. So Twitter commenters can all relax now; they're both fine. Swim along, please.

## Part 1

Today, as our submarine travels through the underground cave, we find 100 conveniently aligned dumbo octopuses whose
bodies flash as they gain energy. We're here to watch the show and keep records of when they flash. I will explain
the steps of the flashing rules throughout the problem, but if you haven't read the puzzle, it would make more sense
if you did so.

Let's start off with parsing. On day 9, I said that I usually convert grids to maps but wanted instead to work with
vectors of vectors. Well... this time I stuck with maps. They can often be easier to deal with, especially when a
change with one point cascades to another point. To help with today's problem, I pulled in some helper code I've used
in previous years, in a function called `point/parse-to-char-coords`, which reads multiple lines of input and returns
a sequence (not a map) of `[[x y] c]` mapping the coordinates to the character. To parse out the grid, I push that
sequence into a map, converting each character into an int, using the `char->int` function we saw on day 9.

```clojure
; advent-2021-clojure.point
(defn parse-to-char-coords
  "Given an input string, returns a lazy sequence of [[x y] c] tuples of [x y] coords to each character c."
  [input]
  (->> (str/split-lines input)
       (map-indexed (fn [y line]
                      (map-indexed (fn [x c] [[x y] c]) line)))
       (apply concat)))

; advent-2021-clojure.day11 namespace
(defn parse-grid [input]
  (->> (point/parse-to-char-coords input)
       (reduce (fn [acc [p c]] (assoc acc p (utils/char->int c))) {})))
```

Next, I'm going to create two helper functions called `flashes?` and `flash-keys`. Since an octopus flashes if its
energy level exceeds a value of 9, the `flashes?` function handles that logic. Then `flash-keys` examines the energy
level of octopuses, returning the coordinates of those who are flashing.

```clojure
(defn flashes? [v] (> v 9))

(defn flashing-coordinates [grid] 
  (keep (fn [[k v]] (when (flashes? v) k)) grid))
```

Now I've said many times in these write-ups that part of what I love about both Clojure and functional programming is
the clarity you can from putting together lots of little functions. I will gladly take a few extra lines to introduce
"business-sounding" functions for clarity, even if it makes my code more verbose.  Therefore, I'll make a few smaller
functions, and one larger one, so the code is easier to understand.

First, we'll scroll down a bit in my solution to the `take-turn` function. Eventually, I'm going to want to call this
function and have it return the next state of the grid and the number of nodes that flashed. For reasons I'll explain
later, this function will take in an argument of the same type it will return - `[grid num-flashed]`. Let's walk
through what the steps are:
1. Given the current grid state, increment all values.
2. Any octopus wwith an energy level greater than 10 will flash, causing its cardinal and intercardinal neighbors to
increment their energy levels too, potentially causing them to flash as well.
3. Once they have all finished flashing, reset any flashed octopuses to an energy state of zero.
4. Count the number of octopuses with a current energy level of 0, since those must be the ones that just flashed.

```clojure
(defn take-turn [[grid _]]
  (let [grid' (-> grid increment-all cascade-flashes reset-flashes)
        num-flashed (->> grid' (map second) (filter zero?) count)]
    [grid' num-flashed]))
```

Note that this function makes use of three helper functions that we need to implement: `increment-all`,
`cascade-flashes`, and `reset-flashes`. Let's pick those up one at a time.

`increment-all` is a very simple function, but we're going to leverage a utility function I found online a while ago,
called `update-values`, which appliles a function `f` to the value of every key in a map. In this case, we just call
`inc`. I'll include the text of `update-values` here since it's the first time we're using it this year.

```clojure
(defn update-values
  "Thank you to Jay Fields' post for this awesome way to apply a function
  to every element of a map.
  http://blog.jayfields.com/2011/08/clojure-apply-function-to-each-value-of.html"
  [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn increment-all [grid] (utils/update-values grid inc))
```

The `cascade-flashes` function is a little more involved, as it is both multi-arity and recursive. My approach is to
take in the initial grid and a set of points that have already flashed this time around, defaulting to an empty set.
If therae is any coordinate that is currently flashing and which we haven't seen already, then follow the steps shown
above. We'll use `if-some` to detect if there is any such point, leveraging `flashing-coordinates` from above. If we
find such a point, we'll call `(point/surrounding p)` to get all eight of its surrounding neighbors, and then use
`(filter grid points)` to ensure that those neighbors are within bounds. With that list of valid neighbors, we'll
use `reduce` on the entire grid, incrementing every matching coordinate; note how clean and succinct
`(reduce #(update %1 %2 inc) grid points)` is here. Finally, we'll add the point `p` to the set of `flashed` values.
If there are no more flashing coordinates, just return the new state of the grid.

```clojure
(defn cascade-flashes
  ([grid] (cascade-flashes grid #{}))
  ([grid flashed] (if-some [p (->> grid flashing-coordinates (remove flashed) first)]
                    (recur (->> (point/surrounding p)
                                (filter grid)
                                (reduce #(update %1 %2 inc) grid)
                                (merge grid))
                           (conj flashed p))
                    grid)))
```

One thing that might look a little odd in that function is that we are using `recur` without first using `loop`. The
`recur` function "rebinds the bindings of the recursion point to the values of the expressions," which is how Clojure
can emulate tail call optimization even though Java doesn't actually support it. Clojure supports two recursion points -
functions (`fn` or `defn`) and `loop`s. Here we're making the whole `cascade-flashes` function recursive.

Finally, we get to the `reset-flashes`, updating the grid values for all flashing coordinates to 0. I originally
implemented this reusing the `update-values` function, but that seemed a bit wasteful for every coordinate that _isn't_
flashing. I'm putting both implementations here, but I think the latter is easy enough to understand and saves a few
clock cycles.

```clojure
; Solution using update-values on all values in the map.
(defn reset-flashes [grid]
  (utils/update-values grid #(if (flashes? %) 0 %)))

; Solution using a simple reduce on only the flashing coordinates - more efficient.
(defn reset-flashes [grid]
  (reduce #(assoc %1 %2 0) grid (flashing-coordinates grid)))
```

Ok, so now that `take-turn` is fully functional, I am going to create a function `octopus-flash-seq` that takes in an
input string, and returns a sequence of the number of octopuses that flashed with each generation, starting with the
original generation. We'll use `(iterate take-turn [(parse-grid input) 0])` to generate the sequence of
`[grid num-flashed]`, mapping the number of flashes out of the second element in each vector.

```clojure
(defn octopus-flash-seq [input]
  (->> (iterate take-turn [(parse-grid input) 0])
       (map second)))
```

Finally, we can write part 1! We want to add together the number of flashes for the first 100 generations, which means
to get the first 101 values, or drop the initial value and add the next 100. Either way, it's a small function.

```clojure
(defn part1 [input]
  (->> (octopus-flash-seq input)
       (take 101)
       (apply +)))
```

---

## Part 2

Well now this is a lovely treat - we have almost nothing to do! We need to find the first step during which every
octopus flashes. Luckily, the problem statement tells us there are 100 octopuses, so from the `octopus-flash-seq`, we
just need to find the first generation where flash count is 100. `keep-indexed` comes to the rescue here, since it
returns the non-nil results of calling a function `(fn [index value])` on each element in its input sequence. We use
`when` to return either the index (`%1`) or `nil` if the value isn't 100, and then we pull out the first such index.

```clojure
(defn part2 [input]
  (->> (octopus-flash-seq input)
       (keep-indexed #(when (= 100 %2) %1))
       first))
```

We're almost halfway through the Advent, and the problems are still pretty straightforward.  That means the rest of the
puzzles will be equally reasonable, right?

Right?