(ns songbook.utils.core)

(defn normalize-string [string]
  (clojure.string/replace string (char 160) " "))

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

