(ns songbook.views.editor
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljs.core.match :refer-macros [match]]
              [songbook.model.core :refer [->Mark insert-val rb-tree->ordered-seq shift-right shift-left]]
              [cljsjs.react :as react])
    (:import goog.History))

(def input-chord-promp-message  "Input a chord for the part")
(def invisible-char "\u00A0")
(def default-lyric-line "A sample lyric line \u266B")

(def lines (atom [{:key 0, :lyric default-lyric-line, :chord nil}]))

(defn insert-new-line []
  (swap! lines conj {:key (inc (:key (last @lines))), :lyric default-lyric-line, :chord nil}))

(defn update! [line field value & kvs]
  (swap! lines assoc (:key line) (apply assoc (reduce #(cons %2 %1) kvs [value field line]))))

(defn find-start-offset [before after length]
  (let [length-before (count before)
        length-after  (count after)]
    (reduce #(if (and
                  (= nil %1)
                  (not= (nth before %2) (nth after %2)))
              %2
              %1)
           nil (range length))))

(defn diff-region [before after]
  (cond
    ; they are equal => no change!
    (= before after)                  {}
    ; empty after => the line was deleted!
    (empty? after)                    {:type :deletion, :start 0, :end (dec (count before))}
    ; before is included and is the first part of after => addition!
    (= (.indexOf after before) 0)     {:type :insertion, :start (count before) , :end (dec (count after))}
    ; before is included and is the last part of after => addition!
    (> (.indexOf after before) 0)     {:type :insertion, :start 0, :end (dec (.indexOf after before))}
    ; after is included and is the first part of before => deletion!
    (= (.indexOf before after) 0)     {:type :deletion, :start (count after) , :end (dec (count before))}
    ; before is included and is the last part of after => deletion!
    (> (.indexOf before after) 0)     {:type :deletion, :start 0, :end (dec (.indexOf before after))}
    ; else we'll go through a deeper analysis
    ; TODO - handle change events (insertions + deletions)
    :else (let [length (min (count before) (count after))
                offset  (find-start-offset before after length)
                result (diff-region (.substring before offset) (.substring after offset))]
      (assoc result :start (+ offset (:start result)) :end (+ offset (:end result))))))

(defn normalize-string [string]
  (clojure.string/replace string (char 160) " "))

(defn updated-chords [line originalLyrics newLyrics diff]
  (let [chord (:chord line)]
    (match diff
          {:type :insertion, :start start, :end end} (shift-right (:chord line) start (inc (- end start)))
          {:type :deletion, :start start, :end end}  (shift-left (:chord line) start (inc (- end start)))
          :else chord)))

(defn changed-lyrics [line originalLyrics newLyrics]
  (let [diff (diff-region (normalize-string originalLyrics) (normalize-string newLyrics))]
    (if (not (empty? diff))
      (update! line :lyric newLyrics :chord (updated-chords line originalLyrics newLyrics diff)))))

(defn chord-prompt [line]
  (let [position (.-startOffset (.getRangeAt (.getSelection js/window) 0))
        chord    (js/prompt input-chord-promp-message)]
             (if (and (not= chord nil) (not= chord ""))
               (update! line :chord (insert-val (:chord line) (->Mark position chord))))))

(defn line-input [line]
  [:p.editor-line {:content-editable true
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
                                            (= "Enter" key)
                                            (do
                                              (.preventDefault %)
                                              (insert-new-line))))}
         (:lyric line)])

(defn print-mark [mark]
  (concat (apply str (repeat (:position mark) invisible-char)) (:content mark)))

(defn incremental-positions [offset marks]
  (if (empty? marks)
    marks
    (let [mark     (first marks)
          position (:position mark)
          content  (:content mark)]
      (cons (->Mark (- position offset) content) (incremental-positions (+ position (count content)) (rest marks))))))

(defn print-string [marks]
  (reduce #(concat %1 (print-mark %2)) "" (incremental-positions 0 (rb-tree->ordered-seq marks))))

(defn new-line [line]
  [:div
   [:p.editor-line.chord-line (print-string (:chord line))]
   [line-input line]])

(defn print-lines []
  [:div (map new-line @lines)])

(defn editor-page []
  [:div
    [:h2 "Songbook editor!"]
    [print-lines]])

