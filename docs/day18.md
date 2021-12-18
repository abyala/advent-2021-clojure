# Day Eighteen: Snailfish

* [Problem statement](https://adventofcode.com/2021/day/18)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day18.clj)

---

## Preamble

I found today's problem to be quite simple to work through, which was a delight for a Saturday morning exercise.
Clojure definitely made a few things extra easy today, as we help our snailfish friends with their math homework.

We are given several snailfish numbers that need to be added together, reducing them after each addition, and then
return the final magnitude calculation. I won't repeat all of the instructions for how the snail math works, but I do
want to spend a moment to talk about Clojure persistent vectors, their equivalent of Java arrays or lists. These
vectors implement a bunch of interfaces, including Associative, Sequential, and Indexed. This means that they are very
powerful workhorses, especially in their ability to work like maps.

It's common in Clojure to use functions like `get`, `get-in`, `assoc`, or `update-in` to modify elements within a map.
We can use all of those functions with vectors, where the key of each function is its index in the vector. In the case
of nested vectors, a function like `get-in` takes in a vector of the indexes to access.

```clojure
; Work with maps
(def person {:name "Andrew" :address {:street-num 123, :street-name "Fake Street", :city "Springfield"}})
=> nil

(get person :name)                       
=> "Andrew"

(get-in person [:address :city])         
=> "Springfield"

(assoc person :name "Homer")             
=> {:name "Homer" :address {:street-num 123, :street-name "Fake Street", :city "Springfield"}}

(update-in person [:address :street-num] inc)
=> {:name "Andrew" :address {:street-num 124, :street-name "Fake Street", :city "Springfield"}}

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; Work with vectors
(def tree [[1, 2] 3])
=> nil

(get tree 1)
=> 3

(get-in tree [0 1])
=> 2

(assoc tree 0 5)
=> [5 3]

(update-in tree [0 0] dec)
=> [[0 2] 3]
```

Alright, with that lesson out of the way, let's do some math!

---

## Part 1


First of all, parsing - there's almost nothing to do. Whether we use Clojure's core `read-string` function or the
`edn/read-string` function, either can turn a string `[[1,2],3]` into a two element vector of a pair of longs and a
long. How convenient for us, since I'm quite happy to represent a snailfish number (which I call "trees" in my code)
as a nested vector. So check out this rough parsing logic.

```clojure
(defn parse-trees [input] (map read-string (str/split-lines input)))
```

### Exploding

Now let's think about how to explode a tree. Calm down, we're just going to explode a snailfish number within a tree,
but `explode-snailfish-number-within-a-tree` is ridiculous. Without keeping track of every node within the tree, its
value(s) and depth, I'm just going to walk through the tree instead. The `explode-pos` function takes in a tree and
returns the vector position of which node can be exploded, if any. To do this, the function will recursively call it
with different `pos` values, representing the index in the tree to examine, in a depth-first search. If there are four
elements within `pos`, then we've reached explosion depth, so return that `pos` vector itself. Otherwise, look at the
two children in the tree at that index, recursing into `explode-pos` if the leaf node is a vector; we don't explode
regular numbers, only vector nodes.

```clojure
(defn explode-pos
  ([tree] (explode-pos tree []))
  ([tree pos] (if (>= (count pos) 4)
                pos
                (->> (get-in tree pos)
                     (keep-indexed (fn [idx leaf] (when (vector? leaf)
                                                    (explode-pos tree (conj pos idx)))))
                     first))))
```

So let's assume we find an `explode-pos` for a tree. That position will become a regular zero, but we need to find the
closest regular numbers on either side, or at least the positions of them. For this, we'll make two functions called
`regular-numbers` and `regular-number-positions`. The former will take in a tree and return a sequence of `[pos val]`
pairs for each regular number in the tree, representing the position vector and the value of that number. The latter
just strips out the position values. Again, this is a depth-first search for _all_ regular numbers, meaning leaf nodes
that aren't themselves vectors.

```clojure
(defn regular-numbers
  ([tree] (regular-numbers tree []))
  ([tree pos] (->> (get-in tree pos)
                   (map-indexed (fn [idx leaf] (let [leaf-pos (conj pos idx)]
                                                 (if (number? leaf)
                                                   [[leaf-pos leaf]]
                                                   (regular-numbers tree leaf-pos)))))
                   (apply concat))))

(defn regular-number-positions [tree] (->> tree regular-numbers (map first)))
```

Why did we make these sequences?  Because if we explode a node by converting it into a 0, and we know its location, we
can then find the positions of the nodes immediately to the left and the right, with a simple function called
`pos-before-and-after`. This will take in an arbitrary collection (in this case, a sequence of position vectors) and
a search value, and returns a two-element vector of the values right before and right after that value. We can use an
old trick of calling `(partition 3 1)` to create three-element windowed views of all values in the collection, which
we call `[a b c]`, returning `[a c]` when `b` equals the search value. The only tricky part is that we'll have to add
`nil` values to the front and back of the input collection, since the search value might be the first or last value,
and `(partition 3 1 [:my-value :my-neighbor])` would return `nil`, when we would want `[nil :my-neighbor]`.

```clojure
(defn pos-before-and-after [coll v]
  (->> (concat [nil] coll [nil])
       (partition 3 1)
       (keep (fn [[a b c]] (when (= b v) [a c])))
       first))
```

It's time to write `explode-tree`, which takes in a tree and returns either the output of exploding its first node, or
`nil` if it can't be exploded. We'll call `explode-pos` to get the position of the exploding node. If it's there, we'll
pull out its elements `a` and `b`, then zero out the value at that position, and find the positions of the neighboring
regular numbers, if any. Finally, we'll add `a` to the left neighbor, and `b` to the right neighbor. The
`add-if-present` helper function adds the previous value to the neighboring number, if it's present. We'll use
`(if pos then else)` structure to only attempt the `update-in` if the `pos` value is not `nil`. 

```clojure
(defn add-if-present [tree pos v]
  (if pos (update-in tree pos + v) tree))

(defn explode-tree [tree]
  (when-some [pos (explode-pos tree)]
    (let [[a b] (get-in tree pos)
          zeroed-out (assoc-in tree pos 0)
          [left-pos right-pos] (-> (regular-number-positions zeroed-out)
                                   (pos-before-and-after pos))]
      (-> zeroed-out
          (add-if-present left-pos a)
          (add-if-present right-pos b)))))
```

### Splitting

Splitting a node in the tree is much easier to do, now that we've got all the pre-work done for explosions. Just as we
made `explode-pos`, we'll make `split-pos` to find the position of the first regular number to be split, meaning its
value is at or above 10. Using `regular-numbers`, we just return the position of the first such value, so that's easy.
To split the value there, we'll make `split-val`, which makes a vector of half of the value rounded-down, and the
difference between it and the original value; this was easier for me than rounding up. Then `split-tree` just calls
`update-in` to invoke `split-val` on the value of the number at `split-pos`, if there is one.

```clojure
(defn split-pos [tree] (->> (regular-numbers tree)
                            (keep (fn [[p v]] (when (>= v 10) p)))
                            first))

(defn split-val [n] (let [a (quot n 2)
                          b (- n a)]
                      [a b]))

(defn split-tree [tree]
  (when-some [pos (split-pos tree)]
    (update-in tree pos split-val)))
```

### Loose ends

We have three more little functions to write before we create `part1`. First, we need to reduce a tree, watching out
because `reduce` is a core CLojure function. In this case, we start with a tree, explode a leaf if we can, or else
split it, and keep repeating that pattern until there are no more changes. We'll use my best friends `iterate` and
`some-fn`, where the latter attempts to either explode or split the previous tree, and `iterate` creates an infinite
sequence of applying one function or the other. Once neither function does anything, the `some-fn` will return a
falsey value. So `reduce-tree` calls `(take-while some?)` to keep only the valid values, and returns `last` to get the
final state.

```clojure
(defn reduce-tree [tree]
  (->> tree
       (iterate (partial (some-fn explode-tree split-tree)))
       (take-while some?)
       last))
```

The `add-trees` function is very simple - given a sequence of trees, it calls a simple (core) `reduce` on it. The code
creates a vector around each pair of trees, and invokes `reduce-tree` to simplify it before moving on to the next tree.

```clojure
(defn add-trees [trees]
  (reduce #(reduce-tree (vector %1 %2)) trees))
```

Finally, the `magnitude` of a tree comes from recursively adding the triple of the first value in a node with the
double of the second value. We'll use `mapv` to map the two sides of the tree to either its own value, if it's a
number, or the `magnitude` of its side. Then we again use `mapv` in `(mapv * [3 2] pair-of-values)` to multiple the
two values, and then we'll add them together.

```clojure
(defn magnitude [tree]
  (->> (mapv #(if (number? %) % (magnitude %)) tree)
       (mapv * [3 2])
       (apply +)))
```

Alright, it's time for part 1!  Parse the input, add all of the trees together, and calculate the magnitude.

```clojure
(defn part1 [input]
  (->> input parse-trees add-trees magnitude))
```

---

## Part 2

Well now, there's almost nothing to do here. We need to combine every combination of trees from the input, recognizing
that snailfish addition is not commutative, since `[[1 2] [2 3]]` does not equal `[[2 3] [1 2]]`. So we'll use a
`for` macro to combine every combination of `t0` and `t1` where they aren't the same. then for each pair of trees,
we'll call `add-trees` and `magnitude`, and then collect the max value to finish the puzzle!

```clojure
(defn part2 [input]
  (let [trees (parse-trees input)]
    (->> (for [t0 trees, t1 trees, :when (not= t0 t1)] [t0 t1])
         (map (comp magnitude add-trees))
         (apply max))))
```
