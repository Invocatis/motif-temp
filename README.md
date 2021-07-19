# motif

** Note this repo is currently frozen while we fix some issues with the repo **

Recursive, data driven, pattern matching for Clojure

## Releases and dependency information

[![CircleCI](https://circleci.com/gh/Invocatis/motif/tree/master.svg?style=svg)](https://circleci.com/gh/Invocatis/motif/tree/master)

```
[motif "1.0.1"]
```
```
<dependency>
  <groupId>motif</groupId>
  <artifactId>motif</artifactId>
<<<<<<< HEAD
  <version>1.0.1</version>
=======
  <version>1.0.1\</version>
>>>>>>> master
</dependency>
```
## Overview

Motif brings expressive pattern matching to Clojure. Motif focuses on using the power of our core Clojure data structures, giving each its own meaning and possibilities.

Let's get started!

```clojure
(require '[motif.core :refer [matches?] :as motif])
```

## Patterns

### Value Patterns
Simple value patterns are simply compared via equality.

```clojure
(matches? 1 1) ;=> true

(matches? 1 2) ;=> false
```
### Function patterns
Functions as patterns are invoked on the given target. Monadic predicates are only supported.

```clojure
(matches? pos? 1) ;=> true

(matches? string? 1) ;=> false

(matches? inc 1) ;=> true

(matches? (fn [x y]) 1) ;=> false, due to ArityException
```

### Regex patterns
Regex patterns are compared to the string representations of their targets as with clojure.core/re-matches.

```clojure
(matches? #"\d*" "123") ;=> true

(matches? #"\d*" 123) ;=> true

(matches? #"\[(\d*\s)*\d*\]" [1 2 3]) ;=> true
```

### Vector patterns
Vectors compare each element oridnally, that is the first element of the pattern is compared to the first of the target, the second to the second, and so on. Vectors require their targets to be at least as long as their patterns, but the targets can be longer than the patterns.

```clojure
(matches? [1 2] [1 2]) ;=> true

(matches? [1 2] [1]) ;=> false

(matches? [1 2] [1 2 3]) ;=> true

(matches? [pos? neg?] [1 -1]) ;=> true

(matches? [\a \b] "ab") ;=> true
```

### Seq patterns
Seq patterns work similarly to vector patterns, however, they can be longer or shorter than their targets. This allows for infinite length sequences to be used as patterns, however, beware of infinite loops!

```clojure
(matches? '(1 2 3) [1]) ;=> true

(matches? '(1) [1 2 3 4]) ;=> true

(matches? (repeat even?) [2 2 2 2]) ;=> true

(matches? (repeat odd?) (repeat 1)) ;=> infinite loop!
```
### Map patterns
Maps compare corresponding key values with `matches?` Maps a conjunctive, and thus all keys much match positively. Extra keys in the target map are acceptable, and ignored by the pattern. Keys in the pattern are not required to be in the target map.

```clojure
(matches? {:key :pattern-value} {:key :target-value}) ;== (matches? :pattern-value :target-value)

(matches? {1 2} {1 2 3 4}) ;=> true

(matches? {1 2 3 4} {1 2}) ;=> false

(matches? {:key1 :value :key2 nil} {:key1 :value}) ;=> true

(matches? {:key1 :value :key2 nil} {:key1 :value :key2 nil}) ;=> true
```

If a key in the pattern is an function (`ifn?`), it is instead applied to the target as apposed to gotten with `get`; this opens up another dimension of expressiveness using maps.

```clojure
(matches? {:key 1 #(count (keys %)) 1} {:key 1}) ;=> true

(matches? {pos? false neg? false} 0) ;=> true
```

### Set patterns

Sets are our disjunctive patterns; they need only one of their elements match against the target.

```clojure
(matches? #{1 2 3} 1) ;=> true

(matches? #{even? odd?} 0) ;=> true

(matches? #{pos? neg?} 0) ;=> false
```

## Modifiers

Modifiers extend, specify, and hack how the patterns work. Modifiers are attached to their patterns as metadata values.

### Equality Modifier

The Equality modifier directs motif to compare values using the `=` function, rather than `matches?`. We can use this when motif's special interpretations might get in the way.

```clojure
(matches? {identity 1} {identity 1}) ;=> false

(matches? ^:= {identity 1} {identity 1}) ;=> true
```

### Use Modifier

The Use modifier explicitly directs motif to use a different function than `matches?`. The Equality modifier is a special case of the use modifier.

```clojure
(defn same-keys
  [m0 m1]
  (= (set (keys m0))
     (set (keys m1))))

(matches? {:x 1 :y 2} {:x 3 :y 4}) ;=> false

(matches? ^{:use same-keys} {:x 1 :y 2} {:x 3 :y 4}) ;=> true
```

### Getter Modifier

Exclusive to maps, this modifier explicitly defines how keys are used to access their values in maps. In cases where you have a map with function keys, this may be useful to stop them from being applied to the map and instead be gotten with `get`.

```clojure
(matches? ^{:getter get} {pos? neg? neg? pos?} {pos? -2 neg? 2}) ;=> true
```

### Star Modifier

The Star modifier maps the pattern over the target, requiring all elements to match. More specifically, the target is seq'd, and each element is matched against the pattern. If every element matches, the pattern matches.

```clojure
(matches? #{1 2} [1 2]) ;=> false

(matches? ^:* #{1 2} [1 2]) ;=> true
```

Additionally, a positive integral value can be passed to the star modifier. The number will define how many extra times motif should seq targets before matching against the pattern. The default value is `0`, and thus only does one seq.

```clojure
(matches? ^{:* 1} (fn [n] (integer? n)) [[1 2] [3 4] [5 6]]) ;=> true
```

### Meta Modifier

Since modifiers take up the information space of the structures metadata, the Meta Modifier adds another area for metadata to be matched. That is, the metadata of the target is matched against the values in the Meta Modifier. The values are matched in the same way as in `matches?`, so all patterns discussed work within the modifier.

```clojure
(matches? {:y 1} ^{:x 1} {:y 1}) ;=> true

(matches? ^{:meta {:x 2}} {:y 1} ^{:x 1} {:y 1}) ;=> false

(matches? ^{:meta {:x ^:* #{1 2}}} {:y 1} ^{:x [1 2]} {:y 1}) ;=> true
```

### Strict Modifier

The Strict modifier is interpreted different for each pattern type, though, it in general reduces some of the laxness that our patterns might have.

#### Strict Maps
Strict maps require equality in keys.

```clojure
(matches? {:x 1} {:x 1 :y 2}) ;=> true

(matches? ^:! {:x 1} {:x 1 :y 2}) ;=> false

(matches? ^:! {:x 1 :y 2} {:x 1 :y 2}) ;=> true
```

#### Strict Sets
Sets require at least one element to match the target. Strict sets strengthen this requirement to require one and only one element to match the target.

```clojure
(matches? #{1 pos?} 1) ;=> true

(matches? ^:! #{1 pos?} 1) ;=> false
```

#### Strict Vectors
Strict Vectors require the length of their targets be the same as their own length.

```clojure
(matches? [1 2 3] [1 2 3 4]) ;=> true

(matches? ^:! [1 2 3] [1 2 3 4]) ;=> false
```

## Logical Implementations

It is worth noting the implicit ability to create logical patterns using our given tools. Maps require all pattern elements to match, a natural implementation of `and`. Sets require only one pattern to match, being an implementation of `or`. Finally, strict sets require one and only one element match the pattern, a perfect representation of `xor`. Here's a few examples:

```clojure
; Or
(matches? #{integer? pos? odd?} -2) ;=> true

; And
(matches? {integer? true pos? true even? true} 2) ;=> true

; Xor
(matches? ^:! #{pos? neg?} 1) ;=> true
(matches? ^:! #{pos? neg?} 0) ;=> false
(matches? ^:1 #{pos? odd?} 1) ;=> false
```

Here we find ourselves with a slight discomfort: It can increase tedium and decrease legibility to write `true` as a value for simple `and` maps. Similarly, `or` as sets has less robustness since only boolean values can be compared. To fix this, we have additional modifiers `^:&` and `^:|` to imply conjunctive/disjunctive natures, respectively.

```clojure
; or map
(matches? ^:| {:x 1 :y 2} {:x 1 :y 3}) ;=> true

; and set
(matches? ^:& #{pos? even?} -2) ;=> false
```

## Some things to note

### Non-symmetry
matches? is not symmetric! That is (matches? a b) does not imply (matches? b a). This is due to patterns being treated differently that their targets. Consider the following:
```clojure
(matches {:x 1 nil? true} {:x 1}) ;=> false

(matches {:x 1} {:x 1 nil? true}) ;=> true
```

This dissymmetry is due to how various structures are interpreted when they are used as patterns.

### Exceptions are failures

Any exceptions thrown my the patterns, or motif itself, are treated as general failures, and simply cause `matches?` to return `false`

```clojure
(matches? #(throw (Exception. "Some Exception")) nil) ;=> Exception Some Exception
```

However, be aware that exceptions will be thrown during the pattern compilation step; so if there's something wrong with your pattern, it will be represented as an exception.

### Ordering of Patterns

In most instances, clojure implements sets and maps using hashing, which does not guarantee ordering. There may arise some cases where you want one element of a pattern to be executed before another. In these cases one should remember the use of `array-map` and, though not included in core, `ordered-set`.

### Modifier Tag Precedence

As some modifiers completely change the course of interpretation, there is an implicit precedence in which some tags nullify others.

The `^:use` and `^:=` tags void all other tags, as the `matches?` semantics are disregarded.

Strictness in sets `^:!` supersedes conjuction `^:&`.

Strictness in maps `^:!` does not interfere with disjunction `^:|`, though will result in strange effects. It is likely these effects are not desired, so avoid using both.

Meta `^:meta` and star `^:*` tags play nicely with others.

### Modifiers on Functions

For reasons known only to the compiler, meta tags are not picked up when applied to functions stored in vars. However, there are many reasons one might want to apply our modifiers to functions as well. To accomplish this, one can either use meta tags on function literatls of the form `(fn [...] ...)`, or one can use the `with-meta` function.

### Any Function
We've added a convenience function to the library. `_` is the same as `clojure.core/any?`, but has a more idiomatic feel to it.

```clojure
(require '[motif.core :refer [_ matches?]])
(matches? _ 1) ;=> true
(matches? _ nil) ;=> true
(matches? {:x _} {:x {:y 1}}) ;=> true
```

### Match Macro
Included in `motif.core` is the`match` macro. `match` works the same as `(condp = x ...)`.

```clojure
(match 1
  neg? "negative"
  zero? "zero"
  pos? "positive") ;=> positive
```

## That's it!

That's all you need to go out into the world. But before you go, let's look at some fun examples that might help illuminate some of the possibilities:

```clojure
(matches? (interleave (repeat odd?) (repeat even?)) [1 2 3 4]) ;=> true

(matches? {first 1 last 4} [1 2 3 4]) ;=> true

(matches? {0 1 1 2 2 3 3 4} [1 2 3 4]) ;=> true

(matches? {some? true inc 1} 0) ;=> true

(matches? {(partial reduce max) 4} [1 2 3 4]) ;=> true

(matches? (complement #{1 2 3}) 4) ;=> true

(matches? {:x nil keys ^:* #{:x}} {:x nil}) ;=> true

(matches? {:x nil keys ^:* #{:x}} {:x nil :y nil}) ;=> false

(matches? ^:* ^:! {:x pos?} [{:x 1} {:x 2} {:x 3} {:x 4}]) ;=> true

(matches? ^{:meta {(comp set keys) ^:= #{:x :y}}} {:a pos?} ^{:x 1 :y 2} {:a 1}) ;=> true

(matches? ^{:use clojure.set/subset?} #{1 2 3} #{1 2 3 4}) ;=> true

(matches? ^{:getter (fn [target key] (inc key))} {0 1 1 2 2 3} {})
```
