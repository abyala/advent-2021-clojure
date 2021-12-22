# Day Twenty-Two: Reactor Reboot

* [Problem statement](https://adventofcode.com/2021/day/22)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day22.clj)

---

## Preamble

I loved today's puzzle again!  Part 1 was really simple until I realized that the algorithm wouldn't scale into the
large dataset that was coming up in Part 2, so I had to do large rewrite. That said, I found this to be a very
enjoyable challenge.

We are given a list of reboot instructions, providing a range of cuboids with instructions on whether to set every
enclosed cube on or off. After running through all the instructions in order, we need to count the number of cubes
that are switched on. My original solution was to identify every cube within each line of instructions, converting
them into maps of the cube to `:on` or `:off`, merge the maps together, and count the number of `:on` values. But
that wasn't the final approach.

My solution looked at the puzzle a little differently from the instructions. In the instructions, we start with two
"on" lines; each line independently turned on 27 cubes, but since line 2 overlapped with line 1, it only added 19 cubes
that weren't already on from line 1. I approached this from the other perspective - the cuboid from line 1 contains
points that are all one, but the cuboid from line 1 breaks cuboid 1 into smaller cuboids that don't overlap with
cuboid 2. After breaking apart all overlapping previous cuboids, the new one either joins the set of "on" cuboids, or
it just quietly disappears because the new cuboid is off.

Take a 2-dimensional example. We start with a square of `x=1..5,y=1..5` and we overlay it with a new square of
`x=4..6,y=4..6`. Instead of breaking apart the second square, I break the first square into two smaller rectangles
which don't overlap with each other or the new rectangle. Then if the new rectangle is "on," I end up with three
rectangles in total; otherwise I end up with just two.

```
Adding an "on" square:
.....                                        11122                111   22   ###
.....                                        11122                111   22   ###
.....   plus overlay              yields     11122     which is   111   22   ###
.....                   ###                  111###               111
.....                   ###                  111###               111
                        ###                     ###
                        
Adding an "off" square:
.....                                        11122                111   22
.....                                        11122                111   22
.....   plus overlay              yields     11122     which is   111   22
.....                   ###                  111                  111
.....                   ###                  111                  111
                        ###
```

This approach lets me deal only with squares, or rather cuboids when we add the third dimension back in, without
having to deal with any irregular shapes. Plus the overlay logic doesn't care if the new square/cuboid is on or off
until after we've dealt with the overlays.

---

## Part 1

Alright, let's get down to business. Because we'l be looking at overlapping dimensions, the easiest way to represent
a cuboid would be a map of `{:x [low high], :y [low high], :z [low high]}`, and each input instruction will be of the
form `[op cuboid]`. So to parse each line of the input, we'll split the first word from the rest of the line as a
keyword, identifying each dimension on the right using a neat regular expression I saw online a few days ago:
`#"\-?\d+"`. This captures both the optional negative sign and also any number of numeric digits, and `read-string` can
properly map those values to Longs.

Note that as a matter of convention in my solution, I'll usually use `x0` and `x1` to represent `x-low` and `x-high`.
If I have two cuboids where I'm examining both of their min and max values, I'll use `x0a` and `x1a` for the first
cuboid, and `x0b` and `x1b` for the second.

```clojure
(defn parse-instruction [line]
  (let [[instruction dim-str] (str/split line #" ")
        [x0 x1 y0 y1 z0 z1] (map read-string (re-seq #"\-?\d+" dim-str))]
    [(keyword instruction) {:x [x0 x1], :y [y0 y1], :z [z0 z1]}]))
```

We know that we'll need to discard any input instruction whose cuboid lies outside the initialization area, which is
defined to the cuboids between -50 and 50. `within-initialization-area?` looks at the value range for each dimension
of a cuboid, and calls `every?` to check if the values are within the required area.

```clojure
(def dimensions [:x :y :z])

(defn within-initialization-area? [cuboid]
  (letfn [(dim-ok? [dim] (let [[v0 v1] (dim cuboid)]
                           (and (>= v0 -50) (<= v1 50))))]
    (every? dim-ok? dimensions)))
```

Next we implement another helper function called `overlap?`, which compares two cuboids either for a single dimension
or for all of them. Cuboids overlap in a dimension if each min value is not greater than the other's max value. Cuboids
overlap if all of their dimensions overlap.

```clojure
(defn overlap?
  ([cuboid1 cuboid2] (every? (partial overlap? cuboid1 cuboid2) dimensions))
  ([cuboid1 cuboid2 dim] (let [[v0a v1a] (dim cuboid1)
                               [v0b v1b] (dim cuboid2)]
                           (and (<= v0a v1b) (<= v0b v1a)))))
```

Now we get to the first of two meaty functions - `split-on-dimension`. This takes in two cuboids and a dimension, and
breaks the first cuboid apart into overlapping and non-overlapping cuboids. In my mind, non-overlapping cuboids are
"safe," because they can't be affected by the invading cuboid, while overlapping ones are "unsafe," so the function
returns a map of `{:safe cuboids, :unsafe cuboids}`. First off, if the two cuboids don't overlap in the dimension,
the first cuboid is safe, so return a map of `:safe` to the vector of the cuboid itself. If they do overlap, we
calculate `overlap0` and `overlap1` as the min and max values that overlap, recognizing that we should get the same
values if we were to flip the order of the cuboids; the `:unsafe` region will be the original cuboid with this region
set at the correct dimension. Then we look on both sides of this unsafe region to see if they're still part of the
original cuboid, and only add them in if they are.

For example, imagine we are looking in two dimensions again, and we call
`(split-on-dimension {:x [1 5], :y [10 20]} {:x [3 5], :y [1 100]} :x)`. Looking at just the `x` values, we can see
that cuboid1 has values 1 through 5 while cuboid2 has values 3 through 5. The overlap is therefore `[3 5]`, so we'll
want to return `{:unsafe [{:x [3 5], :y [10 20]}]}`. But what about the area to the left and right of it?  The left 
value would be `{:x [1 2], :y [10 20]}` which is still within the original cuboid, so that's fine. The right value
would be `{:x [6 5], :y [10 20]}` since the overlap region includes `x=6`, and this range doesn't make any sense since
the min `x` value is greater than the max, so we discard it.

```clojure
(defn split-on-dimension [cuboid1 cuboid2 dim]
  (if-not (overlap? cuboid1 cuboid2 dim)
    {:safe [cuboid1]}
    (let [[v0a v1a] (dim cuboid1)
          [v0b v1b] (dim cuboid2)
          overlap0 (max v0a v0b)
          overlap1 (min v1a v1b)
          safe-regions (filter (partial apply <=) [[v0a (dec overlap0)] [(inc overlap1) v1a]])
          overlap-region [overlap0 overlap1]]
      {:safe   (map #(assoc cuboid1 dim %) safe-regions)
       :unsafe [(assoc cuboid1 dim overlap-region)]})))
```

Hopefully that previous function made sense, because we're about to leverage it for `remove-overlaps`. Here, we again
check if the two cuboids overlap at all; if not, we just return the vector of the first cuboid, since there's no reason
to break it apart. (Note that with this check, we don't actually have to check `overlap?` in `split-on-dimension`, but
it's a cheap check to make sure the data is always valid.) If the cuboids do overlap, then we're going to use a `reduce`
over the three dimensions, looking at the set of safe and unsafe cuboids. Initially, the first cuboid is considered
unsafe since we haven't examined it over overlaps. For each dimension, we compare each of the unsafe regions to the
new cuboid, merging together all of their safe and unsafe cuboids for that dimension. If we identified any new safe
cuboids, we'll add them to collection of previously reviewed ones. However, we _replace_ the previous unsafe regions
with the new ones, since a previously unsafe region may have been split apart by `split-on-dimension`. When all is
said and done, `remove-overlaps` just returns the sequence of safe regions, as region still unsafe must fully overlap
with cuboid2.

```clojure
(defn remove-overlaps [cuboid1 cuboid2]
  (if-not (overlap? cuboid1 cuboid2)
    [cuboid1]
    (first (reduce (fn [[acc-safe acc-unsafe] dim]
                     (let [{:keys [safe unsafe]}
                           (->> acc-unsafe
                                (map #(split-on-dimension % cuboid2 dim))
                                (apply merge-with conj))]
                       [(apply conj acc-safe safe) unsafe]))
                   [() [cuboid1]]
                   dimensions))))
```

The worst is behind us now.  We'll make three small functions that should build up into running all instructions from
the input data set. First, `remove-all-overlaps` takes in a sequence of cuboids and a new cuboid, and uses `reduce` to
join together all safe regions after removing their overlaps from the new cuboid. Then `apply-instruction` takes in the
cuboids and an instruction, which again is an operation and a cuboid. After stripping away all overlaps, this function
either returns the remaining safe regions for an `:off` instruction, or adds the new cuboid to the safe regions for an
`:on` instruction. Finally, `apply-instructions` just calls `apply-instruction` on all instructions, starting with an
empty collection of safe cuboids, since initially every cube is off.

```clojure
(defn remove-all-overlaps [cuboids new-cuboid]
  (reduce (fn [acc c] (apply conj acc (remove-overlaps c new-cuboid)))
          ()
          cuboids))

(defn apply-instruction [cuboids [op new-cuboid]]
  (let [remaining (remove-all-overlaps cuboids new-cuboid)]
    (if (= op :on)
      (conj remaining new-cuboid)
      remaining)))

(defn apply-instructions [instructions]
  (reduce apply-instruction () instructions))
```

Almost done! Once we've run through all instructions, we need to know how many cubes are on. For this, the
`cuboid-size` function looks at each dimension within a cuboid, multiplying their lengths together. Remember that all
dimensions in this problem are inclusive on both ends, so the length of `[4 6]` is 3, not 2. 

```clojure
(defn cuboid-size [cuboid]
  (->> (map (fn [dim] (let [[v0 v1] (cuboid dim)]
                        (inc (- v1 v0)))) dimensions)
       (apply *)))
```

Alright, let's finish up with the `part1` function already! We'll read each line of the input, and map it to its
parsed instruction. We then need to filter out the ones whose cuboids are within the initialization area. Then with
only valid instructions, we'll apply them all together, map the size of each resulting cuboid, and add them together.
Done!

```clojure
(defn part1 [input]
  (->> (str/split-lines input)
       (map parse-instruction)
       (filter #(within-initialization-area? (second %)))
       apply-instructions
       (map cuboid-size)
       (apply +)))
```

---

## Part 2

Ok, so part 2 is the same as part 1, except that we can use all cuboids, not just the ones in the initialization area.
So we'll just pull out most of the logic from `part1` into a `solve` function, which gets invoked with the input data
and a filter function to apply to the initial cuboids. For part 1, we'll pass in the `within-initialization-area?`
function again, while for part 2 we can ust `identity` to keep all of the input.

```clojure
(defn solve [instruction-filter input]
  (->> (str/split-lines input)
       (map parse-instruction)
       (filter #(instruction-filter (second %)))
       apply-instructions
       (map cuboid-size)
       (apply +)))

(defn part1 [input] (solve within-initialization-area? input))
(defn part2 [input] (solve identity input))
```

That's it!  Pretty fun little puzzle today.