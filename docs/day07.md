# Day Seven: The Treachery of Whales

* [Problem statement](https://adventofcode.com/2021/day/7)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day07.clj)

---

## Part 1

In a fight between giant whales and an army of crabs in submarines, always go with the crabs. That is the lessons for
the day.

A bunch of crabs are all spread out in a line, and we need to find the cheapest way to get them to come together on
a single spot. Each time a crab moves one space, it costs one piece of fuel, so we need to sum up the total number of
steps needs to get everyone aligned, no matter what space they end up on.

Alright, let's do the parsing. It's almost not worth mentioning anymore, but here's how we parse a comma-separated
String into a sequence of integers. We've done this multiple times already.

```clojure
(defn parse-crabs [input]
  (map parse-int (str/split input #",")))
```

Now the plan is to pick a point on the line, and calculate the total fuel spent to get every crab on that point.  So
we'll make a `distances-to` function that maps the absolute value of the difference between two points, and sums them
up. Now call me petty, but I really prefer to call Clojure functions over Java functions because it just looks so much
nicer, and because it makes the typecasting nicer to look at. 

```clojure
; advent-2021-clojure.utils namespace
(defn abs [^long n] (Math/abs n))

; advent-2021-clojure.day07 namespace
(defn distances-to [crabs pos]
  (->> crabs
       (map #(utils/abs (- % pos)))
       (apply +)))
```

Ok, it's time to write the `part1` function already! After we parse the input, we're going to pick the min and max
crab values, since that will define the range of values we'll want to calculate with `distances-to`. Then we do the
calculations for each value in the range, and then extract the minimum to get to the answer.

```clojure
(defn part1 [input]
  (let [crabs (parse-crabs input)
        [first-crab last-crab] (apply (juxt min max) crabs)]
    (->> (range first-crab (inc last-crab))
         (map (partial distances-to crabs))
         (apply min))))
```

Onward!

---

## Part 2

This part is pretty much the same as the first part, except that the cost of moving crabs increases according to the
summation function. (Thank you to coworker Matt for reminding me that this was a summation and not a factorial!) And
thank you to high school math for teaching me that `(summation n) = (n * (n + 1)) / 2`, so I'll throw that
into a `summation` function in the `utils` namespace again.

```clojure
; advent-2021-clojure.utils namespace
(defn summation [n]
  (-> (* n (inc n))
      (/ 2)))
```

So now we need to have a new calculation function, `summation-distances-to`, which sums up the summation distance from
each crab to a given position. Hmm... that looks awfully similar to the `distances-to` function. Is there a
refactoring in our future?

```clojure
(defn summation-distances-to [crabs pos]
  (->> crabs
       (map #(summation (utils/abs (- % pos))))
       (apply +)))
```

Well we can write the `part2` function. And oh look - it's a copy-paste job from the `part1` function, except that
we are calling `summation-distances-to` instead of `distances-to`.

```clojure
(defn part2 [input]
  (let [crabs (parse-crabs input)
        [first-crab last-crab] (apply (juxt min max) crabs)]
    (->> (range first-crab (inc last-crab))
         (map (partial summation-distances-to crabs))
         (apply min))))
```

---

## Refactor!

Alright, I can begrudgingly accept one copy-paste job, but not two. So let's make this cleaner.

The first approach we can take is having a `solve` function where we pass in either `distances-to` or
`summation-distances-to`.

```clojure
(defn distances-to [crabs pos] ...)
(defn summation-distances-to [crabs pos] ...)

(defn solve [f input]
  (let [crabs (parse-crabs input)
        [first-crab last-crab] (apply (juxt min max) crabs)]
    (->> (range first-crab (inc last-crab))
         (map (partial f crabs))
         (apply min))))
  
(defn part1 [input] (solve distances-to input))
(defn part2 [input] (solve summation-distances-to input))
```

That's a good start, since it combines shared logic between `part1` and `part2`, but we still have code duplication
between `distances-to` and `summation-distances-to`, so we aren't done yet. What we really want to do is make a
common function called `total-crab-distances-to`, which will sum up the distances from every crab to a position,
applying the input function `f` to each distance. For part 1, we don't need to mutate the distances in any way, so
we'll pass in `identity`.  For part 2, we need the summation value of the distance, so we'll pass in 
`utils/summation`. Then the `solve` function just needs to pass that function `f` through to `total-crab-distance-to`
to get to the answer.

```clojure
(defn total-crab-distance-to [f crabs pos]
  (->> crabs
       (map #(-> % (- pos) utils/abs f))
       (apply +)))

(defn solve [f input]
  (let [crabs (parse-crabs input)
        [first-crab last-crab] (apply (juxt min max) crabs)]
    (->> (range first-crab (inc last-crab))
         (map (partial total-crab-distance-to f crabs))
         (apply min))))

(defn part1 [input] (solve identity input))
(defn part2 [input] (solve utils/summation input))
```
