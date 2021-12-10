# Day Ten: Syntax Scoring

* [Problem statement](https://adventofcode.com/2021/day/10)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day10.clj)

---

## Part 1

In this puzzle, we are given several lines of strings, where they all have one of two syntax errors - either they are
_incomplete_ or they are _corrupted_. Apparently on our submarine, we have an ongoing bug bounty program, so we're
going to get paid (in points) to fix the bad lines! In part 1, we need to examine only the corrupted lines, find the
first corrupt character, and assign it to its point bounty, and add up our point total.

Good news for today - we're not going to do up-front parsing, since we just need the list of strings, one from each
line. So instead, let's focus on the meat of the problem - a `process-line` function that will return the first corrupt
character _if_ the line is corrupt. Before we jump in, let's review a few Clojure concepts.

First, let's look at destructuring. Clojure lets us take an expression and assign it to a binding in a few places, but
the most common are within function arguments and within `let` expressions. Destructuring allows a developer to
immediately break apart a data structure into its representative pieces, only assigning a binding to the entire value
if desired. So for a function `min-and-max` that returns a two-element vector from an input collection, there are
lots of options:
* `(let [results (min-and-max coll)])` - typical function invocation, with a single binding for the entire result.
* `(let [[a b] (min-and-max coll)])` - destructures the result of the function call into the first two values, binding
them to `a` and `b` respectively, or else `nil` if there are no such values.
* `(let [[a] (min-and-max coll)])` - destructures the collection, but only binds the first value, no matter whether or
not there are any more.
* `(let [[a b & the-rest] (min-and-max coll)])` - destructures the first two values and assigns them bindings of `a`
and `b`, and binds `the-rest` to a collection of remaining values, if any.
* `(let [[a b :as results] (min-and-max coll)])` - destructures the first two values and assigns them to bindings of
`a` and `b`, and also binds the entire result including the first two values, to the binding `results`.

There are lots of super powers in Clojure destructuring, but that's enough for now.

Second, let's look at lists as stacks. Clojure, being a LISP, is based on tons of lists everywhere, including in many
lazy sequences from the core library. But when it comes to manually constructing new data objects, as developers we
seldom use lists directly; vectors, maps, and sets are much more comfortable for direct usage. It's probably because
we normally think of appending values to the end of a vector, rather than inserting them into the front of a list, at
least when order matters. That said, lists make perfect stacks. `cons` or `conj` work like `push` on a stack, putting
values on "top." And `rest` or `next` work like `pop`, removing the top of the stack and revealing what's underneath.

So now that we've had that little lecture, let's get back to `process-line`, where we take in one of our failing lines
and return the problem, although for now we only know how to handle corrupt inputs. The function will loop through
all characters and a stack/list of close-delimiters we need to see in order. For each character:
* If the character is one of the four open-delimiters `([{<`, then the syntax is still fine. Figure out its matching
close delimiter, and push that onto the stack while recurring through the loop.
* If the character isn't an open delimiter, check to see if it matches the head of the list (top of the stack). If so,
this character correctly completed a chunk, so continue through the loop by popping the delimiter off the stack.
* Otherwise, if this is neither an open delimiter or the correct close delimiter, it's the corrupt character. Return
`[:corrupt c]` because we know there will other syntax errors to process later, so let's capture both the type and the
data.

```clojure
(def close-delimiters {\( \), \[ \], \{ \}, \< \>})
(def delimiters (set (keys close-delimiters)))

(defn process-line [line]
  (loop [[c & xc] line, [s & xs :as stack] ()]
    (when c
      (cond (delimiters c) (recur xc (conj stack (close-delimiters c))) ; push
            (= c s)        (recur xc xs)                                ; pop
            :else          [:corrupt c]))))                             ; error
```

Note we use a lot of destructuring in that function for clarity sake. As we go character-by-character through the line,
we destructure the list of characters as `[c & xc]`, where `c` is the next letter in the line, and `xc` is the sequence
of remaining characters. We'll use `c` to inspect the current letter, and use `xc` to recur through the loop. Then for
the stack, we'll destructure it using `[s & xs :as stack]`, because we need lots of data from it. We'll use `s` to
see if the character `c` matches it as a close delimiter. We'll use `xs` to pop the close delimiter if we complete
a chunk. And we'll use the entire `stack` when we need to push the next delimiter onto the top of the stack.

Another cool trick here is we can say `(when c ...)`, because if the list is empty, `[c & xc]` will destructure into 
two `nil` values, and `nil` is falsey for `if` and `when` expressions.  If we go through the entire string and don't
find a corrupt character, for lack of knowing what to do yet, we'll just return `nil`, which is why we use `when`
instead of `if` here.

Let's move ahead. We know that not every line is going to be corrupt, so let's make a function `first-corrupt-char`
that takes in a string and returns the first corrupt character if and only if that line is corrupt. Destructuring and
`when` make this a snap - we'll call `process-line` on the string, pull out the two elements of the results, and return
the second result if the first is `:corrupt`.

```clojure
(defn first-corrupt-char [line]
  (let [[status c] (process-line line)]
    (when (= status :corrupt) c)))
```

We can write the `part1` function now, and it reads just how we want. We'll parse the input string into each line,
call `(keep first-corrupt-char)` to map the value to its corrupt character while discarding `nil`s, then map each value
to the number of points we get for that corrupt character, and add up our winnings.

```clojure
(def corrupt-char-points {\) 3, \] 57, \} 1197, \> 25137})

(defn part1 [input]
  (->> (str/split-lines input)
       (keep first-corrupt-char)
       (map corrupt-char-points)
       (apply +)))
```

Moving on!

---

## Part 2

We knew we'd be coming back to _incomplete_ lines, also now with a more complex point system. But the code is all ready
to go.

For incomplete lines, we need to know which close delimiters we would need to add to the line in order for all chunks
to complete. Conveniently for us, we've got that data sitting and waiting for us in the `stack` binding, at the point
that `process-line` runs out of characters. So we'll need to change `process-line` to use an `if` instead of `when`,
and to return `[:incomplete stack]` if it runs out of characters before reading a corrupt character.

```clojure
(defn process-line [line]
  (loop [[c & xc] line, [s & xs :as stack] ()]
    (if c
      (cond (delimiters c) (recur xc (conj stack (close-delimiters c)))
            (= c s) (recur xc xs)
            :else [:corrupt c])
      [:incomplete stack])))
```

Then we need a function `missing-chars` that's similar to the `first-corrupt-char` from part 1, where we process a line
and return its missing characters only if the status is `:incomplete`. It'll only take a second to extract out the
common code into an `error-chars` function and refactor `first-corrupt-char`, so let's do that.

```clojure
(defn- error-chars [error line]
  (let [[status c] (process-line line)]
    (when (= status error) c)))

(defn first-corrupt-char [line] (error-chars :corrupt line))
(defn missing-chars [line]      (error-chars :incomplete line))
```

Now we need to handle the bounty points for incomplete characters. The problem says that for each incomplete character,
we multiple the points accumulated so far by 5, then add in a new point mapping for the close character. That sounds
like the `reduce` function to me.

```clojure
(def autocomplete-char-points {\) 1, \] 2, \} 3, \> 4})

(defn incomplete-char-bounty [chars]
  (reduce (fn [acc c] (+ (* acc 5)
                         (autocomplete-char-points c)))
          0
          chars))
```

Finally, after writing a trivial `middle` function that returns the data in the middle element of a list, we can create
the `part2` function. Once again, we'll split the input line-by-line, then keep only the `missing-chars` (instead of
the `first-corrupt-char` from part 1), map the sequence of incomplete characters to its bounty value, then sort the
bounties and return the one in the middle.

```clojure
(defn part2 [input]
  (->> (str/split-lines input)
       (keep missing-chars)
       (map incomplete-char-bounty)
       sort
       middle))
```