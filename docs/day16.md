# Day Sixteen: Packet Decoder

* [Problem statement](https://adventofcode.com/2021/day/16)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day16.clj)

---

## Part 1

Today we did a bunch of parsing of recursive data structures. There's a voice in the back of my minder which tells me
we're going to reuse today's problem again in future problems; I could easily foresee a problem stating "you take out
your trust Pocket Decoder and parse the values slightly different," but my experience last year tells me YAGNI, so
let's proceed without this potential foreknowledge!

We're going do a bunch of manipulation with hex strings, binary strings that require padding, and conversions to
decimal values. So perhaps let's start with a few helper functions that we'll need later. First off,
`binary-to-decimal` converts a string representing a binary number into its decimal form. I learned the hard way to use
Longs instead of Ints; Clojure does a good job of expanding Ints to Longs most of the time, but apparently not always?
Anyway, we just use Java's `Long.parseLong` method here.

Then we need to left-pad a string with a repeated character. We can do that with a simple recursive function, but I
rewrote it in a more functional way. `left-pad-to-multiple` checks to see if the incoming string `s` is a multiple of
`mult`. If so, just return it. Otherwise, we use `(repeat (- mult rem) c)` to make a sequence of the pad character `c`
of the length needed to get to the multiple, and then use `reduce` to add each character to the front of the string.

```clojure
; advent-2021-clojure.utils namespace
(defn binary-to-decimal [s] (Long/parseLong s 2))

; advent-2021-clojure.day16
(defn left-pad-to-multiple [s mult c]
  (let [rem (mod (count s) mult)]
    (if (zero? rem) s (reduce #(str %2 %1) s (repeat (- mult rem) c)))))
```

Ok, it's time to start the puzzle, and today's puzzle is almost entirely focused on parsing. The goal, for now, is to
take in a hex string, convert it into a binary string, and return a datatype representing a packet. A packet always has
a `:version` and a `:packet-type`; if it's a literal packet then it will also have a `:value`, or else it will have one
or more `:sub-packets` in a vector. Naturally, this comes across nicely as a map, which roughly looks like:

```clojure
{:version 1, :packet-type 4, :value 123} ; Literal packet
{:version 2, :packet-type 2, :sub-packets [{:version 6, :packet-type 4, :value 123},
                                           {:version 9, :packet-type 4, :value 888}]}
```

### Parsing packets

Now the trick to our parsing is that we read the entire string once from left to right, which means that as we read
characters in the string, we need to continue parsing the substring after what we've read. This would probably be
much easier to do with mutation, like with passing around an `atom` of the input string we haven't read yet, but I
wanted to keep everything pure. So that means almost every function has to return both the packet that it parsed, but
also the remaining input string to be read; this seemed simpler than passing back the total length of the string that
was read, although I think that's a respectable option too.

So let's make sure that `parse-packet` makes sense.

```clojure
(defn parse-packet [input] (first (parse-sub-packet input)))
```

Whew! That was rough! `parse-sub-packet` is the worker function that takes in a string and returns `[packet leftovers]`,
so `parse-packet` just returns the packet itself.

`parse-sub-packet` isn't so bad, especially since I decided to stay away from regular expressions this time around.
Since every packet has a version and a packet type, we'll parse those out from their set locations, converting the
binary values into their decimal representations; technically this isn't necessary for `packet-type`, but I found it
easier to look at... especially if I expect to see us reuse this logic in a future problem! We're told that we parse
literal packets separately from operator packets, so we'll pass the partially constructed packet with the remaining
payload to either `parse-literal-packet` or `parse-operator-packet`. Remember that we expect `parse-sub-packet` to
return a two-element vector of `[packet remaining-input]`, which means we place the same expectation on
`parse-literal-packet` and `parse-operator-packet`.

```clojure
(defn parse-sub-packet [input]
  (let [version (binary-to-decimal (subs input 0 3))
        packet-type (binary-to-decimal (subs input 3 6))
        packet {:version version, :packet-type packet-type}
        payload (subs input 6)]
    (if (literal-value? packet-type)
      (parse-literal-packet packet payload)
      (parse-operator-packet packet payload))))
```

### Parsing literal packets

Parsing a literal packet take a little work, because we need to look at the input, break in into 5-character groups,
and use all of the groups up to and including first one that begins with a 0. Then we take each matching group, strip
off the first character, combine them into one large binary number, and convert it to decimal. Let's break this logic
apart.

`literal-value-portion` takes in a string and returns only the portion of that string which we need to get the literal
value. We'll use the `split-with` function, which returns a 2-element vector of `[(take-while pred) (drop-while pred)]`.
In our case, we'll use `(partition 5 s)` to get our 5-element partitions, and `(split-with #(-> % first (= \1)))` to
return a sequence of character sequences that begin with a `1`, and then a sequence of character sequences such that
at least the first one begins with a `0`. We'll immediately destructure that into `[ones [first-zero]]` since we only
want the first element of the second sequence. To reassemble a string out of these sequences of characters, we'll
first use `concat` to add the `first-zero` to the end of the sequence of `ones`, then flatten it all down into one big
sequence of characters, and put them all back together into a string.

```clojure
(defn literal-value-portion [s]
  (let [[ones [first-zero]] (split-with #(-> % first (= \1)) (partition 5 s))]
    (->> (concat ones first-zero) flatten (apply str))))
```

`padded-binary-to-decimal` repeats some of the logic in `literal-value-portion`, by again breaking apart the "value"
portion of a literal value packet into its groups of 5-character sequences. We drop the first character by calling
`rest`, then again flatten the sequences and call `apply str`, and finally use `binary-to-decimal` to get the value of
the packet.

```clojure
(defn padded-binary-to-decimal [s]
  (->> s (partition 5) (map rest) flatten (apply str) binary-to-decimal))
```

Finally, `parse-literal-packet` has to create its packet and return it in a tuple with the unused portion of the input.
After binding `portion` to the part of the input string we need, we'll associate its `padded-binary-to-decimal` onto
the `:value` property of the packet, and return a substring of `input` for the portion we didn't use.

```clojure
(defn parse-literal-packet [packet input]
  (let [portion (literal-value-portion input)]
    [(assoc packet :value (padded-binary-to-decimal portion)) (subs input (count portion))]))
```

### Parsing operator packets

There are two types of operator packets - total-length and num-sub-packet operators. All that `parse-operator-packet"`
does is decide which length-type operator we have, initialize the `:sub-packets` property of the packet to be an
empty vector, and then call either `parse-total-length-operator` or `parse-num-sub-packets-operator`. Again, we expect
both of those functions to return the two-element vector that all parse functions return.

```clojure
(defn parse-operator-packet [packet input]
  (let [length-type-id (binary-to-decimal (subs input 0 1))
        remainder (subs input 1)
        op-packet (assoc packet :sub-packets [])]
    (if (total-length-type? length-type-id)
      (parse-total-length-operator op-packet remainder)
      (parse-num-sub-packets-operator op-packet remainder))))
```

`parse-total-length-operator` and `parse-num-sub-packets-operator` are very similar functions, to the point that I
refactored much of their structure into a shared function. In both cases, we are given a packet, such that we need to
add one or more `:sub-packets` to it. To do so, we'll read the next sub-packet out of the input, add it to the packet,
and then check to see if we're done. If not, we read another sub-packet. If we're done, return the expanded packet
along with the remaining string we didn't use.

```clojure
(declare parse-sub-packet)

(defn parse-sub-packets [finished? packet input]
  (loop [p packet, remainder input]
    (if (finished? p remainder)
      [p remainder]
      (let [[sub-packet leftovers] (parse-sub-packet remainder)]
        (recur (update p :sub-packets conj sub-packet) leftovers)))))
```

Quick note here before we get to the `finished?` function in the argument list. Remember that `parse-packet` calls
`parse-sub-packet`, which in turn made its way to `parse-sub-packets`, which in turn calls `parse-sub-packet`. Unlike
Java and Kotlin, Clojure's reader goes from top to bottom, so ordinarily we could never represent a circular
dependency between functions because they can't both be evaluated at the same time. So we'll need to use
`(declare parse-sub-packet)` to tell Clojure that we're going to later make a definition of `parse-sub-packet`, but for
now to recognize it as a valid symbol to be used later.

Let's define the `parse-total-length-operator`, such that it uses `parse-sub-packets` by passing in the correct
predicate. A total length operator knows exactly how much of the input string to use, so after interpreting the first
15 characters of the string as the length of the sub-packets, we split the rest of the input into the
`sub-packet-payload` and the `non-sub-packet-payload`. We'll call `parse-sub-packets`, using a function that returns
`true` if the entire `sub-packet-payload` has been consumed, meaning that it's blank now. Remember that these functions
all return two-element vectors, so we only need the first element of `parse-sub-packets` since the second will be an
empty string, and then we have to combine it again with `non-sub-packet-payload` so the rest of the parsing can 
continue.

```clojure
(defn parse-total-length-operator [packet input]
  (let [sub-packet-length (parse-binary (subs input 0 15))
        sub-packet-payload (subs input 15 (+ 15 sub-packet-length))
        non-sub-packet-payload (subs input (+ 15 sub-packet-length))]
    [(first (parse-sub-packets (fn [_ remainder] (str/blank? remainder)) packet sub-packet-payload))
     non-sub-packet-payload]))
```

Ironically, the `parse-num-subpackets-operator` function is much simpler. We'll pull out the first 11 digits to tell
us how many sub-packets to expect, and then we'll call `parse-sub-packets` again. This time, the function will check
to see if the number of sub-packets on the packet is equal to the count we're expecting. This time, however, since we
don't know how much of the input that `parse-sub-packets` will use, and since the latter will return the two-element
vector of the completed packet and the rest of the input, that's exactly what we want to return here, so there's no
need to construct a new vector!

```clojure
(defn parse-num-sub-packets-operator [packet input]
  (let [num-sub-packets (parse-binary (subs input 0 11))]
    (parse-sub-packets (fn [p _] (= num-sub-packets (-> p :sub-packets count))) packet (subs input 11))))
```

### Finishing part 1

Umm... are we done?  Let's recap what we did. We know how to take in the input hex string and turn it into a giant
binary string. We know how to parse the string into a packet, which is made of one or more sub-packets. For each
sub-packet, we can represent a literal value packet which has a numeric value, or an operator packet which has one
or more sub-packets. So the only thing left for us to do is figure out how to add up all of the version of every
packet and sub-packet.

The `add-versions` function is simple - we need to add the packet's version to the sum of its sub-packets' versions; 
this is a single `reduce` function! We'll initialize with the version of the packet, reducing over the sub-packets,
adding their values together.

Then for `part1`, we read the input string as binary, parse it, and call `add-versions` to get the star.

```clojure
(defn add-versions [packet]
  (reduce #(+ %1 (add-versions %2))
          (:version packet)
          (:sub-packets packet)))

(defn part1 [input]
  (-> input hex-packet-to-binary-string parse-packet add-versions))
```

---

## Part 2

Compared to part 1, part 2 was very simple. To calculate the total value of the packets, we leverage the packet type
of operator packets. Now I decided _not_ to calculate the value as part of the parsing process, because I think that
parsing and evaluating are distinctly different tasks. Plus... I still suspect we're going to reuse this code, darn it!

So we'll make a simple `value-of` function, which returns either the `:value` of the packet for a literal value, or
else applies the correct operator. Some operations apply to all sub-packets (addition, multiplication, min and max),
while some apply to only the first two (greater-than, less-than, and equal-to). For the sake of clarity, I made these
look a little different from each other. We call `(map value-of (:sub-packets packet))` to get a sequence the values
of all sub-packet, which we then construct into `[sub-a sub-b :as sub-values]`. For the first 4 operators, we call
`(apply op sub-values)`, while for the comparison operators we call `(if (op sub-a sub-b) 1 0)` instead. I could have
put in some more reuse, but this is just easy to read.

Finally, part 2 just parses the packet again, this time calling `value-of` for the solution.

```clojure
(defn value-of [packet]
  (or (:value packet)
      (let [[sub-a sub-b :as sub-values] (map value-of (:sub-packets packet))]
        (case (:packet-type packet)
          0 (apply + sub-values)
          1 (apply * sub-values)
          2 (apply min sub-values)
          3 (apply max sub-values)
          5 (if (> sub-a sub-b) 1 0)
          6 (if (< sub-a sub-b) 1 0)
          7 (if (= sub-a sub-b) 1 0)))))

(defn part2 [input]
  (-> input hex-packet-to-binary-string parse-packet value-of))
```
