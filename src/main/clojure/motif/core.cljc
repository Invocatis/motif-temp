(ns motif.core)

(declare compile-pattern)

(defn _
  [any]
  "Any predicate, returns true on all inputs"
  true)

(defn- and-pattern
  [p1 p2]
  (fn [target] (and (p1 target) (p2 target))))

(defn- strict?
  [pattern]
  (-> pattern meta :!))

(defn- compile-meta
  [pattern accessor]
  (and-pattern
    (compile-pattern (with-meta pattern (dissoc (meta pattern) :meta)) accessor)
    (compile-pattern (:meta (meta pattern)) (comp meta accessor))))

(defn- compile-star
  [pattern accessor]
  (let [{star-value :*} (meta pattern)
        meta (if (and (number? star-value) (> star-value 0))
               (update (meta pattern) :* dec)
               (dissoc (meta pattern) :*))
        matcher (compile-pattern (with-meta pattern meta))]
    (fn [target] (every? matcher (accessor target)))))

(defn- compile-question
  [pattern accessor]
  (let [{?value :?} (meta pattern)
        ?value (if (boolean? ?value) 1 ?value)
        ?value (if (number? ?value) #(>= % ?value) ?value)
        meta (dissoc (meta pattern) :?)
        matcher (compile-pattern (with-meta pattern meta) accessor)]
    (fn [target] (apply ?value [(count (filter matcher (accessor target)))]))))

(defn- compile-strict-question
  [pattern accessor]
  (let [{?value :!?} (meta pattern)
        ?value (if (boolean? ?value) 1 ?value)
        meta (dissoc (meta pattern) :!?)
        matcher (compile-pattern (with-meta pattern meta) accessor)]
    (fn [target] (apply #(= % ?value) [(count (filter matcher (accessor target)))]))))

(defn- compile-use
  [pattern accessor]
  (fn [target] ((:use (meta pattern)) pattern (accessor target))))

(defn- compile-element
  [pattern accessor]
  (cond
    (fn? pattern) #(boolean (pattern (accessor %)))
    :else #(= (accessor %) pattern)))

(defn- compile-simple-map
  [pattern accessor]
  (let [getter (-> pattern meta :getter)]
    (reduce
      (if (-> pattern meta :|)
        some-fn
        every-pred)
      (map
        (fn [[k v]]
          (let [acc (or (when getter #(getter % k)) (if (ifn? k) k #(get % k)))]
            (compile-pattern v
              (comp acc accessor))))
        pattern))))

(defn- compile-map
  [pattern accessor]
  (if (empty? pattern)
    (if (strict? pattern)
      (fn [target] (empty? (accessor target)))
      (fn [target] true))
    (if (strict? pattern)
      (and-pattern
        (compile-simple-map pattern accessor)
        (fn [target] (every? (partial contains? pattern) (keys (accessor target)))))
      (compile-simple-map pattern accessor))))

(defn- compile-seq
  [pattern accessor]
  (fn [value]
    (let [n (count (accessor value))]
      (every?
        boolean
        (map
          #(apply % [value])
          (take n
            (map-indexed
              (fn [i p]
                (compile-pattern p
                  (comp #(nth % i nil) accessor)))
              pattern)))))))

(defn- compile-set
  [pattern accessor]
  (let [subpatterns (map #(compile-pattern % accessor) pattern)]
    (cond
      (strict? pattern)
      (fn [target]
        (= 1 (count (filter identity (map (fn [sp] (sp target)) subpatterns)))))
      (-> pattern meta :&)
      (fn [target]
        (every? identity (map (fn [sp] (sp target)) subpatterns)))
      :else
      (fn [target]
        (not (empty? (filter identity (map (fn [sp] (sp target)) subpatterns))))))))

(defn- compile-simple-vector
  [pattern accessor]
  (let [subpatterns (map-indexed
                      (fn [i p] (compile-pattern p (comp #(nth % i) accessor)))
                      pattern)]
    (fn [target]
      (and
        (<= (count pattern) (count (accessor target)))
        (every? #(% target) subpatterns)))))

(defn- compile-vector
  [pattern accessor]
  (cond
    (strict? pattern)
    (and-pattern
      (fn [target] (= (count pattern) (count (accessor target))))
      (compile-simple-vector pattern accessor))

    (or (-> pattern meta :|) (-> pattern meta :&))
    (compile-set pattern accessor)

    :else
    (compile-simple-vector pattern accessor)))

(defn- compile-regex
  [pattern accessor]
  (fn [value]
    (boolean (re-matches pattern (-> value accessor str)))))

(def ^:private regex-type (type #""))

(defn- regex?
  [any]
  (= (type any) regex-type))

(defn compile-pattern
  ([pattern]
   (compile-pattern pattern identity))
  ([pattern accessor]
   (cond
     (-> pattern meta :*)
     (compile-star pattern accessor)

     (-> pattern meta :?)
     (compile-question pattern accessor)

     (-> pattern meta :meta)
     (compile-meta pattern accessor)

     (-> pattern meta :=)
     (compile-use (with-meta pattern (assoc (meta pattern) :use =)) accessor)

     (-> pattern meta :use)
     (compile-use pattern accessor)

     (map? pattern)
     (compile-map pattern accessor)

     (set? pattern)
     (compile-set pattern accessor)

     (vector? pattern) (compile-vector pattern accessor)

     (seq? pattern) (compile-seq pattern accessor)

     (regex? pattern) (compile-regex pattern accessor)

     :else (compile-element pattern accessor))))

(defn matches?
  "Given a pattern, and an expression, recursively determines
  if the expression matches the pattern.

  For f, a function, and x, any expression:

    (match f e) => (f e)

  For vector patterns, each ordinal spot is checked:

    (matches? [p0 p1 p2] [t0 t1 t2]) =>
        [t0 t1 t2 ...]
          ↑  ↑  ↑      matches?
        [p0 p1 p2 ...]

    Vectors ensure their targets are at least as long as they are.
    Strict vectors must have identical lengths.

  For lazy sequence patterns, like vectors, each oridnal spot is checked:

    (matches? (p0 p1 p2) (t0 t1 t2)) =>
        (t0 t1 t2 ...)
          ↑  ↑  ↑      matches?
        (p0 p1 p2 ...)

    Lazy seqs targets can be shorter, or longer, than they are.
    Infinite sequences can be used, though if they are matched against
    inifinte targets, a infinte loop will happen

  For m, a map with keyset {k1,k2,...,kn}, and n, a map:

    (match m n) => (and (match (get m k1) (get n k1))
                        (match (get m k2) (get n k2))
                        ...

    If the key is an ifn, it will be applied to the target instead.
    Strict maps require that the pattern contains all keys of the target.

  Set patterns are disjunctive, and only require one of their elements to match.

    (match m n) => (or (match m0 n)
                       (match m1 n)
                       ...

    Strict set patterns require exactly one element to match.

  For any pattern not described above, equality is checked.

    (match 1 2) => (= 1 2)

  Given the expression passed matches the given pattern,
  true will be returned. Otherwise, false will be returned.


  Meta tag modifers can enhance and change how each pattern functions.

    ^:!
      Strict modifier is defined for each pattern type
    ^:=
      Equality modifier forces equality to be used, rather than matches?
    {^:use f}
      Use mofider forces f to be used as predicate, rather than matches?
    ^:*
      Star modifier maps pattern over target, expecting all to match
    {^:meta m}
      Meta modifier matches m to the meta of the target
  "
  ([pattern]
   (fn [target]
     (try
       (apply (compile-pattern pattern) [target])
       (catch #?(:clj Exception :cljs :default) _ false))))
  ([pattern expr]
   (try
     (apply (compile-pattern pattern) [expr])
     (catch #?(:clj Exception :cljs :default) _ false))))

(defmacro match
  "Takes a subject expression, and a set of clauses.
  Each clause should be of the form:

    pattern resultant

  For each clause, (match pattern expr) is performed. If it
  returns logical true, the clause is a match and the resultant
  is returned. A single default expression can follow the clauses
  and its value will be returned if no clause matches. If no
  default expression is provided, and no clause matches, nil will
  be returned"
  ([expr]
   nil)
  ([expr default]
   `~default)
  ([expr pattern result & statements]
   `(if (matches? ~pattern ~expr)
      ~result
      ~(cons `match (cons expr statements)))))
