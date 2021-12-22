# Day Twenty-One: Dirac Dice

* [Problem statement](https://adventofcode.com/2021/day/21)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day21.clj)

---

## Part 1

Today we're going to be playing some 2-player dice games. Each player rolls a die three times, moves their position
around a board track with values from 1 to 10, adding up the point value on the third space landed on.  The first
player to 1000 wins.

As always, we start with considering the data structures to use. I changed this several times, before settling on a
mix of practicality and using neat features because I can. For the game board, I'll use a straightforward structure of
type `{:players [{:pos 1, :score 0}], :next-player 0}`. The `:players` value is a two-element vector of players, each
of which being a map of `:pos` to the value from 1 to 10 around the board, and `:score` being the accumulated score
thus far. The second element in the map is `:next-player`, which is either 0 or 1 and points to the next player in the
vector to move.

We're also going to use a deterministic die, one that loops values from 1 to 100 with each roll.  A normal person would
just merge in `{:die {:next-value 1, :num-rolls 3}}` to the game, but I decided to have some fun and instead store the
die's `:values` as an infinite sequence of cycling values from 1 to 100. The good news is that I rarely get to use
`cycle` and it's a fun function. The bad news is that I can never just `println` the game anymore because the die
will forever generate the next value to print. That's what I get for trying to be cute.

There's no parsing to be done, so let's make generator functions `new-player` and `new-game`. The former takes in a
player's starting position and creates its map, and the latter takes in the two players and forms a game around them,
not counting the die.

```clojure
(defn new-player [start] {:pos start, :score 0})
(defn new-game [players] {:players (vec players), :next-player 0})
```

Next we'll make a few simple helper functions. `move-pos` takes the sum of three die rolls and the current position of
a player on the board, and returns the new position. 1-indexing is always fun, so I just made a simple recursive
function called `board-pos` to simplify the code. And finally, `swap-players` just changes the player number from 0 to
1 and back again.

```clojure
(defn board-pos [n] (if (> n 10) (board-pos (- n 10)) n))
(defn move-pos [roll pos] (board-pos (+ roll pos)))
(defn swap-players [n] (- 1 n))
```

Even though we haven't dealt with the deterministic die yet, we can still create `move-player`, which takes in the
current state of the game we just built and the sum of rolling the die three times, and returns the new state of the
game. We'll first use `:next-player` to figure out which player in the vector is moving. First we'll use `update` to
create a player who has moved `roll` number of times, by way of `move-pos`. Then with that new player on the correct
space, we'll `update` it again to add its new space to its current score. Finally, we'll replace the player back into
the game, and use `swap-players` to return the state with the next `:next-player`.

```clojure
(defn move-player [game roll]
  (let [{:keys [players next-player]} game
        player (players next-player)
        moved (update player :pos (partial move-pos roll))
        scored (update moved :score + (:pos moved))]
    (-> game
        (assoc-in [:players next-player] scored)
        (update :next-player swap-players))))
```

Alright, now let's take care of that deterministic die. As I said, I represented the die as
`{:values infinite-seq, :num-rolls n}`, so that's easy enough. Now to roll the die, I could have decided to make the
die itself stateful, such that I can roll it, drop the head of the sequence, and increment the number of rolls all at
once, but we can do this functionally and immutably fairly easily. The `roll-die` function takes in a die and the 
number of rolls we want (always 3), and returns a vector of the die after its rolls, plus the sum of the rolls. This
means just calling `take` on the next 3 values in the sequence and adding them up, and then using `update` on the die
to call `drop` the same number of times. The calling function will be responsible for replacing its die with the new
one.

```clojure
(def deterministic-die {:values (cycle (range 1 101)), :num-rolls 0})
(defn roll-die [die n]
  (let [{:keys [values]} die
        sum (apply + (take n values))]
    [(-> die
         (update :values #(drop n %))
         (update :num-rolls + n))
     sum]))
```

Speaking of the calling function, we can create `take-turn` now, and it's really simple. We'll call `roll-die` on the
game's die, extracting out the replacement die and the sum of what it rolled. Then we return the game after calling
`move-player` based on the die rolls, and then to associate in the new die.

```clojure
(defn take-turn [game]
  (let [[rolled-die sum] (-> game :die (roll-die 3))]
    (-> game
        (move-player sum)
        (assoc :die rolled-die))))
```

At every state of the game, we need to know if there's a winner, so the `winner` function will do that. The easiest
option is just to return whether any of their scores are at least as high as the target. (We'll come back in part 2 to
enhance this slightly.) Then the `play-until` function will run through all moves within the game until there is a
winner; as usual, we leverage `iterate` to generate the sequence of game states before returning the first one with a
winner.

```clojure
(defn winner [game target-score]
  (some #(>= % target-score) (map :score (:players game))))

(defn play-until [game target-score]
  (->> (iterate take-turn game)
       (filter #(winner % target-score))
       first))
```

All that's left now is to tally the final score and put it all together. `final-score` multiplies `:num-rolls` from the
latest version of the deterministic die, by the score of the next player, since the one that _didn't_ just play is the
loser of the game. Then `part1` just assembles the game, places the die into it, plays until someone hits 1000, and
returns the final score.

```clojure
(defn final-score [game]
  (let [{:keys [die players next-player]} game]
    (* (:num-rolls die)
       (:score (get players next-player)))))

(defn part1 [player1 player2]
  (-> (new-game (map new-player [player1 player2]))
      (assoc :die deterministic-die)
      (play-until 1000)
      final-score))
```

---

## Part 2

The first and most obvious thing to note is that when the problem statement has numeric values in the trillions, _don't
go with brute force!_  We're splitting universes every time we roll the magical 3-sided die, and returning in how many
universes the best player wins.

The trick here is to realize that there are only 27 possible ways to roll three dice -- 111, 112, 113, 121, 122, 123,
etc. The smallest sum of rolls is 3 (111), and the largest is 9 (333); the rest can be represented as a simple map
of the sum of rolls to the number of times it happens. Note that there are only seven possible sums, from 3 to 9.

```clojure
(def dirac-rolls {3 1, 4 3, 5 6, 6 7, 7 6, 8 3, 9 1})
```

So our strategy will be to begin with the starting position (where the total score is 0), and queue up the next seven
game states we can imagine. To avoid calculating the same game multiple times, we'll use a sorted set as our priority
queue for the next game state to inspect. As long as we always evaluate lower-scoring states before higher-scoring
ones, we'll never repeat a calculation. I learned from Day 15's Sorted Value Maps that a sorted set is just fine.

Now I won't go into the full details, but while comparators and sorting aren't bad in Clojure, you have to be really
careful about _incomplete_ comparators in sorted sets; if Clojure compares two values and sees a zero, then the sorted
set thinks it's the same value and won't store them both. This actually makes sense for Clojure, since it deals with
immutable constructs all over the place, so if two values are compared to be the same, then you don't need two of them.

```clojure
; A sorted set of two different vectors
(sorted-set [1 2] [1 3])
=> #{[1 2] [1 3]}

; A sorted set where we instruct Clojure to only compare the first value of each vector, results in the second vector
; being swallowed up!
(sorted-set-by #(< (first %1) (first %2)) 
               [1 2] [1 3])
=> #{[1 2]}
```

Who cares? Well if we're going to sort multiple game states in a sorted set, we'll need to make sure we not only sort
them logically (from lowest total score to highest), but that we also include all of the key data values in the
comparator. We'll make `game-option-sorter` to handle this work. It creates an internal helper function called
`game-compare`, which takes in a game and returns a vector of its summed up score, then each of the scores, the next
player ID, and the player positions. All we really care about is the score, but everything else must be included to 
remove accidental data deletion.

```clojure
(def game-option-sorter
  (letfn [(game-compare [g] (let [{:keys [players next-player]} g
                                  [{pos1 :pos, score1 :score} {pos2 :pos, score2 :score}] players]
                              [(+ score1 score2) score1 score2 next-player pos1 pos2]))]
    (fn [g1 g2] (compare (game-compare g1) (game-compare g2)))))
```

Ok, let's figure out what to do with the Dirac die. We'll make a function called `roll-dirac-dice`, which takes in a
game state, and returns a map of each possible game state the rolls can lead to, mapped to their frequency. This shows
us how the universe explodes after rolling the die three times.

```clojure
(defn roll-dirac-dice [game]
  (reduce (fn [acc [roll n]]
            (let [next-game (move-player game roll)]
              (u/update-add acc next-game n)))
          {}
          dirac-rolls))
```

At this point, I'm going to throw everything into one big `part2` function, because honestly I'm a little tired from
lots of refactorings today. That said, it's in a pretty decent state. We'll start by initializing the game with the
two players, and set up a `loop-recur`, since we'll want to go through all of our possible non-winning game options.
`game-options` is our sorted set of unseen game states, while `universes` is the map of all upcoming game states to
the number of paths to get there. Each time we loop, if there's an unseen game state, we'll pick the lowest-scoring
one, and roll the Dirac die to get find the possible universes we might explore. Before we recurse, we'll want to
remove the selected game state, since we will have seen it; and add in all rolled universes that aren't winners,
since a game stops once there's a winner. For the universes, we'll again remove the current game from the map, since 
we'll never look at it again; and we add in the each of the newly-seen universes, multiplied by the number of universes
that made the selected game state possible.

Note that I got tired of dealing with updating maps to add in values when the keys may not have already been present,
so I created `update-add` to the `utils` namespace to handle this for us.

Finally, once we've run out of game states without winners, it's time to get our final score. From all of the universes,
which should now only contain game states with winners, we'll map each universe to its winner and frequency, and then
group them by the winner. Finally, we add together the number of universes, and select the larger value.

```clojure
; advent-2021-clojure.utils namespace
(defn update-add [m k v] (update m k #(+ (or % 0) v)))

; advent-2021-clojure.day21 namespace
(defn part2 [player1 player2]
  (let [target 21
        initial-game (new-game (map new-player [player1 player2]))]
    (loop [game-options (sorted-set-by game-option-sorter initial-game), universes {initial-game 1}]
      (if-let [game (first game-options)]
        (let [paths-to-game (universes game)
              rolled-universes (roll-dirac-dice game)
              next-game-options (->> (keys rolled-universes)
                                     (remove #(winner % target))
                                     (apply conj (disj game-options game)))
              next-universes (reduce-kv (fn [m k v] (u/update-add m k (* v paths-to-game)))
                                        (dissoc universes game)
                                        rolled-universes)]
          (recur next-game-options next-universes))
        (->> universes
             (map (fn [[game n]] [(winner game target) n]))
             (group-by first)
             (map (comp #(apply + (map second %)) second))
             (apply max))))))
```

So we're done, right? Well not quite. I said before that the `winner` function had to change slightly. Whereas in part
1 we only needed to know if there was a winner, now we need to know who it is! So we'll just look at all players in a
game, keeping the index of whichever has a score above the threshold, and select the first one. We shouldn't have to
change anything in part 1, even though the old solution returned `true` or `nil` in the past and this one returns the
index of the winning player, since integers are as truthy as `true` is. But for part 2, the `winner` function now
returns the player index, which we use in our `group-by` function.

```clojure
(defn winner [game target-score]
  (->> (:players game)
       (keep-indexed (fn [idx {:keys [score]}] (when (>= score target-score) idx)))
       first))
```

---

## Epilogue

I know there's a way to combine parts 1 and 2 together into a combined algorithm, but I don't think I'm going to do it
this time. It just requires knowing that the deterministic die from part 1 returns only a single universe with a
single possible outcome, whereas the Dirac die creates multiple universes. In that vein, it should be fairly simple
to keep a copy of the changing die (deterministic die changes while Dirac doesn't), and either choosing the single
winning universe for part 1 or look at all of them for part 2. Maybe I'll do this in a few days, maybe not!