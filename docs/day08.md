# Day Eight: Seven Segment Search

* [Problem statement](https://adventofcode.com/2021/day/8)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day08.clj)

---

## Part 1

Today's puzzle brought me back to my high school days, and the old clock radio I had beside my bed. We're reading
jumbled up values on a numeric display, where our goal is to determine which strings refer to which numbers on the
display.  Let's do it.

First, let's check out the input. We get a sequence of lines, where each line has two groups of space-separated
strings, each group separated by a pipe. Each letter within a string represents one of the seven segments of on the
display, so we actually care about the set of letters within each string, rather than the string itself. Thus we will
make three small parse functions that fit together:

1. `parse-component` will take in one half of a line (space-separated strings), and convert them into a sequence of
sets of letters; in future, we will refer to each letter as a _signal_. Thus the string `abc ad` becomes
`(#{\a \b \c} #{\a \d})`.
2. `parse-line` splits an entire line by the pipe symbol, and returns a two-element sequence of parsed components; in
future, we will refer to the first element as the signal pattern or pattern, and the second element as the output value
or output. Thus the line `abc ad | a bc` would become `((#{\a \b \c} #{\a \d}) (#{\a} #{\b \c))`.
3. `parse-input` just calls `parse-line` for each line. The final data structure is a sequence of sequences of patterns
and outputs. More explicitly, the data structure is a sequence of two-element sequences, of sequences of sets of
characters. It's best not to sound such things out because it's much easier to work with this data than explicitly
define its type.

```clojure
(defn parse-component [component]
  (map set (str/split component #" ")))

(defn parse-line [line]
  (->> (str/split line #"\|")
       (map (comp parse-component str/trim))))

(defn parse-input [input]
  (map parse-line (str/split-lines input)))
```

For part one, we need to count the number of outputs that correspond to the digit 1, 4, 7, or 8. These digits are
special because each is the only digit with that number of segments - 1 has two digits, 4 has four, 7 has three, and
8 and seven. So what we'll do first is define `unique-lengths` as the set of lengths that correspond to a single digit,
this being `#{2, 3, 4, 7}`. Then we'll parse the input into each line, pull together all of the digits together,
filter out only the ones whose length is one of the `unique-lengths`, and count them up. `mapcat` is Clojure's
implementation of `flatmap`. Thus in one step it will `map` through every line to its digits by using the `second`
function (remember that `first` are the patterns and `seconds` are the digits), and then it will `cat` all of those
sets together.

```clojure
(def unique-lengths #{2 3 4 7})
(defn part1 [input]
  (->> (parse-input input)
       (mapcat second)
       (filter #(-> % count unique-lengths))
       count))
```

I'll make a quick note here about Clojure's approach to sets and maps. Both of these data structures can act as
functions over their elements.  So `(#{:a :b :c} :b)` and `(:b #{:a :b :c})` both mean "return `:b` if it's an
element of the set `#{:a :b :c}`, or else return `nil`." In this case, it will return `:b`, which is itself a truthy
value. So you can do cool things like `(if (#{:a :b :c} :b) "present" "absent")` to use the set itself as a predicate.
Even cooler, you can say `(filter #{:a :b} [:a :b :c :d :e])`, which returns `(:a :b)`, because `filter` will only
return the values in the collection that appear within the set. Really cool.

So in `part1`, after we call `mapcat` we have a sequence of sets of characters. The `filter` function takes each set
of characters, calls `count` to get the number of characters within the set, and calls `(unique-lengths n)` where the
former is a set. The collection `(#{:a :b} #{:a} #{:a :b :c :d})` would come back as `(#{:a :b} #{:a :b :c :d})`
because the strings have lengths of 2, 1, and 4, and only 2 and 4 are in the `unique-lengths` set.

---

## Part 2

Part 2 was neat because the code itself wasn't very complex, but this was a pure logic puzzle. For each line of
patterns and outputs, we need to determine which character corresponds to which segment of the clock, then use that to
map each of the outputs into their digits.  Each row has four digits, so convert each row into its four-digit number,
and add them all together. We don't actually care which _individual_ letter corresponds to which segment; it's more
important to know which _set_ of letters correspond to which numeric digit, and that's actually not too bad.

Let's ignore the code for now, and just work through the problem logically.
* 1, 4, 7, and 8 are easy enough to spot, because each of their patterns have a unique number of letters.
* We then move on to 0, 6, and 9, because those three each have six letters.
  * Both 0 and 9 have both of the letters on the right-hand side of the digit, which combined make up the digit 1.
    Thus, the 6 is the word within 0, 6, and 9 which does _not_ have all of the letters that the 1 does.
  * Similarly, 9 is the only number that has all of the segments that the 4 has, so we can pick that out.
  * Of the three of these, the one that isn't the 6 or the 9 is the 0.
* Then we move on to the last three numbers, 2, 3, and 5, which each have a length of 5 letters.
  * We know which letters make up the 6, and 5 is the only digit that is a distinct subset of the 6, as they only
    differ in the bottom-left segment.
  * Similarly, 3 is the only digit that has both segments that make up the 1.
  * The last string in the collection must be the 2.

Once we have that all identified, the code to implement `identify-signals` is pretty easy. Purely for the sake of
clarity, I used `letfn` to make four helper functions, so we think in business terms.
* `only-subset` takes in a set of letters and a collection of patterns, and returns the one and only pattern where
  where the letters are a subset of the pattern.
* `only-superset` does the same thing, except that the first argument is a superset of the pattern.
* `only-not-subset` returns the only pattern where the first argument is _not_ a subset.
* `leftover` just returns the only element of the collection which is not in the set of strings in the first argument.

Thus armed, we call `(group-by count signal-patterns)` to create a map of `{pattern-length (patterns)}`, and bind
each digit as we work through the problem.  At the end, we return a nice map of each set of strings to its numeric
digit.

```clojure
(defn identify-signals [signal-patterns]
  (letfn [(only-subset [s coll] (first (filter (partial set/subset? s) coll)))
          (only-superset [s coll] (first (filter (partial set/superset? s) coll)))
          (only-not-subset [s coll] (first (remove (partial set/subset? s) coll)))
          (leftover [vals coll] (first (remove vals coll)))]
    (let [patterns (group-by count signal-patterns)
          ; Unique lengths
          one (first (patterns 2))
          four (first (patterns 4))
          seven (first (patterns 3))
          eight (first (patterns 7))

          ; 0, 6, and 9 all have length of 6
          zero-six-nine (patterns 6)
          six (only-not-subset one zero-six-nine)
          nine (only-subset four zero-six-nine)
          zero (leftover #{six nine} zero-six-nine)

          ; 2, 3, and 5 all have length of 5
          two-three-five (patterns 5)
          five (only-superset six two-three-five)
          three (only-subset one two-three-five)
          two (leftover #{three five} two-three-five)]
      {zero 0, one 1, two 2, three 3, four 4, five 5, six 6, seven 7, eight 8, nine 9})))
```

It's smooth sailing from here. We'll make a `find-digits` function that takes in the signal map from above and the
list of output strings, and returns the numeric digit value. For this, we start with `(map signals outputs)`, which
this time uses a map as a function instead of a set. So `(map {:a 1, :b 2, :c 3} [:a :c])` yields `(1 3)`. After we
use the `map` function to apply the `signals` map to the output sequence, we get a sequence of ints, so we put them
into a string and parse them into a new integer to get the actual numeric value.

```clojure
(defn find-digits [signals outputs]
  (->> (map signals outputs)
       (apply str)
       (parse-int)))
```

Finally, we write the `part2` function. Here we'll parse the input, identify the signals from the patterns, feed them
in to `find-digits` with the `outputs`, and add together the results.  See?  That wasn't so bad!

```clojure
(defn part2 [input]
  (->> (parse-input input)
       (map (fn [[patterns outputs]] (-> (identify-signals patterns)
                                         (find-digits outputs))))
       (apply +)))
```