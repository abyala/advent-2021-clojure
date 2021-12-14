# Day Fourteen: Extended Polymerization

* [Problem statement](https://adventofcode.com/2021/day/14)
* [Solution code](https://github.com/abyala/advent-2021-clojure/blob/master/src/advent_2021_clojure/day14.clj)

---

## Preamble

Today we're going to play with string manipulation... sort of. We're given an initial polymer, and a mapping of
two-character strings to the character we must insert between them. This results in the polymer growing in size very
quickly, and we need to subtract the number of occurrences of the least common character from the most common character
after a number of generations.

I enjoyed this problem because part 1 was very simple, but part 2 took a moment to consider because it suggested I
needed a more efficient algorithm. The key point is that we don't actually care about the polymer string itself; all
that matters is how many instances of each character pair appears within the list.

For instance, given the rules `{"AB" "C", "BA" "D", "AA" "X"}`, the input polymer `"AABA"` becomes `"AXACBDA"`, while
the input polymer `"ABAA"` becomes `"ACBDAXA"`. Both of those output strings have the same 2-character pairs of
internal strings, namely `("AC" "AX" "BD" "CB" "DA" "XA")`. Therefore, the strings themselves don't actually matter;
it's just the number of each character pair in the string.

With that understanding, let's get to the code.

---

## Parts 1 and 2

First off, we'll parse the input. I took a slightly different approach from my usual strategy of parsing everything
into individual lines, based on some tricks I've seen in previous Advent solutions from the Clojure community. So
today, I still use my `split-blank-line` function to separate the first line (the template) from the multi-line rules.
But this time, I use `re-seq` on all of the rule lines together, taking apart just the words. Given that long sequence,
I call `(partition 2 words)` to collect them into pairs, then `map` each list pair into a vector, so I can collect
them all into a map. The result of parsing the input is a two-element vector of the initial template and the map of 
2-character strings to the 1-character string to inject between them.

```clojure
(defn parse-input [input]
  (let [[template rules] (u/split-blank-line input)]
    [template
     (->> (re-seq #"\w+" rules) (partition 2) (map vec) (into {}))]))
```

Now really quickly, I'm going to make a helper function called `update-add`, which sort of acts like a map with a
default value, since we saw in Day 12 that if you try to update a map by calling `inc` or `+`, when the current key is
not in the map, you'll get a NullPointerException. So `update-add` lets us still update the map by essentially allowing
the default value to be a zero. We'll use this several times in the solution.

```clojure
(defn update-add [m k n] (update m k #(+ (or % 0) n)))
```

Next we'll create the function `apply-rules`, which takes in the rule set and the frequencies of string pairs. It's
just a simple use of `reduce`, but with a heck of a deconstructor on its input. Remember that our `freqs` function
argument is a map of structure `{pair1 c1, pair2 c2}` or `{[a1 b1] c1, [a2 b2] c2}`. Since maps are themselves
sequences, we can think of the map as a big sequence of key-value pairs, like `([pair1 c1], [pair2 c2])`, which again
looks like `([[a1 b1] c1], [[a2 b2] c2])`.  Thus because the reducing function takes an accumulated value and the next
pair in the `freqs` map, we deconstruct it to `(fn [acc [[a b :as word] n]])`.

With that ready to go, the reduction is simple. We find the character `c` in the rules for our `a-b` word, and in the
next generation each of those pairs will translate into the same number of `ac` and `cb` pairs. So we'll use our handy
`update-add` function to add `n` instances of both strings to the accumulating map of frequencies.

```clojure
(defn apply-rules [rules freqs]
  (reduce (fn [acc [[a b :as word] n]] (let [c (rules word)]
                                         (-> acc
                                             (update-add (str a c) n)
                                             (update-add (str c b) n))))
          {} freqs))
```

Alright, let's work out the score for a given "string," which again we are representing as a frequency map. To start,
we need to convert the frequency map of string pairs to the frequency map of individual characters. To do that, we must
recognize that if we had the string `aabaab`, the map would be `{"aa" 2, "ab" 2, "ba" 1}`. Looking at just the first
three characters, `aab`, we note that the second character of the first pair is the first character of the second pair,
so we don't want to count both letters within each pair. We just need to count the first of each pair, so long as we
add in the very last letter of the _original_ input, since that will never change from generation to generation. Once
we have that value, named below `char-freqs`, we need to find the largest and smallest frequency; note again that we
don't care about the characters themselves, so we can call `(sort-by - (vals char-freqs))` to pull out the frequency
counts using `vals` before sorting from largest to smallest.  Finally, we use `juxt` to grab the first and last values
(largest and smallest, respectively) and subtract the two.

```clojure
(defn score [initial-template freqs]
  (let [char-freqs (update (reduce (fn [acc [[a] n]] (update-add acc a n))
                                   {} freqs)
                           (last initial-template) inc)
        sorted-instances (sort-by - (vals char-freqs))]
    (apply - ((juxt first last) sorted-instances))))
```

We're in the home stretch, because it's time for our solve function! First we'll parse the input, and then we need to
transform the polymer template into the initial frequency map by pairing together all of the characters before calling
`frequencies`. Once we have that map, we'll use `iterate` to make an infinite sequence of new frequency maps for each
generation, skip to the correct generation, and score it. Remember that we need to pass in the original template to the
`score` function so we get that last character.

```clojure
(defn solve [input step]
  (let [[template rules] (parse-input input)
        initial-freqs (->> template (partition 2 1) (map (partial apply str)) frequencies)]
    (->> (iterate (partial apply-rules rules) initial-freqs)
         (drop step)
         first
         (score template))))
```

Now we just have to invoke the `solve` method with the generation number, which is `10` for part 1 and `40` for part 2.

```clojure
(defn part1 [input] (solve input 10))
(defn part2 [input] (solve input 40))
```