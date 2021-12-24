# Day Twenty-Three: Amphipod

* [Problem statement](https://adventofcode.com/2021/day/23)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day23.clj)

---

## Preamble

Well you can't win them all. I got through today's puzzle, but it's certainly not the "right" solution. My algorithms
do get to the correct answer, but they take anywhere from 5 to 10 minutes to run apiece. After I publish this, I'll
start reading other folks' solutions to uncover the "trick" to getting it to run quickly, as I sure didn't find it
myself!

Also, I made some questionable decisions in terms of the data model I used. It works, and if it didn't take so darn
long to run, I'd consider refactoring it. But I'd rather consider today a mulligan, albeit one with a working
solution, so I won't be cleaning it up very much.

---

## Part 1

### Data model and helper functions

We need a way to represent the burrow, and I decided use a structure of
`{:hallway [], :rooms {:name []}, :cost 0}`. The `:hallway` is a vector of 11 elements, initially set to all `nil` 
values since they're all unoccupied. The `:rooms` is a map of the "name" of the room (the letters we want to store
there) to a vector of its occupants from top to bottom. And the `:cost` is the total movement cost to get to this
configuration.

Looking at my code and the original problem, I see that the problem uses capital letters in the rooms, while I
subconsciously changed them all to lowercase. Huh; there ya go, brain.

First off, we'll define a few helper values. `hallway-length` is just the size of the hallway (11). `room-pos` is a
map of each room name to its position along the hallway, and `all-rooms` and `room-entranes` represent the keys and
values of the four rooms. Finally, `move-cost` calculates the travel cost of an amphipod over a number of spaces.

```clojure
(def hallway-length 11)
(def room-pos {:a 2, :b 4, :c 6, :d 8})
(def all-rooms (keys room-pos))
(def room-entrances (-> room-pos vals set))
(defn move-cost [amphipod amount] (* amount ({:a 1 :b 10 :c 100 :d 1000} amphipod)))
```

I decided not to parse the input this time, instead making `create-burrow` take in the eight symbols of the initial
room occupants, from `:a` through `:d`, from top to bottom. I then create two more helper functions, `hallway-at` and
`room-occupants`, to return the occupant of a hallway index or a named room. 

```clojure
(defn create-burrow [a0 a1, b0 b1, c0 c1, d0 d1]
  {:hallway (vec (repeat hallway-length nil))
   :rooms   {:a [a0 a1]
             :b [b0 b1]
             :c [c0 c1]
             :d [d0 d1]}
   :cost    0})

(defn hallway-at [burrow n] (get-in burrow [:hallway n]))
(defn room-occupants [burrow room] (get-in burrow [:rooms room]))
```

Each room is in one of three states - either it's full of only the correct occupants, or it has no incorrect occupants
and can receive new ones, or else it has incorrect occupants that need to be evicted. For these three functions, I
created `room-complete?`, `room-entering?`, and `room-exiting?`. Because I modeled the rooms as vectors where evicting
an occupant means placing a `nil` in its slot, the rooms will always have two elements (at least in part 1), so the
logic to determine the state of each room is a little tricky.

```clojure
(defn room-exiting? [burrow room] (let [values (room-occupants burrow room)]
                                    (and (not-every? nil? values)
                                         (not-every? #(= room (or % room)) values))))
(defn room-entering? [burrow room] (let [values (room-occupants burrow room)]
                                     (and (not-every? #(= room %) values)
                                          (every? #(= room (or % room)) values))))
(defn room-complete? [burrow room] (every? #(= room %) (room-occupants burrow room)))
```

Next, we're going to figure out which occupants would be evicted from an "exiting" room, and which rooms are "entering"
and ready to receive new occupants. `room-mover` takes in a burrow and its room name, and returns the index and value
of the top-most non-`nil` occupant.  `exiting-rooms` and `entering-rooms` return the names of all rooms that are
"exiting" and "entering," respectively. And `success?` returns true if all the rooms are "complete," meaning we have
reached the final state of the puzzle.

