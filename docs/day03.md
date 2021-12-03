# Day Three: Binary Diagnostic

* [Problem statement](https://adventofcode.com/2021/day/3)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day03.clj)

---

## Part 1

Today's session was another fun one, this time playing with binary numbers. The problem says to read a list of binary
numbers and do binary calculations on the values. It defines two calculations - the gamma rate and epsilon rate. The
gamma rate is obtained by determining whether for each bit position, whether most numbers have a 1 instead of a 0, and
then combining those bit values into a new number. The epsilon rate is the opposite - combining the least common value
for each bit position. We multiple gamma and epsilon together to get the answer.

Let's start with parsing. We need to read each line and convert it into a number by reading it as binary. Note that
my original solution involved working with strings and characters, but that felt ugly. I created a `parse-binary`
function in my `utils` namespace, which leverages the `java.lang.Integer/parseInt` method. Leveraging that makes
for a simple `parse-nums` function; once again I am choosing to `refer` this function in since I find it to be
foundational.

```clojure
; utils namespace
(defn parse-binary [s] (Integer/parseInt s 2))

; advent-2021-clojure.day03 namespace
(defn parse-nums [input] (->> input str/split-lines (map parse-binary)))
```

I'm going to need to know the most significant bit across all of the numbers. I could go back to the input Strings and
just pick the length of the largest (or first) value, but I don't want to think about those Strings after I parse the
input. So I define `most-significant-bit`, which takes in a sequence of numbers and returns the most significant bit
value; for `[2 5 9]` it should return `3` since `01001` represents `9` and it has four significant bits, starting from
zero. Using an infinite sequence of values from `(range)`, I take the largest value where `2^x` is not greater than the
largest value.

Having written that out in text, it sure looks like we're talking about applying the natural log, where
`log2(max_value) = x`. We don't need the actual natural log, just the integer portion of it, so we can simplify this
function by replacing the original `take-while` with some math functions.

```clojure
; Functional solution
(defn most-significant-bit [nums]
  (let [n (apply max nums)]
    (->> (range)
         (take-while #(>= n (Math/pow 2 %)))
         last)))

; Mathematical solution
(defn most-significant-bit [nums]
  (let [n (apply max nums)]
    (int (/ (Math/log n) (Math/log 2)))))
```

Now for the meat of the solution. For a given bit position, I want to know whether 1 or 0 is more common. I thought
about grouping each of the numbers by its bit value, calling `frequencies`, finding the largest group, and returning
the bit value, but I didn't like how it looked. So instead, I opted to use a reducing function which measures the
relative number of 1s vs 0s. Starting with an aggregation of 0, for each number in the sequence, I call `bit-test` to
return `true` if the value at that bit index is 1, or `false` if it's 0. If th `bit-test` is a 1, i increment the
aggregate counter; else for a 0 I decrement it.

With that in hand, it becomes trivial to calculate `most-common-bit` and `least-common-bit` by testing if the counter
is negative. For `most-common-bit`, a negative value means there are more 0s than 1s, so we return a 0 else 1. For
`least-common-bit`, a negative value means that there are more 0s than 1s, so we return 1 (it's less prevalent) else 0. 

```clojure
(defn bit-diff [nums bit]
  (reduce #(if (bit-test %2 bit) (inc %1) (dec %1))
          0
          nums))

(defn most-common-bit [nums bit] (if (neg-int? (bit-diff nums bit)) 0 1))
(defn least-common-bit [nums bit] (if (neg-int? (bit-diff nums bit)) 1 0))
```

Now let's calculate the gamma rate. We'll go through each bit from most significant to least, finding
the most common bit at each position across all the numbers. Then we combine that into a string and
convert it into binary. Note that I use `(range (most-significant-bit-nums) -1 -1)` to start from the
largest bit, moving toward zero (the first -1 is the exclusive max), with a step of -1. That's a
little ugly, so let's make a helper function called `range-down-from` to clean it up.

```clojure
(defn range-down-from [n] (range n -1 -1))

(defn gamma-rate [nums]
  (->> (range-down-from (most-significant-bit nums))
       (map (partial most-common-bit nums))
       (apply str)
       parse-binary))
```

Now there are two ways to calculate the epsilon rate -- either flip all of the bits of the gamma
rate, or just recalculate it using the least significant bits instead of the most significant. Well
that's easy, and it lets us work with epsilon rate even if we weren't going to play with gamma anymore.

```clojure
(defn epsilon-rate [nums]
    (->> (range-down-from (most-significant-bit nums))
         (map (partial least-common-bit nums))
         (apply str)
         parse-binary))
```

Uh oh!  That's an unnecessary copy-paste job, and I just won't stand for it. Let's refactor that into
a function called `one-pass-bit-check`, which takes in the numbers and the bitwise calculation to
apply at each position. `gamma-rate` and `epsilon-rate` just have to call `one-pass-bit-check` with
our `most-common-bit` and `least-common-bit` functions we already created. How convenient!

```clojure
(defn one-pass-bit-check [nums bit-fn]
  (->> (range-down-from (most-significant-bit nums))
       (map (partial bit-fn nums))
       (apply str)
       parse-binary))

(defn gamma-rate [nums] (one-pass-bit-check nums most-common-bit))
(defn epsilon-rate [nums] (one-pass-bit-check nums least-common-bit))
```

Ok, let's finish it up!  For part 1, we just parse the numbers, use `juxt` to calculate both the
gamma and epsilon rates, and multiply them together.

```clojure
(defn part1 [input]
  (->> (parse-nums input)
       ((juxt gamma-rate epsilon-rate))
       (apply *)))
```

---

## Part 2

Now we need to calculate the oxygen generation and CO2 scrubbing rates too, and boy do they look
similar to each other. But let's give it a moment before we get there. Unlike the gamma rate, the
oxygen generation rate requires keeping all numbers that have the most common most significant digit,
and then do the same with all _remaining_ numbers on the second digit, until only one number remains.
This means that we don't calculate the most common bits for all of the numbers up front, but rather
only once per iteration.

We can solve this with a simple recursive function. Starting with all numbers and the most significant
bit, we loop until there's only one "remaining" number. If we don't find it, then calculate the most
common bit at the current `bit` position, and create a `test` binding to whether we expect it to be a 1.
Then we filter all of the remaining numbers to those that have the correct value at that bit. We
use the `bit-test` function, which returns `true` if the number has a 1 at the relevant bit value.
Then we recurse with a decremented bit index, signifying a less significant bit. Again, this is a
place where using sets might have made more sense, but this worked for me.

```clojure
(defn oxygen-generator-rating [nums]
    (loop [remaining nums, bit (most-significant-bit nums)]
      (if (= 1 (count remaining))
        (-> remaining first)
        (let [test (= 1 (most-common-bit remaining bit))]
          (recur (filter #(= test (bit-test % bit)) remaining)
                 (dec bit))))))
```

We can see already that the CO2 scrubbing algorithm will be the same, so let's refactor it now into
a common function called `multi-pass-bit-check`.  Again we'll pass in a `bit-fn` to capture the
most common bit for oxygen, or the least common bit for CO2, and we're off to the races.

```clojure
(defn multi-pass-bit-check [nums bit-fn]
  (loop [remaining nums, bit (most-significant-bit nums)]
    (if (= 1 (count remaining))
      (first remaining)
      (let [test (= 1 (bit-fn remaining bit))]
        (recur (filter #(= test (bit-test % bit)) remaining)
               (dec bit))))))

(defn oxygen-generator-rating [nums] (multi-pass-bit-check nums most-common-bit))
(defn co2-scrubber-rating [nums] (multi-pass-bit-check nums least-common-bit))
```

Now I've been trying to use less recursion in this year's AoC, so it's possible to rewrite
`multi-pass-bit-check` using a `reduce` function instead of `loop-recur`. The only thing that made
this a tiny bit tricky is that I can't always depend on the `reduced` function pulling out the single
value from the sequence, since the `reduce` function might need to process the entire range of bits
without a short-circuit. I could handle this by using a range that goes to the imaginary bit at -1, but
that's ugly.  So instead I wrapped the `reduce` function in a `first`, which will apply to either the
`reduced` short-circuit or the final reduction.

```clojure
(defn multi-pass-bit-check [nums bit-fn]
  (first (reduce (fn [remaining bit] (if (= 1 (count remaining))
                                       (reduced remaining)
                                       (let [test (= 1 (bit-fn remaining bit))]
                                         (filter #(= test (bit-test % bit)) remaining))))
                 nums
                 (range-down-from (most-significant-bit nums)))))
```

Before writing part 2, it's clear that we are going to do the same thing as we did in part 1 -- parse
the input, apply a couple of functions to the number, and multiply the results. So we can extract that
into a `multiply-rates` function which takes in the input string and any number of rates, and multiplies
the result of each value.  Note that I'm creating a variatic function, meaning one that takes in any
number of arguments. The ampersand in the function argument list of `[input & rate-fns]` will put all
subsequent values into a sequence.

```clojure
(defn multiply-rates [input & rate-fns]
  (->> (parse-nums input)
       ((apply juxt rate-fns))
       (apply *)))

(defn part1 [input] (multiply-rates input gamma-rate epsilon-rate))
(defn part2 [input] (multiply-rates input oxygen-generator-rating co2-scrubber-rating))

```

That's it! This right here is what I adore about clojure and its convenient function composition.
It brings a tear to my eye to see how lovely this language is.

```clojure
(defn gamma-rate [nums]              (one-pass-bit-check nums most-common-bit))
(defn epsilon-rate [nums]            (one-pass-bit-check nums least-common-bit))
(defn oxygen-generator-rating [nums] (multi-pass-bit-check nums most-common-bit))
(defn co2-scrubber-rating [nums]     (multi-pass-bit-check nums least-common-bit))
```
