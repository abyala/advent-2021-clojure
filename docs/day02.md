# Day Two: Dive!

* [Problem statement](https://adventofcode.com/2021/day/2)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day02.clj)

---

## Part 1

Ahoy, mateys! It be time to take th' ol' sub ta sea!

Today we're taking our submarine out for a ride. We're given a list of instructions, which either moves our submarine
forward (increasing its position), down (increasing its depth), or up (decreasing its depth). We will need to multiply
its final position by its depth to give our answer. Pretty straightforward.

As always, let's start with parsing. Assuming we'll be splitting the input by each line, we want to turn an individual
line of `"forward 5"` to `[:forward 5]`. Clojurians would _never_ use Strings when keywords convey the intention better,
and the String 5 needs to become numeric. So we split the line into the words before and after the space, converting
the former into a keyword and the later into an int.

```clojure
(defn parse-instruction [s]
  (let [[a b] (str/split s #" ")]
    [(keyword a) (Integer/parseInt b)]))
```

Now let's briefly think about how to represent the submarine. The easiest option is a simple map with keys of
`:pos` and `:depth`; to feel good about that decision, we'll define the initial submarine state. And while we're at
it, let's quickly define the function `final-position` that multiplies together the `:pos` and `:depth` of the sub.

```clojure
(def initial-submarine {:pos 0 :depth 0})

(defn final-position [{:keys [pos depth]}]
   (* pos depth))
```

As we move line by line through the input, we'll need update the submarine based on the instruction found. Of course,
I'm using the Clojure definition of "update," which doesn't mutate the original sub but rather creates a new one based
on changed data values. While one could write, for the `:forward` instruction, `(update submarine :pos #(+ % amount))`,
we can simplify the expression by providing `update`'s function and arguments inline as 
`(update submarine :pos + amount)` instead. Combine that with a `case` macro, and we have a simple `move-sub` function.
Note that I'm destructuring the instruction into its `dir` and `amount` components within the function argument list
since it's clear in this namespace what an instruction looks like.

```clojure
(defn move-sub [submarine [dir amount]]
   (case dir
      :forward (update submarine :pos + amount)
      :down (update submarine :depth + amount)
      :up (update submarine :depth - amount)))
```

Finally, we're ready to put it all together into the `part1` function. All we need to do is split the input string
into its lines, map each line to its `[dir amount]` vector with the `parse instruction` function, reduce each such
instruction with the `move-sub` function and the `initial-submarine` definition, and then calculate the 
`final-position`. This right here is what I love about Clojure - from three simple functions, we get a very
easy-to-read coordination function that reads cleanly without a lot of syntax clutter.

```clojure
(defn part1 [input]
   (->> (str/split-lines input)
        (map parse-instruction)
        (reduce move-sub initial-submarine)
        final-position))
```

Looking good!  Let's move on to Part 2.

---

## Part 2

Hmm. This looks very much the same as Part 1, except that we'll have different interpretations of each of the three
instructions. We'll start with the obvious solution first, and then refactor it later.

First off, a submarine is now defined by three properties instead of just two -- the position, depth, and aim. So
let's revise the `initial-submarine` function. Note that this shouldn't impact `part1` at all.

```clojure
(def initial-submarine {:pos 0 :depth 0 :aim 0})
```

Now we'll make a `move-sub2` function that's very similar to `move-sub`, except that each instruction changes the sub
differently. The `:down` and `:up` instructions just move the aim instead of the depth, so they're easy enough.
The `:forward` instruction now becomes two separate `update` calls, where the latter needs to extract out the current
`aim` in order to multiply it by the `amount` to get to the right answer.

```clojure
(defn move-sub2 [submarine [dir amount]]
  (case dir
    :forward (-> (update submarine :pos + amount)
                 (update :depth + (* (:aim submarine) amount)))
    :down (update submarine :aim + amount)
    :up (update submarine :aim - amount)))
```

Finally, `part2` is the same as `part1`, except that its reducing function is `move-sub2` instead of `move-sub`.

```clojure
(defn part2 [input]
   (->> (str/split-lines input)
        (map parse-instruction)
        (reduce move-sub2 initial-submarine)
        final-position))
```

I mean, it works, but if you're actually reading this text, you should know by now that I'm not going to keep that
code lying around. That means it's time to refactor!

---

## Cleanup

It's clear that parts 1 and 2 only differ in how the submarine moves based on each instruction. I'd like to abstract
that away by defining a function called `create-mover`, which will take in the three functions to apply to a submarine
(forward, down, and up), and return a new function that will call the right function when invoked with a submarine and
an instruction.

It sounds much more complicated that it looks. Within the `defn`, we define an anonymous function with the same
signature as the previous `move-sub` and `move-sub2` functions - it takes in a submarine and an instruction
(destructured into its direction and amount), and it returns an updated submarine. To avoid a case statement, I create
a map of the three directional keywords (`:forward`, `:down`, and `:up`) to their respective functions, and I call
`(dir {map})`. Once we know which operation/function to call, we simply call `(op submarine amount)`. 

```clojure
(defn create-mover [forward-fn down-fn up-fn]
   (fn [submarine [dir amount]]
      (let [op (dir {:forward forward-fn, :down down-fn, :up up-fn})]
         (op submarine amount))))
```

Now let's create our two mover functions. Each calls `create-mover` with three functions, which I've chosen to
represent as anonymous functions since they're so small. The first function for part 2 (move forward) is a little more
verbose since it performs two updates, but this time I destructure the `aim` out of the submarine in the function
argument declaration. This is one of the awesome features of Clojure destructuring - we can define an argument of
`{aim :aim :as submarine}` to mean "given an associative argument (a map), pull the `:aim` property into a
local binding of `aim`, but still bind the entire argument to the binding `submarine`." Without this capability, we
would have needed a `let` binding lower down, but this is much more concise and expressive.

```clojure
(def part1-mover (create-mover (fn [submarine amount] (update submarine :pos + amount))
                               (fn [submarine amount] (update submarine :depth + amount))
                               (fn [submarine amount] (update submarine :depth - amount))))
(def part2-mover (create-mover (fn [{aim :aim :as submarine} amount] (-> (update submarine :pos + amount)
                                                                         (update :depth + (* aim amount))))
                               (fn [submarine amount] (update submarine :aim + amount))
                               (fn [submarine amount] (update submarine :aim - amount))))
```

We could have gone farther and made everything anonymous, but that would turn my beautiful Clojure code into something
that looks like Perl, and nobody wants that!

```clojure
; This is awful. Don't do this. These functions deserve to have named arguments, so let's honor them as shown above. 
(def part1-mover (create-mover #(update %1 :pos + %2)
                               #(update %1 :depth + %2)
                               #(update %1 :depth - %2)))
(def part2-mover (create-mover #(-> (update %1 :pos + %2)
                                    (update :depth + (* (:aim %1) %2)))
                               #(update %1 :aim + %2)
                               #(update %1 :aim - %2)))
```

Now that we have defined two movers, we can make the unified `solve` function. This function takes in both the mover
and the input string, and then it reduces each instruction using that `mover` function.

```clojure
(defn solve [mover input]
  (->> (str/split-lines input)
       (map parse-instruction)
       (reduce mover initial-submarine)
       final-position))
```

Finally, we redefine the `part1` and `part2` functions. Now I could use `def` and partial functions, since I used
`def` for the movers, but I think that's a little harder to understand when we're not using the functions as arguments
into other functions. So instead, I'll use the normal `defn` that calls the `solve` function with the correct mover
and the input String.

```clojure
; Using defs for the movers were fine, but I don't like it here.
(def part1 (partial solve part1-mover))
(def part2 (partial solve part2-mover))

; For a tiny bit of repetition, I think these definitions are clearer.
(defn part1 [input] (solve part1-mover input))
(defn part2 [input] (solve part2-mover input))
```

There, now that's a handsome submarine processing algorithm! I really love the use of higher order functions in this
solution.