(ns songbook.model.core
  #?(:cljs (:require [cljs.core.match :refer-macros [match]])
     :clj  (:require [clojure.core.match :refer [match]])))
              
(defrecord Mark [position content])

(defn balance
  "Ensures the given subtree stays balanced by rearranging black nodes
  that have at least one red child and one red grandchild"
  [tree]
  (match [tree]
    [(:or ;; Left child red with left red grandchild
      [:black [:red [:red a x b] y c] z d]
      ;; Left child red with right red grandchild
      [:black [:red a x [:red b y c]] z d]
      ;; Right child red with left red grandchild
      [:black a x [:red [:red b y c] z d]]
      ;; Right child red with right red grandchild
      [:black a x [:red b y [:red c z d]]])] [:red [:black a x b]
                                                   y
                                                   [:black c z d]]
      :else tree))

(defn insert-val
  "Inserts x in tree.
  Returns a node with x and no children if tree is nil.

  Returned tree is balanced. See also `balance`"
  [tree x]
  (let [ins (fn ins [tree]
    (match tree
      nil [:red nil x nil]
      [color a y b] (cond
        (< (:position x) (:position y)) (balance [color (ins a) y b])
        (> (:position x) (:position y)) (balance [color a y (ins b)])
        :else tree)))
      [_ a y b] (ins tree)]
    [:black a y b]))

(defn find-val
  "Finds value x in tree"
  [tree x]
  (match tree
         nil       nil
         [_ a y b] (cond
                    (< (:position x) (:position y)) (find-val a x)
                    (> (:position x) (:position y)) (find-val b x)
                    :else x)))

(defn- rb-tree->tree-seq
  "Return a seq of all nodes in an red-black tree."
  [rb-tree]
  (tree-seq sequential? (fn [[_ left _ right]]
                          (remove nil? [left right]))
            rb-tree))

(defn rb-tree->seq
  "Convert a red-black tree to a seq of its values."
  [rb-tree]
  (map (fn [[_ _ val _]] val) (rb-tree->tree-seq rb-tree)))

(defn rb-tree->ordered-seq [tree]
  (match tree
         nil nil
         [_ a y b] (concat (rb-tree->ordered-seq a) [y] (rb-tree->ordered-seq b))))

;(deftype RedBlackTree [tree]
;  clojure.lang.IPersistentSet
;  (cons [self v] (RedBlackTree. (insert-val tree v)))
;  (empty [self] (RedBlackTree. nil))
;  (equiv [self o] (if (instance? RedBlackTree o)
;                    (= tree (.tree o))
;                    false))
;  (seq [this] (if tree (rb-tree->seq tree)))
;  (get [this n] (find-val tree n))
;  (contains [this n] (boolean (get this n)))
;  ;; (disjoin [this n] ...) ;; Omitted due to complexity
;  clojure.lang.IFn
;  (invoke [this n] (get this n))
;  Object
;  (toString [this] (pr-str this)))
;
;(defmethod print-method RedBlackTree [o ^java.io.Writer w]
;  (.write w (str "#rbt " (pr-str (.tree o)))))

(defn shift-mark [mark start length]
  (if (>= (:position mark) start)
    (->Mark (+ length (:position mark)) (:content mark))
    mark))

(defn shift-right [content start length]
  (match content
         nil nil
         [color a x b] [color (shift-right a start length) (shift-mark x start length) (shift-right b start length)]))

