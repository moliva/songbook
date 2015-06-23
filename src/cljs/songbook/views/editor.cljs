(ns songbook.views.editor
    (:require [reagent.core :as reagent :refer [atom]]
              [cljs.core.match :refer-macros [match]]
              [songbook.utils.core :refer [normalize-string updatem index-of]]
              [songbook.model.core :as model :refer [->Mark insert-val rb-tree->ordered-seq shift-right shift-left]]))

(def input-chord-promp-message  "Input a chord for the part")
(def invisible-char "\u00A0")
(def first-lyric-line "A sample lyric line \u266B")
(def default-lyric-line "")
(def line-prototype {:id -1 :lyric default-lyric-line :chord nil})

(def id-gen (atom -1))

(defn next-id []
  (swap! id-gen inc)
  @id-gen)

(defn new-line []
  (assoc line-prototype :id (next-id)))

(def lines (atom [(assoc (new-line) :lyric first-lyric-line)]))

(defn line-div-id [line]
  (str "line-div-" (:id line)))

(defn editor-id [line]
  (str "editor-line-" (:id line)))

(defn add-at [vector val pos]
  (let [split (split-at pos vector)]
    (vec (concat (split 0) [val] (split 1)))))

(defn insert-new-line-before [line]
  (let [position (index-of @lines line)]
    (swap! lines add-at (new-line) position)))

(defn insert-new-line [line]
  (let [position (inc (index-of @lines line))]
    (swap! lines add-at (new-line) position)))

(defn swap-line! [line field value & kvs]
  (swap! lines assoc (index-of @lines line) (apply updatem (concat [line field value] kvs))))

(defn find-end-offset [before after length]		
  (let [length-before (count before)		
        length-after  (count after)]		
    (reduce #(let [index-before (- length-before %2 1)		
                   index-after  (- length-after %2 1)]		
               (if (and 		
                    (= nil %1) 		
                    (not= (nth before index-before) (nth after index-after)))		
                [index-before index-after]
                %1)) 		
           nil (range length))))

(defn find-start-offset [before after length]
  (reduce #(if (and
                 (nil? %1)
                 (not= (nth before %2) (nth after %2)))
             %2
             %1)
          nil (range length)))

(defn diff-region [before after]
  (let [length (min (count before) (count after))]
    (cond
     ; they are equal => no change!
     (= before after)                  {}
     ; empty after => the line was deleted!
     (empty? after)                    {:type :deletion :start 0 :end (dec (count before))}
     ; before is included and is the first part of after => addition!
     (= (.indexOf after before) 0)     {:type :insertion :start (count before) :end (dec (count after))}
     ; before is included and is the last part of after => addition!
     (> (.indexOf after before) 0)     {:type :insertion :start 0 :end (dec (.indexOf after before))}
     ; after is included and is the first part of before => deletion!
     (= (.indexOf before after) 0)     {:type :deletion :start (count after) :end (dec (count before))}
     ; before is included and is the last part of after => deletion!
     (> (.indexOf before after) 0)     {:type :deletion :start 0 :end (dec (.indexOf before after))}
     ; both before and after might share a first part => recurse after it!
     (let [offset (find-start-offset before after length)]
       (> offset 0)) (let [offset        (find-start-offset before after length) 
                           inc-by-offset #(+ offset %)
                           result        (diff-region (.substring before offset) (.substring after offset))]
                       (if (= :replacement (:type result)) 
                         (updatem result :start inc-by-offset :end-before inc-by-offset :end-after inc-by-offset)
                         (updatem result :start inc-by-offset :end inc-by-offset)))
     ; they don't share a first part, but they do share the last part => replacement!
     :else (let [offset (find-end-offset before after length)]
       {:type :replacement :start 0 :end-before (offset 0) :end-after (offset 1)}))))

(defn updated-chords [chord diff]
  (match diff
         {:type :insertion :start start :end end} 
           (shift-right chord start (inc (- end start)))
         {:type :deletion :start start :end end}  
           (shift-left chord start (inc (- end start)))
         {:type :replacement :start start :end-before endb :end-after enda}  
           (let [updated-chord (shift-left chord start (inc (- endb start)))] 
             (shift-right updated-chord start (inc (- enda start))))
         :else chord))

(defn changed-lyrics [line originalLyrics newLyrics]
  (let [diff (diff-region (normalize-string originalLyrics) (normalize-string newLyrics))]
    (if (not (empty? diff))
      (swap-line! line :lyric #(identity newLyrics) :chord #(updated-chords % diff)))))

(defn mark-at [position marks]
  (songbook.model.core/find-val marks position))

(defn chord-prompt [line]
  (let [position      (.-startOffset (.getRangeAt (.getSelection js/window) 0))
        current-mark  (mark-at position (:chord line))
        current-chord (:content current-mark)
        chord         (js/prompt input-chord-promp-message (if (nil? current-chord) "" current-chord))]
             (cond
               ; if the return of the prompt is nil -> cancel!
               (nil? chord) nil
               (and (not (nil? current-mark)) (= chord "")) (swap-line! line :chord #(model/delete-val % current-mark))
               (not= chord "") (swap-line! line :chord #(insert-val % (->Mark position chord))))))

(defn line-input [line]
  [:p.editor-line {:content-editable true
                   :id (editor-id line)
                   :on-context-menu #(do
                                       (.preventDefault %)
                                       (chord-prompt line))
                   :on-input #(let
                                [originalLyrics (:lyric line)
                                 newLyrics      (-> % .-target .-innerText)]
                                 (changed-lyrics line originalLyrics newLyrics))
                   :on-key-press #(let [key (-> % .-key)]
                                          (cond
                                            (and (= "Enter" key) (.-altKey %))
                                              (do
                                                (.preventDefault %)
                                                (chord-prompt line))
                                            (and (= "Enter" key) (.-shiftKey %))
                                              (do
                                                (.preventDefault %)
                                                (insert-new-line-before line))
                                            (= "Enter" key)
                                              (do
                                                (.preventDefault %)
                                                (insert-new-line line))))}
         (:lyric line)])

(defn print-mark [mark]
  (concat (apply str (repeat (:position mark) invisible-char)) (:content mark)))

(defn incremental-positions [offset marks]
  (if (empty? marks)
    marks
    (let [mark     (first marks)
          position (:position mark)
          content  (:content mark)]
      (cons (update mark :position #(- % offset)) (incremental-positions (+ position (count content)) (rest marks))))))

(defn print-marks [marks]
  (reduce #(concat %1 (print-mark %2)) "" (incremental-positions 0 (rb-tree->ordered-seq marks))))

(def initial-focus-wrapper 
  (with-meta identity
    {:component-did-mount #(.focus (reagent/dom-node %))}))

(defn print-line [line should-focus]
  [:div
   {:key (line-div-id line)}
   [:p.chord-line (print-marks (:chord line))]
   (if should-focus [initial-focus-wrapper [line-input line]] [line-input line])])

(defn print-lines []
  (let [last-inserted-line (reduce #(if (> (:id %1) (:id %2)) %1 %2) nil @lines)]
    [:div (map #(print-line % (= last-inserted-line %)) @lines)]))

(defn print-controls []
  [:p
    "Edit lines with lyrics" [:br]
    [:b "Enter | Shift+Enter"] " - For adding a new line after/before the current one" [:br]
    [:b "Alt+Enter | Right click"]  " - For adding a chord in the caret position" [:br]
    [:b "Tab | Shift+Tab"] " - Focus next/previous line"])

(defn editor-page []
  [:div
    [:h2 "Songbook editor!"]
    [print-controls]
    [print-lines]])