```clojure
(defn room-mover [burrow room] (->> (room-occupants burrow room)
                                    (keep-indexed (fn [idx v] (when v [idx v])))
                                    first))

(defn exiting-rooms [burrow] (filter #(room-exiting? burrow %) all-rooms))

(defn entering-rooms [burrow] (filter #(room-entering? burrow %) all-rooms))

(defn success? [burrow] (every? #(room-complete? burrow %) all-rooms))
```

We're almost ready to do some real work. The last helper function we need is `left-right-hallways`, which, given a
burrow and a room name, returns two lists of hallway indexes, one each facing left and right, skipping all indexes 
that represents a room entrance, since an amphipod can't ever rest there.

```clojure
(defn left-right-hallways [room]
  (let [pos (room-pos room)]
    [(remove room-entrances (range (dec pos) -1 -1))
     (remove room-entrances (range (inc pos) hallway-length))]))
```

### Leaving rooms

We know that amphipods cannot change positions in the hallway once they rest, so we can say that there are only two
types of moves in this puzzle - amphipods leaving rooms and going to an available hallway position, and hallway
amphipods walking into a room. This should simplify the puzzle, since we don't need to worry about amphipods shuffling
back and forth in the hallway.

To leave a room, we need to pick the "exiting" rooms, and for each one, map out every possible scenario where that
topmost occupant could walk to. We'll start with `hallway-destinations-from`, which returns a sequence of all
unoccupied hallway indexes outside of a chosen room, recognizing that amphipods can't go through each other. Given the
`left-right-hallways` function, we'll use `(take-while #(nil? (hallway-at burrow %)))` for each direction to return
every spot until we bump into a neighbor or hit the wall. Remember that `left-right-hallways` already filters out any
room entrances for us.

```clojure
(defn hallway-destinations-from [burrow room]
  (->> (left-right-hallways room)
       (map (fn [rng] (take-while #(nil? (hallway-at burrow %)) rng)))
       (apply concat)))
```

Let's assume we've found a "exiting" room and have picked a hallway location for its occupant; the `leave-room`
function handles moving everyone around. I put in some function pre-conditions to help me validate my inputs, but they
aren't strictly needed. First, we'll call `room-mover` to detrmine who is leaving and from what index value in the room
vector. To figure out its cost, we'll invoke `move-cost` with the total number of steps; the step count is the absolute
value distance between the target index and the room's index, plus the index of the amphipod in the room (the "depth"),
plus one more since the 0th room occupant is one space below the hallway. Then to reflect the move, we put the
amphipod in the hallway, `nil` out its previous location in the room, and add cost to its existing value.

```clojure
(defn leave-room [burrow room hallway-pos]
  {:pre [(room-exiting? burrow room)
         (nil? (hallway-at burrow hallway-pos))]}
  (let [[depth amphipod] (room-mover burrow room)
        cost (move-cost amphipod (-> room room-pos (- hallway-pos) abs (+ depth) inc))]
    (-> burrow
        (assoc-in [:hallway hallway-pos] amphipod)
        (assoc-in [:rooms room depth] nil)
        (update :cost + cost))))
```

Then to wrap things up, we'll `leave-room-options`. This goes through every "exiting" room, and `mapcats` all the
states from leaving the topmost occupant into each available hallway position.

```clojure
(defn leave-room-options [burrow]
  (mapcat (fn [room] (map #(leave-room burrow room %)
                          (hallway-destinations-from burrow room)))
          (exiting-rooms burrow)))
```

### Entering rooms

Moving an amphipod from the hallway into a room isn't very difficult from moving one out. First, we'll create
`nearest-neighbors` to determine who is closest to a room from each side of the hallway. Since `left-right-hallways`
return up to two sequences, we'll use `keep` twice to eliminate `nil` values, as there may be a neighbor on each side,
or no neighbors at all, as is the case at the beginning when the hallway is empty.

```clojure
(defn nearest-neighbors [burrow room]
  (keep (fn [spaces] (->> spaces
                          (keep #(when-some [v (hallway-at burrow %)]
                                   [% v]))
                          first))
        (left-right-hallways room)))
```

For a given room, we need to determine the farthest available index within the room. We can use `(when-not v)` to
find the indexes with `nil`, since every value other than `nil` and `false` are truthy. Then we take `last` to get the
last/farther index with a `nil` occupant.

