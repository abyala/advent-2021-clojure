# Day Four: Giant Squid

* [Problem statement](https://adventofcode.com/2021/day/3)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day03.clj)

---

## Preamble

Today's puzzle was another extremely fun one, where we learned that the correct protocol upon meeting a giant squid is
to play a game or two of bingo. This is another one that I ended up implementing a few ways, as a means of playing with
the language. One of my big fears with moving from OO languages like Java or Kotlin over to Clojure was that
refactoring would be too difficult without the protections and encapsulations of OO. It turns out that's nonsense, or
rather, the difficulty is equivalent.  I'll explain why in the epilogue below.

---

## Part 1

So yeah, we're going to play some bingo. Seems legit. Of course we're going to start with parsing, but first let's
spend some time figuring out what we want to represent in data. I decided that I would represent a game as a map of
three properties - `:numbers-to-draw` as a sequence of numbers we're ready to draw, `:numbers-drawn` as a sequence of
the numbers we did draw, and `:boards` for the current state of the game board. To represent each board, I need to know
two things - the set of coordinates that have been marked, and where to find the spaces we haven't marked yet.
So rather than storing each cell in the board, with its coordinates, numeric value, and a flag for whether or not it's
been seen yet, I went with something more deconstructed.  The final format of the game becomes this:

```clojure
; Structure of a game
{:numbers-to-draw (n3 n4 n5)
 :numbers-drawn (n2 n1)
 :boards ({:marked #{[x y]}
           :unmarked {n [x y]}
           :bingo? boolean})}
```

Now that we know what we're shooting for, it's time to parse. The first line of the input is a comma-separated list of
the numbers to draw, so that's easy enough.

```clojure
(defn parse-numbers-to-draw [line]
  (->> (str/split line #",")
       (map parse-int)))
```

Parsing the each board is a little more complex. A board is represented by a line of text for each row, where the line
is a space-separated list of the numbers on the board. There should be five rows and 5 columns. To parse each line into
five numbers, we will use `re-seq` to return the sequence of numeric strings within the line, and then parse each
number using `parse-int`. From there, we use doubly-nested `map-indexed` functions to derive the `x` and `y`
coordinates, which we associate together into one giant map called `:unmarked`.

```clojure
(defn parse-board [lines]
  {:marked #{}
   :unmarked (->> (str/split-lines lines)
                  (map #(map parse-int (re-seq #"\d+" %)))
                  (map-indexed (fn [y nums]
                                 (map-indexed (fn [x n] [n [x y]]) nums)))
                  (apply concat)
                  (into {}))})
```

Now that we can parse the incoming numbers and the boards, we put them together using `parse-game`. In previous years,
I created the `utils/split-blank-line` function to split a single strings into smaller strings when we see two
consecutive blank lines, accommodating for Windows newline nonsense. And as with the day 3 puzzle, we use a variatic
deconstruction by binding the results of `split-blank-line` using `[drawn & boards]`, which has the effect of binding
the first string to `drawn` and the remaining strings into a sequence named `boards`.

```clojure
(defn parse-game [input]
  (let [[drawn & boards] (utils/split-blank-line input)]
    {:numbers-to-draw (parse-numbers-to-draw drawn)
     :numbers-drawn   ()
     :boards          (map parse-board boards)}))
```

We're not quite ready to play the game yet. First, I want to make a few helper functions to make working with boards a
little easier. The `coords-of` function takes in a number, and returns the `[x y]` coordinates of that number, if it's
on the board. Then `unmarked-values` returns the sequence of numbers that have not yet been marked. They're both simple
to look at, but will make the game logic later easier to read.

```clojure
(defn coords-of [board n] (get-in board [:unmarked n]))
(defn unmarked-values [board] (-> board :unmarked keys))
```

Now let's figure out how a board changes once we draw a number. If we can find the coordinates of that number in an
unmarked cell, we'll need to move it from marked to unmarked, and then check to see if the board has won. This means
using `conj` to add the number to the set of marked coordinates,`dissoc` to remove the mapping from that number to its
coordinates, and then check for a bingo (TBD). If that number doesn't appear anywhere on the board, just return the
board itself.

```clojure
(defn place-number [board n]
  (if-let [c (coords-of board n)]
    (-> board
        (update :marked conj c)
        (update :unmarked dissoc n)
        (check-for-bingo))
    board))
```

It's time to figure out whether a board has won, which means if there is a horizontal or vertical line where all
values have been marked. For this, I defined `winning-combos`, which is a sequence of sequences of coordinates. Using
the list comprehension of the `for` macro, I create all of the horizontal sequences and vertical sequences, and then
concatenate them together.

I seldom use `for`, so I'll jump into it for a second. A horizontal sequence is one where the `y` ordinate is the
same for each value of `x` from 0 to 4.  So we start with `(for [y (range 5)] something)])` to work with each value of
`y`. All we have to do then is map each `x` value to a vector of `[x y]`, leaving us with
`(for [y (range 5)] (map #(vector % y) (range 5)))`. It looks funny to see `(range 5)` twice, but there it is.

```clojure
(def winning-combos (let [horizontal (for [y (range 5)] (map #(vector % y) (range 5)))
                          vertical   (for [x (range 5)] (map #(vector x %) (range 5)))]
                      (concat horizontal vertical)))
```

Now we can create our `check-for-bingo` function, and it's super simple. We need to see if the set of `marked`
coordinates is a superset of any of the winning combinations. `some` in Clojure returns the first truthy value, which
in this context means "are there any winning combinations that are a subset of the marked coordinates." This makes our
function easy:

```clojure
(defn check-for-bingo [{:keys [marked] :as board}]
  (if (some (partial set/superset? marked) winning-combos)
    (assoc board :bingo? true)
    board))
```

We can play a number onto a single board, so let's see how to play an entire turn. We'll need to draw a number from
the front of the `:numbers-to-draw` sequence, move that to the front of the `:numbers-drawn` sequence, and then
update the sequence of boards by calling `place-number` on each of them. The thread-first macro (`->`) lets us apply
each of these changes to the `game` board cleanly.

```clojure
(defn take-turn [{:keys [numbers-to-draw boards] :as game}]
  (let [drawn (first numbers-to-draw)]
    (-> game
        (update :numbers-to-draw rest)
        (update :numbers-drawn conj drawn)
        (assoc :boards (map #(place-number % drawn) boards)))))
```

If the game is in a state where a board has called Bingo, it's time to determine the final score. We'll filter the
boards for the first one for which `:bingo?` is true, and use `when-some` to return `nil` if there is no winner. If
there is a winner, then we add together the unmarked values using `(apply + (unmarked-values winner))`, and multiply it
against the number just drawn, which is `(first numbers-drawn)`.

```clojure
(defn final-score [{:keys [boards numbers-drawn] :as _game}]
  (when-some [winner (first (filter :bingo? boards))]
    (* (apply + (unmarked-values winner))
       (first numbers-drawn))))
```

Almost done. We now need to play the game until we have a single winner, at which point we return the final score.
We just put the pieces together here, using one of my favorite functions, `iterate` - this one infinitely applies a
function to an initial value, or the result of the previous iteration. This function will give us the state of the game
after each number is called. We just call `(keep final-score)`, since the `final-score` function returns `nil` if there
is no winner. At that point, the first final score is _the_ score.

To solve part 1, we just take the input, parse it into a game, and play that game until there's a winner.

```clojure
(defn play-until-winner [game]
  (->> (iterate take-turn game)
       (keep final-score)
       first))

(defn part1 [input] (-> input parse-game play-until-winner))
```

Whew!  That felt like a long explanation for a fairly straightforward problem.  Let's go to part 2!

---

## Part 2

Well isn't it nice of us to let the squid win? We need to play our game of Bingo, throwing away every winner until
there is only one board left, and then play it out until that board wins. This will be a snap!

First, let's find a way to remove all winners from the game, so we can whittle down the boards. This is a simple
one-liner, where we call `(remove bingo? boards)` from the game.

```clojure
(defn remove-winners [game]
  (update game :boards (partial remove :bingo?)))
```

Now we need to start with the initial game board, and keep playing until there's only one board left. I'll call this
`play-until-solitaire`, and it isn't too tough. We're going to use `iterate` again, but instead of just calling
`take-turn`, we also need to call `remove-winners`. Now we could accomplish that by calling `(map remove-winners game)`
after the `take-turn` function runs, but instead we'll use `comp` to compose a single function out of two others. This
will let the `iterate` function do two things for each iteration - take the turn and remove the winners. Once that's
done, we just have to drop game states until there's a single board remaining.

```clojure
(defn play-until-solitaire [game]
  (->> (iterate (comp remove-winners take-turn) game)
       (drop-while #(> (count (:boards %)) 1))
       first))
```

Finally, we can make our `part2` function, which I'll put here next to the `part1` function for aesthetics. We will
parse input into the game board, play until it's a solitaire game, and then play that until we've won.

```clojure
(defn part1 [input] (-> input parse-game play-until-winner))
(defn part2 [input] (-> input parse-game play-until-solitaire play-until-winner))
```

--- 

## Epilogue

In my original implementation, I represented a bingo board as a map of `{[x y] {:value n :marked boolean}}` instead of
two deconstructed values of `:marked` and `:unmarked`. There was nothing wrong with it per se, except that I had to
keep recalculating whether or not the board had won. The good news is that I barely had to change anything, except for
my parsing code and the implementations of `coords-of` and `unmarked-values`, and the `bingo?` calculation. The 
application logic itself was completely unaffected, which is the whole goal.

The same thing was true when I decided to change `:numbers-drawn` from a vector (my original implementation) to a
list. Other than the constructor and changing `final-score` to call `(first numbers-drawn)` instead of
`(last numbers-drawn)`, everything else just worked.

What's the lesson? Encapsulation is still important at times, but using open maps and properties doesn't need to be
scary.
