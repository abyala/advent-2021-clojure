# Day Thirteen: Transparent Origami

* [Problem statement](https://adventofcode.com/2021/day/13)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day13.clj)

---

## Preamble

Today's puzzle was really interesting, until I got stuck on the tiniest silly algorithmic bug that stumped me for
several hours. I ended up coding it up once, then again twice in a worse way, then bashed my head against the wall
until I found the bug. What you see below is the original solution, minus that silly bug. Still, it was a terrific
puzzle once I fumigated my brain.

---

## Part 1

The puzzle gives us a piece of paper with `x,y` pairs that correspond to dots on a piece of paper, and instructions
on how to fold the paper. The idea is that after folding a paper, a dot may appear at a point from either the
"unfolding" side, or from the folding side once we project its new coordinates across the fold line.

Since we're just playing with dots, the easiest data structure I can see is a set of all `[x, y]` coordinates that
have dots, plus a sequence of folding instructions. We can create a two-element sequence from the input file by
using `utils/split-blank-line` as we've seen before. Parsing the set of points isn't too difficult. We look at each
row of input, split each row into two strings that are separated by a comma, and map each side to an integer. This
gives us a sequence of coordinates, which we throw into a set.

Parsing the fold instructions is _just_ difficult enough to extract out a `parse-instruction` function to apply to
each line of instruction input. We'll use a regex to pull out the String `"x"` or `"y"` plus the row or column number
to extract. We'll parse the latter into an integer, and convert the former into a keyword because that's what we do
in Clojure.

Thus the `parse-paper` function returns a two-element sequence of `(set-of-dots, sequence-of-instructions)`.

```clojure
(defn parse-instruction [line]
  (let [[_ axis amount] (re-matches #"fold along ([xy])=(\d+)" line)]
    [(keyword axis) (parse-int amount)]))

(defn parse-paper [input]
  (let [[dots folds] (utils/split-blank-line input)]
    [(->> (str/split-lines dots)
          (map #(mapv parse-int (str/split % #",")))
          set)
     (map parse-instruction (str/split-lines folds))]))
```

All of the magic goes into the `fold` function. Shout out to my Kotlin (and Scala?) friends for whom "fold" is
equivalent to Clojure's `reduce` function - I'm using `fold` here because I can! Anyway, it's actually rather simple.
First, we'll deconstruct the instruction back into its direction and fold-line components. Then we'll create
`coord-idx`, which maps `:x` to the first value in a coordinate pair, and `:y` to the second; this lets us have a
single `fold` function instead of `fold-up` and `fold-left` functions. 

For each dot currently on the paper, we'll pull out the relevant component of the coordinate using `coord-idx`;
assuming a point `[10 15]` and a horizontal fold along `x=8`, the value we look at is `10`. If the dot is not on the
folding side (the value is less than the fold line), the point will just map to itself. If the dot _is_ on the folding
side, we need to update is ordinate by projecting it across the line. In our example `x=10` and `fold-line=8`, using
"normal" arithmatic instead prefix notation, the distance from `x` to the line is `(x - line)`, so the target is
`line - (x - line)` which becomes `(2 * line - x)`. Once we map each point to its new location, we push the sequence
into a set so we can avoid duplicates. This isn't strictly necessary, but the datatypes are more intuitive this way.

```clojure
(defn fold [dots [dir fold-line]]
  (let [coord-idx ({:x 0 :y 1} dir)]
    (->> dots
         (map (fn [dot]
                 (let [v (dot coord-idx)]
                   (if (<= v fold-line)
                     dot
                     (update dot coord-idx (partial - (* 2 fold-line)))))))
         set)))
```

Now it's easy to solve part 1. We just need to fold the paper once, and count the number of dots. Since I'm such a big
fan of Clojure's destructuring, I'll draw attention to how we pull out only the first instruction from the sequence
by making the binding `(let [[dots] [instruction]] (parse-paper input))`, which would be equivalent to
`(let [[dots instructions] (parse-paper input), instruction (first instructions)])`.

```clojure
(defn part1 [input]
  (let [[dots [instruction]] (parse-paper input)]
    (count (fold dots instruction))))
```

### Quick aside

So what tripped me up for so long on this problem? It was the update function, as I did not use `(2 * line - x`).
Instead, I originally decided to find the maximum `x` or `y` value on the board, and just called `(maximum - x)`.
That worked for part 1, and _almost_ worked for part 2, except that if there were an even number of rows or columns,
there would be data loss.

---

## Part 2

For part 2, we just need to run the dots through all of the instructions, and print out the results such that we can
read the 8 capital letters.

Since the data structure for the dots is a map instead of nested vectors (at least this implementation is!), we need
to create lines of strings from the map of points. We'll find the min and max values of `x` and `y` across the points,
and then use nested `map` functions and `range`s to convert each row ito a String of hash marks and spaces.

The only unusual part of the `print-dots` function is the use of the `run!` function. We know that `run!` is unusual
because it ends in an exclamation mark, which by convention is a way of telling the developer that the function has
side effects. `run!` applies a function to every element in a collection; in this case, we want to just call `println`
on each row of strings. I wonder it's called `println` instead of `println!` since it has side effects...

```clojure
(defn print-dots [dots]
  (let [min-x (apply min (map first dots))
        min-y (apply min (map second dots))
        max-x (apply max (map first dots))
        max-y (apply max (map second dots))]
    (run! println (map (fn [y]
                         (apply str (map #(if (dots [% y]) \# \space)
                                         (range min-x (inc max-x)))))
                       (range min-y (inc max-y))))))
```

And now, we just reduce the `fold` function over the dots and the instructions, and print out the results. By calling
`(reduce fold dots instructions)` and combining rivals `reduce` and `fold`, we're bringing about world peace, one
puzzle at a time!

```clojure
(defn part2 [input]
  (let [[dots instructions] (parse-paper input)]
    (print-dots (reduce fold dots instructions))))
```

### That bug again

How did my output look when I used the wrong arithmetic on my dot mapping function? Check it out below. With enough
trial and error, I could have figured out what each of those letters were, but what's the point of that?

```clojure
;; Incorrect mapping function

;;  ## ## # #### #### #    #### # ## #  #
;;   # #  #    # #### #    #  # ###  ####
;;   # ####  ##  #### #    # ## ###  ####
;;#  # ###  ##   #  # #    # ## # #  #  #
;;## # # ## #### ###  #### #### # #  #  #

;; Correct mapping function

;;  ## ###  #### ###  #     ##  #  # #  #
;;   # #  #    # #  # #    #  # # #  #  #
;;   # #  #   #  ###  #    #    ##   ####
;;   # ###   #   #  # #    # ## # #  #  #
;;#  # # #  #    #  # #    #  # # #  #  #
;; ##  #  # #### ###  ####  ### #  # #  #
```
