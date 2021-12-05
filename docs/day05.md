# Day Five: Hydrothermal Venture

* [Problem statement](https://adventofcode.com/2021/day/5)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day05.clj)
* [Point code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/point.clj)

---

## Preamble

Here at Advent Of Code, we do enjoy playing with points on a grid! In the past I've reused previous years' point
namespaces, but this time I just started from scratch again. I swear that someday, I'll combine all my years of point
namespaces, and it could put together a halfway decent library!

In this probem, we're given input where each line contains two `x,y` coordinates, separated by an arrow. This represents
a hydrothermal vent against a 2-dimensional plane. For part 1, we need to look at the lines that are either horizontal
or vertical (not diagonal), project each line onto the individual points they include, and count the number of points
that are included in more than one vent.

Note that I did a pretty big cleanup after implementing the complete solution, such that the algorithm is flexible
enough to support part 2 already. But shhh... we'll pretend we don't know that yet.

The <u>easiest</u> representation for this puzzle would be to keep each vent looking like `[x1 y1 x2 y2]` because
that's how the data comes in, but I opted not to. I think a `point` namespace should deal with either functions that
take in two points `(p/something p1 p2)` or for line calculations, possibly a sequence with two points inside
`(p/line-something [p1 p2])`, but I wouldn't want to make a library that's coupled to `(p/something [x1 y1 x2 y2])`.
So I do a little extra work for the sake of potential reuse someday.

---

## Point namespace

The all new 2021 `point` namespace has a lot of the logic for the puzzle, and it's composed of four functions.

First, `inclusive-distance` measures the total number of inclusive points between two points, assuming that they are
either horizontal, vertical, or at a 45 degree angle. This lets us ignore slopes altogether, which is fine for this
problem. I used `letfn` because I think it's a neat macro when we have ugly or monotonous code inside a function.
So the inner function `local-dist` takes in two long values, and calculates the absolute value of their difference.
I used the `^long` type annotation to avoid Java reflection, since Clojure can't tell which overloaded function I
will have, and we can't use type hints for integers. With that in place, we keep the max distance between the two
`x` and `y` values, and then increment the distance so the line includes both points. For horizontal or vertical lines,
one distance should be 0; for diagonal lines, the two should be equal.

```clojure
(defn inclusive-distance [[x1 y1] [x2 y2]]
  (letfn [(local-dist [^long v1 ^long v2] (Math/abs (- v1 v2)))]
    (inc (max (local-dist x1 x2)
              (local-dist y1 y2)))))
```

The second function is `inclusive-line-between`, which returns all points between two points, inclusive. Here I
support both the 1-arity form (`(inclusive-line-between [p1 p2])`) and the 2-arity form
(`(inclusive-line-between p1 p2)`), as described in the preable. The first form just unpacks the value into the
second.

Now things get fun. I don't want complex logic to check if the points are in a convenient order, or if a diagonal
line goes up-right or down-right, so instead I made another internal function called `ordinate-fn`. The idea is that
I'll start from point 1, and for each point within the distance to point 2, I'll do _something_ to `x1` and
_something else_ to `y1`; `ordinate-fn` tells me which function to use. If the first value is before the second, as
in the x-value of `[1 3] [4 3]`, the correct function to apply to `x` will be `+`. Similarly, if the first value is
less than the second, use `-`. If the two values are the same, such as the y-value of the example above, then we
want to use the singular value, so I made an anonymous function `(fn [v _] v)`.

Armed with both `inclusive-distance` and `ordinate-fn`, we can write `inclusive-line-between`. We'll determine the
distance, meaning the number of inclusive points to return, and the `x-fn` and `y-fn` to apply to `x1` and `y1`.
Then we just map the range of distances to create our sequence of `[x y]` vectors, by calling `x-fn` on `x1` and the
distance, and calling `y-fn` on `y1` and the distance. This little ditty now gives us all inclusive points in any line!

```clojure

(defn inclusive-line-between
  ([[point1 point2]]
   (inclusive-line-between point1 point2))

  ([[x1 y1 :as point1] [x2 y2 :as point2]]
   (letfn [(ordinate-fn [v1 v2] (cond (< v1 v2) +
                                      (> v1 v2) -
                                      :else (fn [v _] v)))]
     (let [distance (inclusive-distance point1 point2)
           x-fn (ordinate-fn x1 x2)
           y-fn (ordinate-fn y1 y2)]
       (map #(vector (x-fn x1 %) (y-fn y1 %)) (range distance))))))
```

What's left are two simple functions, called `horizontal-line?` and `vertical-line?`, and both support the same
1-arity and 2-arity versions that `inclusive-line-between` does. A horizontal line has the same values for `y1` and
`y2`, and a vertical line has the same values for `x1` and `x2`.

```clojure
(defn horizontal-line?
  ([[point1 point2]] (horizontal-line? point1 point2))
  ([[_ y1] [_ y2]] (= y1 y2)))

(defn vertical-line?
  ([[point1 point2]] (vertical-line? point1 point2))
  ([[x1 _] [x2 _]] (= x1 x2)))
```

## Part 1

Well shoot; the code that's left is really easy!

Let's parse. First we'll create `parse-line` to take a single line and convert into the structure `[[x1 y1] [x2 y2]]`
for two points. To do this, we'll apply a regex using `re-matches` to grab each number; `re-matches` upon a successful
match returns the _entire_ string first before the tokens, so we call `rest` to get rid of that unnecessary value,
and then `(map parse-int)` to convert them all into integers.  A little destructuring magic and we compose the data
in the correct shape. Then the `parse-vents` function just calls `parse-line` for each line of the input.

```clojure
(defn parse-line [line]
  (let [[x1 y1 x2 y2] (->> (re-matches #"(\d+),(\d+) -> (\d+),(\d+)" line)
                           rest
                           (map parse-int))]
    [[x1 y1] [x2 y2]]))

(defn parse-vents [input] (->> input str/split-lines (map parse-line)))
```

I'm going to cheat and tell you what anyone reading this problem instantly figured out - if part 1 only allows
horizontal and vertical lines, part 2 also allows diagonals. So rather than write something and refactor it, let's
just proceed with this foreknowledge.

We're going to call a unified `solve` function that does the following:
1. Parse the input string into the sequence of vents, using `(parse-vents)`.
2. For part 1, apply a filter to look for only horizontal or vertical lines, but filter out nothing for part 2.
3. For each valid vent, map it to the sequence of points it contains.
4. Concatenate all of the sequences into one giant sequence of points.
5. Call the `frequencies` function to return a map from each point to the number of times it appears in the sequence.
6. Filter for only those points whose value (the number of instances) is above 1.
7. Count them up.

```clojure
(defn solve [input pred]
  (->> (parse-vents input)             ;1
       (filter pred)                   ;2
       (map p/inclusive-line-between)  ;3
       (apply concat)                  ;4
       frequencies                     ;5
       (filter #(> (val %) 1))         ;6
       count))                         ;7
```

Well that was straightforward, but we have one last piece of magic - the `part1` function. Besides calling `solve`,
we need to provide that predicate which says "return true if the line is either horizontal or vertical." I came up with
two solutions to do this, both involving anonymous functions that manually test each predicate, but I think they're
ugly, so I asked for help on the Clojurian Slack forum and learned about `some-fn`. This function takes in a variatic
number of functions, and returns the first one that returns a truthy response to some input, or else a falsey value.
That's perfect! We can send the predicate `(some-fn p/horizontal-line? p/vertical-line?)` for each vent, and if either
predicate is true, the whole thing returns a truthy value.

```clojure
; These both work, but are awfully busy
(defn part1 [input] (solve input #(or (p/horizontal-line? %) (p/vertical-line? %))))
(defn part1 [input] (solve input #(some (fn [test] (test %)) [p/horizontal-line? p/vertical-line?])))

; Check out some-fn! It does exactly what we need!
(defn part1 [input] (solve input (some-fn p/horizontal-line? p/vertical-line?)))
```

Huge suspense for part 2...

---

## Part 2

Oh look - diagonal lines! Well this should be no work at all, since our `solve` function just needs to include every
vent it sees.

```clojure
(defn part2 [input] (solve input identity))
```

Great puzzle yet again. I'm excited for day 6!