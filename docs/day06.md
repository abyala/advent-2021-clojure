# Day Six: Lanternfish

* [Problem statement](https://adventofcode.com/2021/day/6)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day06.clj)

---

## Preamble

This might be one of the simplest puzzles I've seen in a while.  My write-up breaks apart more succinct code into
more pieces so it's easier to read, even though this doubles the number of lines actually needed. Anyway, off we go!

---

## Part 1

We are observing lanternfish having babies. How awkward. Every fish has a timer, and on its zeroth day, it resets its
timer to 6 and creates a baby lanternfish with a timer of 8. It's the miracle of digital life.

Now the puzzle does each and every fish individually, what its timer is, and the emergence of new fish with timers of
8. I decided not to represent every fish individually, but instead just record how many fish have a certain timer on
9. each day. It turns out I wasn't punished for this when I got to part 2, so score!

My target data representation is a simple map of `{timer fish-count}`, given a single-line input string with
comma-separated starting timers. The easiest approach is to use `re-seq` to do a reg-ex extraction of every number
between the commas, map it to an integer using `parse-int`, and then call the `frequencies` function. This core
function takes in a sequence and returns a map of every value to its count within the sequence, which conveniently is
exactly what we need here! To make later code cleaner, I'll merge the input fish into a default map of `no-fish`,
where all numbers 0 through 8 are mapped to zero, such that every possible timer value is in the map. It's not strictly
necessary, but it helps.

```clojure
(def no-fish {0 0, 1 0, 2 0, 3 0, 4 0, 5 0, 6 0, 7 0, 8 0})

(defn parse-fish [input]
  (->> (re-seq #"\d" input)
       (map parse-int)
       frequencies
       (merge no-fish)))
```

Now the whole problem statement really comes down to one function - `next-generation`, but we've got a little setup to
make it simple to read. First, I define convenience constants of `delivery-timer`, `post-delivery-timer` and
`new-fish-timer`, set to 0, 6 and 8, respectively; the zero may seem like overkill, but I want my code to tell a
story. Then I make a helper function called `next-timer` that defines the next generation of a timer. Values of 0 (aka
`delivery-timer`) become 6 (aka `post-delivery-timer`), and everything else decrements.

```clojure
(def delivery-timer 0)
(def post-delivery-timer 6)
(def new-fish-timer 8)

(defn next-timer [timer]
  (if (zero? timer) post-delivery-timer (dec timer)))
```

Now we can build `next-generation` cleanly in two steps. First, starting with the `no-fish` map again, we map every
timer to its `next-timer` value, and add it to the map. Then we add in the new fish. Now the question is, why do we
_add_ the fish values to the map instead of just setting them? It's because the next generation's number of fish with
a timer of 6 is the sum of the previous generation's fish with timers of 0 and 7.

One terrific function I want to show off here is `reduce-kv`, which is another form of `reduce`. Both take in
3 arguments - a reducing function, an initial value (this is actually optional for `reduce`), and the collection to
reduce over. However, while `reduce` has a 2-arity function of `[accumulator value]`, `reduce-kv` requires the
collection to be associative, and thus its reducing function is `3-arity` or form `[accumulator key value]`. This is
effectively the same as using `(reduce (fn [accumulator [key value]] ...))`, but it's clearer to see what's going on
by using `reduce-kv`.

```clojure
(defn next-generation [fish]
  (let [deliveries (fish delivery-timer)]
    (-> (reduce-kv (fn [m k v] (update m (next-timer k) + v)) no-fish fish)
        (assoc new-fish-timer deliveries))))
```

Almost done. Before creating the `solve` function, let's define `nth-generation`, a function which takes in the day
number we want to look at, as well as the initial map of fish, and then return the map on that day. We'll just use
`iterate` invoking `next-generation`, and then use `nth` to get the correct resulting value.

```clojure
(defn nth-generation [n fish]
  (-> (iterate next-generation fish)
      (nth n)))
```

Finally, we create `solve`, which takes in both the input and the number of days. The function parses the fish, invokes
`nth-generation` to get the state on the last day, calls `vals` to get to the number of fish, and then `(apply +)` to 
add them up. And the `day1` function just calls `solve` with 80 for the number of days.

```clojure
(defn solve [input num-days]
  (->> (parse-fish input)
       (nth-generation num-days)
       vals
       (apply +)))

(defn day1 [input] (solve input 80))
```

---

## Part 2

Well now we just need to get the 256th day instead of the 80th day. I'm assuming this just asserts that our algorithms
don't contain a giant list of each and every fish, because my data set brought me into the trillians of fish. Our
algorithm can handle the extra data, no problem. One nice thing about Clojure is that it will automatically convert
Ints into Longs, or Floats into Doubles, to avoid overflow or underflow.

Anyway, here's the one-line `day2` function.

```clojure
(defn day2 [input] (solve input 256))
```

Be free, lanternfish, and procreate until we run out of water molecules in the sea!
