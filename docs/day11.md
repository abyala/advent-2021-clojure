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

---

## Second Preamble

I completed this puzzle and finished my write-up, and then I went to sleep. In the morning, I realized I had made it
far too complicated, so I've simplified it here. Rather than keep multiple copies of both the code and the write-up,
I'm just replacing the write-up. If you really want to read my original implementation (why would you?), check out the
Git history.

---

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

Next, I'm going to create a few helper functions so we don't have to keep thinking about the actual energy levels
throughout the code. `ready-to-flash?` states whether the octopus has an energy level above 9, meaning it is about to
flash. `flashed?` returns whether the octopus just flashed, which means its current energy level is 0. Then
`coordinates-ready-to-flash` and `coordinates-flashed` returns which octopuses are either ready to flash or just did
flash.

```clojure
(defn ready-to-flash? [v] (> v 9))
(def flashed? zero?)

(defn- coordinates-where [f grid] (keep (fn [[k v]] (when (f v) k)) grid))
(defn coordinates-ready-to-flash [grid] (coordinates-where ready-to-flash? grid))
(defn coordinates-flashed [grid] (coordinates-where flashed? grid))
```

On every step/turn of the program, we'll need to increment every octopus's energy level, and then check for flashes.
If an octopus flashes, it sets its energy level to 0 and causes all of its surrounding neighbors to increment their
energy levels if they hadn't flashed yet. Finally, any of those neighbors check to see if they're ready to flash too.

First, let's take care of the easy part - incrementing every octopus's energy level.  `increment-all` is a very simple
function, but we're going to leverage a utility function I found online a while ago, called `update-values`, which
applies a function `f` to the value of every key in a map. In this case, we just call `inc`. I'll include the text of
`update-values` here since it's the first time we're using it this year.

```clojure
(defn update-values
  "Thank you to Jay Fields' post for this awesome way to apply a function
  to every element of a map.
  http://blog.jayfields.com/2011/08/clojure-apply-function-to-each-value-of.html"
  [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn increment-all [grid] (utils/update-values grid inc))
```

The `cascade-flashes` function is a little more involved since it is recursive. To start, we'll check to see if there
are any points ready to flash. For that, we can get the (lazy) sequence of coordinates ready to flash, and pick the
first one off; if there are none, this will return `nil`. If we find such a point, we'll map it to its surrounding
points, and filter for only those coordinates that are on the grid; this takes care of boundary conditions. Next,
we'll remove any points that correspond to octopuses that have already flashed, since after incrementing every octopus,
only those that just flashed will have an energy level of 0. Note that `(remove (comp flashed? grid) points)` uses the
`comp` function because I think it looks cleaner than `(remove #(flashed? (grid %)) points)`. Finally, we need to
increment those neighbors and reset the current octopus to zero, and we do that with a `reduce` call. The
initialization value in this case isn't the `grid`, but rather `(assoc grid p 0)` so the chosen octopus goes down to
an energy level of 0.

```clojure
(defn cascade-flashes [grid]
  (if-some [p (->> grid coordinates-ready-to-flash first)]
    (recur (->> (point/surrounding p)
                (filter grid)
                (remove (comp flashed? grid))
                (reduce #(update %1 %2 inc) (assoc grid p 0))))
    grid))
```

One thing that might look a little odd in that function is that we are using `recur` without first using `loop`. The
`recur` function "rebinds the bindings of the recursion point to the values of the expressions," which is how Clojure
can emulate tail call optimization even though Java doesn't actually support it. Clojure supports two recursion points -
functions (`fn` or `defn`) and `loop`s. Here we're making the whole `cascade-flashes` function recursive.

So now we can implement the very simple `take-turn` function. Given a grid, it will increment all of the energy levels
and cascade all of the flashes, returning the new grid.

```clojure
(defn take-turn [grid] (-> grid increment-all cascade-flashes))
```

Ok, so now that `take-turn` is fully functional, I am going to create a function `octopus-flash-seq` that takes in an
input string, and returns a sequence of states of the grid, starting with the original generation. We'll use
`(iterate take-turn grid)` to generate the sequence of grids, which we will then map to the number of coordinates
that had just flashed. Again, `(map (comp count coordinates-flashed))` looks simpler than
`(map #(count (coordinates-flashed %)))`.

```clojure
(defn octopus-flash-seq [input]
  (->> (parse-grid input)
       (iterate take-turn)
       (map (comp count coordinates-flashed))))
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