(ns motif.test
  (:refer-clojure :exclude [case]))

(defonce protocols
  (do
    (defprotocol IKinded
      (kind [this]))

    (defprotocol ITyped
      (ty [this]))))

(defmacro deftypeclass
  [kind name args]
  `(let [reified# (reify
                    Object
                    (toString [_] (str (quote ~name)))

                    IKinded
                    (kind [this] ~kind)

                    ITyped
                    (ty [this#] this#)

                    ; Constructor Definition
                    clojure.lang.IFn
                    (invoke
                     ~(into '[this] args)
                     (proxy [clojure.lang.PersistentList
                             motif.test.IKinded
                             motif.test.ITyped]
                       [~'this]
                       (next [] (seq ~args))
                       (kind [] ~kind)
                       (ty [] (first ~'this)))))]

     ; type definition
     (def ~name reified#)

     ; REPL Print Method
     (defmethod print-method (type reified#)
       [val# w#]
       (.write w# (str val#)))

     ; instance? predicate
     (defn ~(symbol (str name '?))
       [any#]
       (= (ty any#) ~name))))

(defmacro data
  [name & types]
  `(do
    ; Kind Definition
    (def ~name '~name)

     ; Sub types
    ~@(map
       (fn [form]
         (if (list? form)
           `(deftypeclass
              '~name
              ~(first form)
              ~(into [] (rest form)))
           `(deftypeclass
              '~name
              ~form
              ~(into [] []))))
       types)

     ; instance? predicate
     (defn ~(symbol (str name '?))
       [any#]
       (= (kind any#) ~name))

     nil))

(defmulti fmap (fn [f [ty]] (kind ty)))

(defmulti >>= (fn [x f] (kind x)))

(data Either (Left x) (Right x))

(assert (= Right (ty Right) (ty (Right 1))))

(defmethod fmap Either
  [f [ty val :as either]]
  (if (= Left ty)
    either
    (ty (f val))))

(defmethod >>= Either
  [either f]
  (condp = (ty either)
     Left either
     Right (f (second either))))

(data Maybe Nothing (Just x))

(defmethod fmap Maybe
  [f])

(defmethod >>= Maybe
  [maybe f]
  (if (= Nothing maybe)
    Nothing
    (let [[_ val] maybe]
      (f val))))

(defn combinator
  [op]
  (fn [mx my]
    (>>= mx (fn [x] (>>= my (fn [y] ((ty mx) (op x y))))))))

(def add (combinator +))



; ----------------------------------------------------