```clojure
(defn enter-room-depth [burrow room]
  (->> (room-occupants burrow room)
       (keep-indexed (fn [idx v] (when-not v idx)))
       last))
```

Now we write `enter-room`, which looks awfully similar to `leave-room`. Given the target room and the position of the
amphipod to enter the room, we calculate the target depth, set the hallway spot to `nil`, and set the `nil` element in
the room to its new value (which is always the same name as the room itself), and increase the cost. No doubt this
would be a good place to refactor common logic with `leave-room`, but I choose not to this time.

```clojure
(defn enter-room [burrow room hallway-pos]
  {:pre [(= room (hallway-at burrow hallway-pos))
         (room-entering? burrow room)]}
  (let [depth (enter-room-depth burrow room)
        cost (move-cost room (-> room room-pos (- hallway-pos) abs (+ depth) inc))]
    (-> burrow
        (assoc-in [:hallway hallway-pos] nil)
        (assoc-in [:rooms room depth] room)
        (update :cost + cost))))
```

Finally, we implement `enter-room-options`, which again parallels `leave-room-options.` We start with the "entering"
rooms, and mapcat all of its possible states after moving elements in. However, we can only move the nearest
neighbors into the room if they have the same name as the room; we can't move amphipod `:b` into room `:a` for
instance. If they do match, then we call `enter-room`; and once again, `keep` removes all of those pesky `nil` values
for us.

```clojure
(defn enter-room-options [burrow]
  (mapcat (fn [room] (keep (fn [[pos v]] (when (= v room)
                                           (enter-room burrow room pos)))
                           (nearest-neighbors burrow room)))
          (entering-rooms burrow)))
```

### Solving part 1

To solve part 1, we'll use another sorted set, just like we did for day 21, to operate as a priority queue. Again we
will need to make a sorting function / comparator to ensure we don't lose any key data from the map. I'll admit I got
a bit lazy here again, and said we should sort by the costs of the two room configurations, but after that we can just
use a `.toString` function on the entire data set since I really don't care if the ordering isn't precise.

```clojure
(def burrow-sorter
  (letfn [(burrow-comparator [b] [(:cost b) (.toString b)])]
    (fn [b1 b2] (compare (burrow-comparator b1) (burrow-comparator b2)))))
```

Let's do it - let's writ ethe `solve` function, and now it's pretty simple after that huge amount of setup. We'll make
our sorted set of potential burrows to inspect, again favoring the cheapest one so far. If we find an option (which we
always should), we return its cost if it's a success state, since we will have reached the cheapest path to the
solution. If not, we'll look first for any options to enter a room, since moving an amphipod into its proper home is
always a safe move; nothing else could ever improve the state of that amphipod if it can enter an available room. If
there are no such options, we instead find the leave-room options and loop again.

Again, this is quite inefficient, to be sure. But it does work.

```clojure
(defn solve [burrow]
  (loop [options (sorted-set-by burrow-sorter burrow)]
    (let [b (first options)]
      (if (success? b)
        (:cost b)
        (let [enter (enter-room-options b)]
          (recur (apply conj (disj options b) (if (seq enter)
                                                enter
                                                (leave-room-options b)))))))))
```

So that brings us to our `part1` function - create the burrow and solve it.

```clojure
(defn part1 [input] (->> input (apply create-burrow) solve))
```

---

## Part 2

All we have to do for part 2 is make each room deeper, holding 4 amphipods instead of 2. This makes our slow solution
even slower, but still only by a minute or so. The `add-header-row` injects the two dummy rows into the middle of each
room vector, by way of a helper function `insert-middle`. And then `part2` calls `add-header-row` before calling
`solve`.

```clojure
(defn add-header-row [burrow]
  (letfn [(insert-middle [[v0 v1] v0' v1'] [v0 v0' v1' v1])]
    (-> burrow
        (update-in [:rooms :a] insert-middle :d :d)
        (update-in [:rooms :b] insert-middle :c :b)
        (update-in [:rooms :c] insert-middle :b :a)
        (update-in [:rooms :d] insert-middle :a :c))))

(defn part2 [input] (->> input (apply create-burrow) add-header-row solve))
```