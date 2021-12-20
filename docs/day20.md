# Day Twenty: Trench Map

* [Problem statement](https://adventofcode.com/2021/day/20)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day20.clj)

---

## Part 1

Today we're simulating a Game Of Life as represented by an image whose pixels are either light or dark with each
generation. Our goal is to run the game a certain number of times against an image atop an infinite background, and
then count up the number of lit pixels in the end.

To start off, let's parse the input, which comes in as a single line of the 512-character "algorithm," followed by
the initial image. We'll just return that as a two-element vector, leveraging `utils/split-blank-line` to split the
input into its two segments. For the algorithm, we'll make a `pixel-map` that converts `.` to zero and `#` to 1; calling
`mapv` will ensure the result is a an indexed vector instead of a linked list. For the image, we'll want that to be a
map of `{[x y] 0-or-1}`. We'll leverage our existing `point/parse-to-char-coords` to return a sequence of each
coordinate pair to its character, and then associate the coords onto the resulting map by again mapping the character
using the `pixel-map`. Naturally we'll use constants of `dark-pixel` and `light-pixel` to avoid repeating `0` and `1`
throughout the solution.

```clojure
(def dark-pixel 0)
(def light-pixel 1)

(defn parse-input [input]
  (let [pixel-map {\. dark-pixel, \# light-pixel}
        [alg image] (utils/split-blank-line input)]
    [(mapv pixel-map alg)
     (reduce (fn [m [coord c]] (assoc m coord (pixel-map c)))
             {}
             (point/parse-to-char-coords image))]))
```

Our goal will include going to every point in the current image, looking at its surrounding 3x3 grid, and construct a
new image from the converted characters. But what do we do along the border? Well initially, every surrounding point
will be dark (aka `0`), but the algorithm tells us whether that will be true for every generation. To determine this,
we'll make an infinite `border-seq`, which takes in the algorithm and returns what every infinite border value will be
in each generation. Imagining an initial point far away from our image; all nine points in its grid will be dark,
meaning that its binary value will be `000000000` or just plain `0`. Some algorithms will map a `0` to `0` again,
keeping the border dark, but some might map it to `1` to make it light. Then similarly, a distant point of all light
values will have a binary value of `111111111` or `511`, which could again map each character to dark or light. So
`block-of` will map `0` to `0`, and `1` to `511`, and we'll `iterate` on mapping each previous value to its block
value, and then use `alg` to convert it to its new value at that index.

For what it's worth, in the sample problem, `(get alg 0)` is zero, so the border is always dark. In my input,
`(map alg [0 511])` was `[1 0]`, so the background flickered every generation from dark to light and back again.

```clojure
(defn border-seq [alg]
  (let [block-of {dark-pixel 0, light-pixel 511}]
    (iterate #(-> % block-of alg) 0)))
```

Now we can create a `migrate-image` function, which takes in the algorithm, the value of all border coordinates, and 
the current image, and returns the next image by migrating every point. To do this, we'll use a simple `reduce-kv` to
migrate each coordinate in the image, calling `next-value-at` for each coordinate. The `next-value-at` looks up all
points surrounding the current one, maps it to either its value on the image or else the default value of the
infinite border character. Then it concatenates all 9 characters into a binary string, which it converts into a number
and finds within the algorithm vector.

```clojure
(defn next-value-at [alg border image coords]
  (->> (point/surrounding true coords)
       (map #(or (image %) border))
       (apply str)
       utils/parse-binary
       alg))

(defn migrate-image [alg border image]
  (reduce-kv (fn [m coord _] (assoc m coord (next-value-at alg border image coord)))
             {} image))
```

We want to get to a function that returns every generation of the image, but there's only one issue to address first.
While `migrate-image` and `next-value-at` takes into account the infinite border, with each generation we almost
certainly will have at least one point along the perimeter that interacted with the infinite border, meaning that the
points just outside the previous border of the image will need to be calculated in the next generation. To account for
this, with each generation we'll need to expand the image by one row at the top and bottom, and by one column on the
left and right. (In theory, we could just expand the entire image once to the maximum intended size, but that pollutes
what each function does.)

The `expand-image` function looks at the current min and max values for both `x` and `y`, and associates the border
character to all coordinates of the surrounding perimeter. We implement `perimeter-points` in the `point` namespace
by taking all `[x y]` coordinates from a top-left point to the bottom-right point. Then `expand-image` uses `reduce`
to `assoc` the `border` onto each perimeter point, starting from the `image` map itself.

```clojure
; advent-2021-clojure.point namespace
(defn perimeter-points [[x0 y0] [x1 y1]]
  (concat (for [x [x0 x1], y (range y0 (inc y1))] [x y])
          (for [y [y0 y1], x (range (inc x0) x1)] [x y])))

; advent-2021-clojure.day20 namespace
(defn expand-image [image border]
  (let [min-max (juxt (partial apply min) (partial apply max))
        [min-x max-x] (min-max (map ffirst image))
        [min-y max-y] (min-max (map (comp second first) image))]
    (reduce #(assoc %1 %2 border)
            image
            (point/perimeter-points [(dec min-x) (dec min-y)]
                                    [(inc max-x) (inc max-y)]))))
```

So where are we now? We know the sequence of values of the infinite border, and we can use that sequence to both
expand the current image one step into its perimeter, and then to migrate every point of the image based on its
neighbors. The only major step remaining is to create `image-seq`, to generate an infinite sequence of every generation
the image goes through as it migrates. This function takes in the algorithm and initial image, and then creates the
`border-seq` in the background. For each generation, is expands and migrates the image on the current head of the
border sequence, and then uses `lazy-seq` (similar to `iterate`) to create the next element of the sequence. We don't
use `iterate` here because we can't use the same value for the border with each generation, but rather need to call
`(rest borders)` each time.

```clojure
(defn image-seq
  ([alg image] (image-seq alg image (border-seq alg)))
  ([alg image borders] (let [border (first borders)
                             image' (->> (expand-image image border)
                                         (migrate-image alg border))]
                         (lazy-seq (cons image' (image-seq alg image' (rest borders)))))))
```

Alright, let's create our `solve` function. We'll take in the input string and the number of enhancements we want to 
apply. The function parses the input, converts it into the sequence of images, and finds the `nth` value by dropping
`(dec enhance-count)` values from the sequence and pulling the next value. From that image, it counts the number of
values in the `[[x y] pixel]` pair by counting the number of pixels that are `light-pixel`, or `1`.

```clojure
(defn solve [input enhance-count]
  (->> (parse-input input)
       (apply image-seq)
       (drop (dec enhance-count))
       first
       (filter #(= light-pixel (second %)))
       count))
```

Finally, `part1` just calls `solve`, looking for the number of pixels after the second enhancement.
```clojure
(defn part1 [input] (solve input 2))
```

---

## Part 2

Part 2 runs the same algorithm for fifty enhancements. The code we already have is fine; we'll get the answer in about
10-15 seconds, which is fast enough for me.

```clojure
(defn part2 [input] (solve input 50))
```