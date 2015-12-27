(ns songbook.randoms)

(def VALID-CHARS
  (map char (concat (range 48 58) ; 0-9
                    (range 65 91) ; A-Z
                    (range 97 123)))) ; a-z

(defn random-char []
  (random-nth VALID-CHARS))

(defn random-str [length]
  (apply str (take length (repeatedly random-char))))
