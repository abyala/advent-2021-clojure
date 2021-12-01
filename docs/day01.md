# Day One: Sonar Sweep

* [Problem statement](https://adventofcode.com/2021/day/1)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day01.clj)

---

## Part 1

The problem starts with us receiving an input string of one integer per line, which we need to turn into a sequence
of integers. We solve this with a simple parsing function, `parse-measurements`, which splits the input by each line,
and then maps each individual String using the Java function `Integer.parseInt` within a lambda call.

```clojure
(defn parse-measurements [input]
  (->> (str/split-lines input)
       (map #(Integer/parseInt %))))
```

The problem needs us to count up the number of times a measurement is greater than the previous measurement. To do this,
we use the `partition` function to create sequences of length 2 with a step of 1, such that we get every adjacent pair.
This means that `(partition 2 1 (range 4))` would convert `(0 1 2 3)` to `((0 1) (1 2) (2 3))`. After that, we apply a
filter for the appropriate pairs, and count them up.

I've shown two ways to implement the filter. In the first instance, I destructured the pair into `[a b]` and then
simply checked the predicate `(< a b)`. However, even though that syntax works, I thought a partial function would look
nicer, since all we need to check is that "each" value in the pair is less than the next one. Thus the function
`(partial apply <)` does the same thing without an explicit lambda. The `partial` function says "run this against all
subsequent values you provide as arguments," which again in this case is a single sequence with two values. The
`apply` function says to call the `<` function on each value _within_ the sequence. 

```clojure
; Original implementation
(defn part1 [input]
  (->> (parse-measurements input)
       (partition 2 1)
       (filter (fn [[a b]] (< a b)))
       count))

; Cleaner implementation
(defn part1 [input]
  (->> (parse-measurements input)
       (partition 2 1)
       (filter (partial apply <))
       count))
```

Easy enough. Let's see what part 2 brings us!

---

## Part 2

This part of the puzzle is very similar to the first, except that instead of checking for increasing values in the
measurements themselves, we need to look for increasing values in the rolling sum of three values. There's really
nothing to this, if we copy-paste part 1 and make a tiny change.

After we parse the measurements, we need to make a new rolling partition of size 3 and step 1, and then add those
values up. Once we're done and we have a new sequence of integers, we again partition our pairs and do our filter.
I use another partial function, this time mapping each three-tuple to the sum of its values, using
`(map (partial apply +) sequence-of-three-tuples)`.  Put it together, and we have a working solution.

```clojure
(defn part2 [input]
  (->> (parse-measurements input)
       (partition 3 1)
       (map (partial apply +))
       (partition 2 1)
       (filter (partial apply <))
       count))
```

---

## Cleanup

Ok, that worked just fine, but let's do a little cleanup to make things nicer. First of all, while we wait for the
next GA version of Clojure that introduces [CLJ-2667](https://clojure.atlassian.net/browse/CLJ-2667), I really hate
having to call `(map #(Integer/parseInt %) sequence)` to get to the Java static method
`Integer.parseInt`. It's just ugly. So I created a utils namespace to define a nice `parse-int` function to use.

```clojure
(defn parse-int [s] (Integer/parseInt s))
```

And then, even though it's sort of bad form, I `refer` to it in my `day01` namespace. It should be in the
`clojure.core` namespace and we all know it, so this is me sticking it to the man. Take that, nobody caring at all!

```clojure
(defn parse-measurements [input]
  (->> (str/split-lines input)
       (map parse-int)))
```

Next, let's address the "issue" where parts 1 and 2 are so very similar. In theory, here's what the algorithm should
look like:
1. Parse the input string into a sequence of integers for each measurement.
2. Transform that numeric sequence into a different numeric sequence, based on what each part requires.
    1. For part 1, no transformation is necessary.
    2. For part 2, transform that sequence into the sum of the three-measurement sliding window.
3. Count the number of adjancent pairs with increasing values.

Piece of cake. Starting with part 1, we'll make our unified `solve` function, and have `part1` send it the input
string and the `identity` function, so the transformation does nothing. For part 2, we'll make a simple function
`three-measurement-sliding-window` to create the sliding window, and then send it along.

```clojure
(defn solve [input mapping]
  (->> (parse-measurements input)
       (mapping)
       (partition 2 1)
       (filter (partial apply <))
       count))

(defn three-measurement-sliding-window [measurements]
  (->> (partition 3 1 measurements)
       (map (partial apply +))))

(defn part1 [input] (solve input identity))
(defn part2 [input] (solve input three-measurement-sliding-window))
```

There we go, all clean. On to day 2!