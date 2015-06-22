(ns songbook.model.core
  (:require
    #?(:cljs [cljs.core.match :refer-macros [match]]
       :clj  [clojure.core.match :refer [match]])
    [songbook.utils.core :refer [between]]))

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
        :else [color a x b])))
      [_ a y b] (ins tree)]
    [:black a y b]))

(defn find-val
  "Finds value x in tree"
  [tree x]
  (match tree
         nil       nil
         [_ a y b] (cond
                    (< x (:position y)) (find-val a x)
                    (> x (:position y)) (find-val b x)
                    :else y)))

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

(defn shift-mark [mark start length]
  (if (>= (:position mark) start)
    (update mark :position #(+ length %))
    mark))

(defn shift-right [content start length]
  (match content
    nil nil
    [color a x b] [color (shift-right a start length) (shift-mark x start length) (shift-right b start length)]))

(defn shift-left [tree start length]
  (let [end     (dec (+ start length))
        ; TODO - optimize by implementing actual deletion of nodes instead of recreating the whole tree - moliva - 21/6/2015
        new-tree (reduce insert-val nil (filter #(not (between start end (:position %))) (rb-tree->seq tree)))]
    (shift-right new-tree start (- length))))

