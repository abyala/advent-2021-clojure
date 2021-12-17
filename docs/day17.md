# Day Sixteen: Packet Decoder

* [Problem statement](https://adventofcode.com/2021/day/17)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day17.clj)

---

## Preamble

There's a math solution here, but it's been years since I took calculus, and I never took discrete mathematics, which
I suspect would be needed here. That said, lying in bed last night, I came up with the equation I think we would need
to calculate the height of the probe we launch, given an initial slope and a time index, but I have no idea how to
apply it here to the problem. So it's ok; we've got some clock cycles to use on a very slightly slower calculation.

I'll admit, I think I'm missing something fundamental here which would make the solution simpler; perhaps I'll figure
it out after reading my peers' solutions. But at least I can happily say my solution is lightning fast, finishing part
2 in about 150ms.

---

## Part 1

Given a starting location, we're launching probes at an ocean trench. In my mind, we're firing angry birds at a pig
house. Either way, the goal is that given a starting position (origin) and a selected tragectory, we have to track the
motion of the probe and see if it ever hits the trench. Of all the possible launch options, we need to identify the
greatest height that the probe ever reaches.

To start, let's figure out our data structure. While I'll occassionally use an `[x y]` pair to represent a point, for
the target (ocean trench), I never really need to look at the values as pairs, so I opted for the shape of
`[min-x max-x min-y max-y]`; conveniently, that mirrors the input string.

Let's parse the input. Why bother making a parse function for a single line? I have no idea... that was a waste of time
to be sure. We'll just use a regex to pull out the four numbers, map each to an integer, and throw it all into a vector.

```clojure
(defn parse-input [input]
  (->> input
       (re-seq #"target area: x=(.*)\.\.(.*), y=(.*)\.\.(.*)")
       first rest (map parse-int) vec))
```

Next is the `progress` function, which takes in a target and a point, and says whether the point hits the target, 
misses it by going too far to the right or too low, or if it is still approaching the target. My original thought was
that I'd make corrections to the slopes I tried whether it went too far to the right or down, but I didn't actually
need it, so I stuck to a simple `:miss`.

There is one interesting piece to this function, and that's my use of the prepost map. This is a map that sits between
the argument list and the first evaluation expression, as a means of doing input or output validation. This was
Clojure's first foray in allowing validation of arguments, since it's a dynamically typed language. One could use this
to put in type checks to give it the same level of protections as typical languages like Java and Kotlin, although we
can apply much more powerful validations; [Clojure Spec](https://clojure.org/guides/spec) is even more powerful, but
I didn't feel like going there. Anyway, I wanted to put some controls on the target input, since I changed its
structure several times. Here, the `:pre` entry says I want to ensure that both the third and fourth components of the
target (the `y` values) are negative, and that the first `y` value is smaller than the second. If not, I'll get a
runtime assertion failure.

```clojure
(defn progress [target point]
  {:pre [(every? neg-int? (drop 2 target))
         (< (target 2) (target 3))]}
  (let [[tx1 tx2 ty1 ty2] target
        [px py] point]
    (cond
      (and (<= tx1 px tx2) (<= ty1 py ty2)) :hit
      (> px tx2) :too-wide
      (< py ty1) :too-low
      :else :approaching)))
```

Next, let's create a `trajectory-from-origin` function, which takes in a `[dx0 dy0]` and returns an infinite sequence
of the points the path covers. I tried being all fancy here with naming my variables `dx` and `dy` to represent the
velocities, and `dx0` and `dy0` as the original velocities; it actually made naming much simpler for me. Anyway, at
each point along the trajectory, we increment the `x` and `y` values by `dx` and `dy`, and then we adjust the
velocities. The `dx` value trends toward 0 without going negative, while the `dy` value continually decreases. Since
I know that the trench is always to the right of the origin, the change to `dx` is just `(max (dec dx 0))`; I
originally supported a negative `dx0` value, but my code was simpler without it. With that in place,
`trajectory-from-origin` uses `iterate` to generate the infinite sequence, but it only provides the `x` and `y`
values, since the caller doesn't need to see the intermediate velocities.

```clojure
(defn trajectory-from-origin [[dx0 dy0]]
  (->> [0 0 dx0 dy0]
       (iterate (fn [[x y dx dy]] [(+ x dx)
                                   (+ y dy)
                                   (max (dec dx) 0)
                                   (dec dy)]))
       (map (fn [[x y]] [x y]))))
```

While we're talking about trajectories, let's make a filter `hits-target?` which returns `true` if the trajectory has
any point that hits the target. To do this, we'll map each point of the trajectory to its `progress` value, throwing
away any initial values while we're still `:approaching` the target. Then we get the next value, which should either be
a `:hit` or `:miss`, so we return `true` if the value is a `:hit`.

```clojure
(defn hits-target? [target trajectory]
  (->> (map #(progress target %) trajectory)
       (drop-while #(= % :approaching))
       (first)
       (= :hit)))
```

Now in theory, to solve the puzzle we would look at every possible velocity of `[dx0 dy0]`, find the ones that hit the
target, and then look for the largest `y` value. While not wanting to over-optimize too badly, I did want to limit the
number of permutations I had to investigate, so I wanted to create `possible-x-in-target` and `possible-y-in-target`
functions.

Let's start with `dx0` - because the x-velocity has drag, eventually all values of `x` will be the same. If `dx0` is
too low (for example, 0), then it will reach its static value before ever hitting the target. If `dx0` is too high,
then the first value will overshoot the target, and it will never come back. So what I want to do is look at all values
for `x` starting with a particular `dx0`, and see if any of those values fall within the target. The result is a set of
potential `dx0` values.

First, we'll build a helper function `x-values`, which returns the sequence of unique `x` values from `dx0`. We'll use
the `trajectory-from-origin` function with a dummy `y` value (we don't care what it is), to get the infinite sequence
of points. We'll call `(map first)` to strip out just the `x` values, then `(partition 2 1)` to show how `x` changes
value from point to point; that's `zipWithNext` for you, Todd! Given these pairs of `x` values, we'll use 
`(take-while #(apply < %))` to only keep the values while `x` is still moving, and then use `(map second)` to bring the
pair back to individual `x` values.

Then we can build `possible-dx0` from a target.  We'll make a set called `all-target-x` for all possible values of `x`
within the target; this just cleans up our filter in a moment. Now we know from above that `dx0` must be between 1 and
the last value in the target, so that will make the range of values we need to inspect. Then we filter the potential
`dx0` values to the ones such that the `x-values` from that `dx0` hit some value within `all-target-x`.

```clojure
(defn x-values [dx] (->> (trajectory-from-origin [dx 0])
                         (map first)
                         (partition 2 1)
                         (take-while #(apply < %))
                         (map second)))

(defn possible-dx0 [target]
  (let [[min-x max-x] target
        all-target-x (set (range min-x (inc max-x)))]
    (filter #(some all-target-x (x-values %))
            (range 1 (inc max-x)))))
```

That wasn't bad, so let's do the same by making `possible-dy0`. First we'll construct `y-values`, which is easier than
`x-values` since `y` has an infinite number of ever-decreasing possible values.  So we'll grab the
`trajectory-from-origin` with a dummy `dx0` value, map out the `y` values, and skip over the initial 0 because it's not
interesting.

Then to see if a potential `dy0` hits the target, we have a bit more work since there are infinite possible values. 
We'll first construct `all-target-y` in a similar way to `all-target-x` in the `possible-dx0` function. Then we'll look
at the range of all possible `dy0` values, which we'll get to in a moment. For each potential `dy0`, we call
`(take-while #(<= min-y %) (y-values dy))` to look at all `y` values that aren't already below the minimum of the
target. Then we can call `(some all-target-y ys)` to see if any of those values `ys` are in the set of `all-target-y`.

The real question is, what's the possible range of `dy0` values to investigate? We know that the smallest value is
`min-y` of the target, since we can get into the target by firing right at it. But how high should we check?  0?  1000?
The trick is to look at the `dy` values if we choose a positive value for `dy0`. If `dy0` equals `3`, then the changing
`dy` values will be `3, 2, 1, 0, -1, -2, -3, -4, -5...`. That means we'll move into some positive values, then back
down to 0, and finally go into `-dy0 - 1` before dropping faster. So we need to ensure that `dy0` is not greater than
the opposite of `min-y`, or else the first negative `y` value will already be too low. Thus, the range of possible
`dy0` values are `(range min-y (- min-y))`.

```clojure
(defn y-values [dy] (->> (trajectory-from-origin [0 dy])
                         (map second)
                         rest))

(defn possible-dy0 [target]
  (let [[_ _ min-y max-y] target
        all-target-y (set (range min-y (inc max-y)))]
    (->> (range min-y (- min-y))
         (filter (fn [dy]
                   (let [ys (take-while #(<= min-y %) (y-values dy))]
                     (some all-target-y ys)))))))
```

Can we solve this problem yet?  Almost. First, we need to create `hit-trajectories`, which takes in a target
and returns a sequence of all trajectories that end up hitting the target. This function is now actually very simple.
We'll use a `for` to cross all values of `possible-dx0` with all values of `possible-dy0` to make a giant list of
initial velocities, which we'll call `all-velocities`. For each of these starting velocities, we map them using
`trajectory-from-origin`, and filter for the ones where `hits-target?` is true.

```clojure
(defn hit-trajectories [target]
  (let [all-x (possible-dx0 target)
        all-y (possible-dy0 target)
        all-velocities (for [x all-x, y all-y] [x y])]
    (->> (map trajectory-from-origin all-velocities)
         (filter #(hits-target? target %)))))
```

We also need a function called `apex`, which finds the largest `y` value in any given trajectory, remembering that
trajectories are infinite. For this we'll just reduce over the trajectory, keeping each value `y` so long as it is
larger than its predecessor, and using `reduced` to short-circuit the infinite sequence once the first value does not
match.

```clojure
(defn apex [trajectory]
  (reduce (fn [acc [_ y]] (if (> y acc) y (reduced acc)))
          Long/MIN_VALUE
          trajectory))
```

Ok, let's finish part 1! We'll parse the input, find all hitting trajectories, map each one to its apex `y` value,
and select the largest value in the sequence.

```clojure
(defn part1 [input] (->> input parse-input hit-trajectories (map apex) (apply max)))
```

---

## Part 2

So much work for part 1, and it's all going to pay off in part 2. We've already calculated `hit-trajectories` to find
every possible trajectory to go to the target, so let's just count them up and get our second star.

```clojure
(defn part2 [input] (->> input parse-input hit-trajectories count))
```