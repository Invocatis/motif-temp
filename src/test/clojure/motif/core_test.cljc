(ns motif.core-test
  (:require
   [clojure.test :as t]
   #?(:clj [motif.core :as motif]
      :cljs [motif.core :as motif :include-macros true])))

(defn yes [x] (= x true))
(defn no [x] (= x false))

(t/deftest equality-attribute
  (t/testing "Simple attributes"
    (t/is (motif/matches? 1 1))
    (t/is (not (motif/matches? 1 2)))))

(t/deftest fn-attribute
  (t/testing "Function attributes"
    (t/is (motif/matches? {:test pos?} {:test 1}))
    (t/is (not (motif/matches? {:test pos?} {:test -1})))))

(t/deftest test-map
  (t/testing "Maps"
    (t/is (motif/matches? {:x 1} {:x 1 :y 2}))
    (t/is (not (motif/matches? {:x 1} {:y 2})))))

(t/deftest test-strict-map
  (t/testing "Strict Maps"
    (t/is (motif/matches? ^:! {:x 1 :y 2} {:x 1 :y 2}))
    (t/is (not (motif/matches? ^:! {:x 1} {:x 1 :y 2})))))

(t/deftest test-vector
  (t/testing "Vectors"
    (t/is (motif/matches? [pos? pos? neg?] [1 1 -1]))
    (t/is (motif/matches? [1 2] [1 2 3]))
    (t/is (not (motif/matches? [1] [])))))

