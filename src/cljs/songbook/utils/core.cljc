(ns songbook.utils.core
  (:require [clojure.string :as str]))

(defn normalize-string [string]
  (str/replace string (char 160) " "))

(defn find-first [predicate collection]
  (->> collection
       (filter predicate)
       first))

(defn index-of [vector element]
  (.indexOf (to-array vector) element))

(defn updatem [map key val & kvs]
  (let [ret (update map key val)]
    (if kvs
      (if (next kvs)
        (recur ret (first kvs) (second kvs) (nnext kvs))
        (throw #?(:clj  (IllegalArgumentException.
                               "updatem expects even number of arguments after map/vector, found odd number")
                  :cljs (js/Error.
                               "updatem expects even number of arguments after map/vector, found odd number"))))
      ret)))

(defn between [start end number]
  (and (>= number start) (<= number end)))