(t/deftest test-set
  (t/testing "Sets"
    (t/is (motif/matches? #{1 2} 1))
    (t/is (not (motif/matches? #{1 2} 3)))))

(t/deftest test-strict-sets
  (t/testing "Strict Sets"
    (t/is (motif/matches? ^:! #{1 neg?} 1))
    (t/is (not (motif/matches? ^:! #{1 pos?} 1)))))

(t/deftest test-seq
  (t/testing "Seqs"
    (t/is (motif/matches? '(1) [1 1 1]))
    (t/is (motif/matches? (repeat nil?) [nil nil nil nil nil]))
    (t/is (motif/matches? (flatten (repeat (list pos? neg?))) [1 -1 1 -1 1 -1 1]))))

(t/deftest test-regex
  (t/testing "Regexes"
    (t/is (motif/matches? #"\d*" "123"))
    (t/is (motif/matches? #"\[(\d\s)*\d?\]" [1 2 3 4]))))

(t/deftest test-star-modifier
  (t/testing "Star Modifier"
    (t/is (motif/matches? ^:* #{1 2 3} [1 2 3]))
    (t/is (motif/matches? ^:* (fn [x] (pos? x)) [1 2 3]))
    (t/is (motif/matches? ^:* {:x 1 :y 2} [{:x 1 :y 2 :z 3} {:x 1 :y 2}]))
    (t/is (not (motif/matches? ^:* #{1 2} [1 4])))))

(t/deftest test-equality-modifier
  (t/testing "Equality Modifier"
    (t/is (motif/matches? ^:= {keys 1 println 2} {keys 1 println 2}))
    (t/is (not (motif/matches? ^:= {:x 1 :y 2} {:x 1 :y 2 :z 3})))))

(t/deftest test-use-modifier
  (t/testing "Use Modifier"
    (t/is (motif/matches? ^{:use not=} {:x 1} {:y 2}))))

(t/deftest test-meta-modifier
  (t/testing "Meta Modifier"
    (t/is (motif/matches? ^{:meta {:x #{1 2 3}}} {:z 2} ^{:x 1} {:z 2}))
    (t/is (not (motif/matches? ^{:meta {:x ^:! {:x 1 :y 2}}} {:z 2} ^{:x {:x 1 :y 2 :z 3}} {:z 2})))))

(t/deftest compound-test
  (t/testing "Compound Test"
    (t/is (motif/matches? ^:! ^:* #{pos? odd?} [2 4 6 8]))
    (t/is (not (motif/matches? ^:! ^:* #{pos? even?} [2 4 6 9])))))

(t/deftest match-macro
  (t/testing "Match Macro"
    (t/is
      (motif/match "Hello"
        1 "Not this one"
        [1 2 3] "Or this one"
        #{1 2 3} "Not at all"
        #"Hello" true))))

(t/deftest readme-examples
  (t/testing "Readme Examples"
    (t/is (motif/matches? 1 1))
    (t/is (not (motif/matches? 1 2)))
    (t/is (motif/matches? pos? 1))
    (t/is (motif/matches? inc 1))
    (t/is (not (motif/matches? (fn [x y]) 1)))
    (t/is (motif/matches? #"\d*" "123"))
    (t/is (motif/matches? #"\d*" "123"))
    (t/is (motif/matches? #"\[(\d*\s)*\d*\]" [1 2 3]))
    (t/is (motif/matches? [1 2] [1 2]))
    (t/is (not (motif/matches? [1 2] [1])))
    (t/is (motif/matches? [1 2] [1 2 3]))
    (t/is (motif/matches? [pos? neg?] [1 -1]))
    (t/is (motif/matches? [\a \b] "ab"))
    (t/is (motif/matches? '(1 2 3) [1]))
    (t/is (motif/matches? '(1) [1 2 3 4]))
    (t/is (motif/matches? (repeat even?) [2 2 2 2]))
    (t/is (motif/matches? {1 2} {1 2 3 4}))
    (t/is (not (motif/matches? {1 2 3 4} {1 2})))
    (t/is (motif/matches? {:key1 :value :key2 nil} {:key1 :value}))
    (t/is (motif/matches? {:key1 :value :key2 nil} {:key1 :value :key2 nil}))
    (t/is (motif/matches? {:key 1 #(count (keys %)) 1} {:key 1}))
    (t/is (motif/matches? {pos? false neg? false} 0))
    (t/is (motif/matches? #{1 2 3} 1))
    (t/is (motif/matches? #{even? odd?} 0))
    (t/is (not (motif/matches? #{pos? neg?} 0)))
    (t/is (not (motif/matches? {identity 1} {identity 1})))

    (t/is (motif/matches? ^:= {identity 1} {identity 1}))
    (t/is (not (motif/matches? {:x 1 :y 2} {:x 3 :y 4})))

    (t/is (motif/matches? ^{:use #(= (set (keys %1)) (set (keys %2)))} {:x 1 :y 2} {:x 3 :y 4}))

    (motif/matches? ^{:getter get} {pos? neg? neg? pos?} {pos? -2 neg? 2})

    (t/is (not (motif/matches? #{1 2} [1 2])))

    (t/is (motif/matches? ^:* #{1 2} [1 2]))
    (motif/matches? ^{:* 1} (fn [n] (integer? n)) [[1 2] [3 4] [5 6]])
    (t/is (motif/matches? {:y 1} ^{:x 1} {:y 1}))

    (t/is (not (motif/matches? ^{:meta {:x 2}} {:y 1} ^{:x 1} {:y 1})))

    (t/is (motif/matches? ^{:meta {:x ^:* #{1 2}}} {:y 1} ^{:x [1 2]} {:y 1}))
    (t/is (motif/matches? {:x 1} {:x 1 :y 2}))

    (t/is (not (motif/matches? ^:! {:x 1} {:x 1 :y 2})))

    (t/is (motif/matches? ^:! {:x 1 :y 2} {:x 1 :y 2}))

    (t/is (motif/matches? #{1 pos?} 1))

    (t/is (not (motif/matches? ^:! #{1 pos?} 1)))

    (t/is (motif/matches? [1 2 3] [1 2 3 4]))

    (t/is (not (motif/matches? ^:! [1 2 3] [1 2 3 4])))

    (motif/matches? ^:| {:x 1 :y 2} {:x 1 :y 3})

    (motif/matches? ^:& #{pos? even?} -2)

    (t/is (not (motif/matches? {:x 1 nil? true} {:x 1})))

    (t/is (motif/matches? {:x 1} {:x 1 nil? true}))

    (t/is (not (motif/matches? #(throw #?(:clj (Exception. "Some Exception") :cljs "Some Exception")) nil)))

    (t/is (motif/matches? (interleave (repeat odd?) (repeat even?)) [1 2 3 4]))

    (t/is (motif/matches? {first 1 last 4} [1 2 3 4]))

    (t/is (motif/matches? {0 1 1 2 2 3 3 4} [1 2 3 4]))

    (t/is (motif/matches? {some? true inc 1} 0))

    (t/is (motif/matches? {(partial reduce max) 4} [1 2 3 4]))

    (t/is (motif/matches? #{{inc 1} {dec 1}} 2))

    (t/is (motif/matches? (complement #{1 2 3}) 4))

    (t/is (motif/matches? (repeat odd?) [1 1 1 1 1 1]))

    (t/is (motif/matches? {(juxt inc dec even?) [2 0 false]} 1))

    (t/is (motif/matches? ^{:getter (fn [target key] (inc key))} {0 1 1 2 2 3} {}))
    (t/is (motif/matches? motif/_ 1))
    (t/is (motif/matches? motif/_ nil))
    (t/is (motif/matches? {:x motif/_} {:x {:y 1}}))
    (t/is (= (motif/match 1 neg? "negative" zero? "zero" pos? "positive") "positive"))))
