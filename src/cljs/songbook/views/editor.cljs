(ns songbook.views.editor
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.match :refer-macros [match]]
            [clojure.string :as str]
            [songbook.utils.core :refer [normalize-string updatem index-of find-first add-at]]
            [cemerick.url :as cem]
            [songbook.model.core :as model :refer [->Mark insert-val rb-tree->ordered-seq shift-right shift-left]]))

(defonce input-chord-promp-message  "Input a chord for the part")
(defonce invisible-char "\u00A0")
(defonce first-lyric-line "A sample lyric line \u266B")
(defonce default-lyric-line "")
(defonce default-song-name  "song.txt")
(defonce line-prototype {:id -1 :lyric default-lyric-line :chord nil})

(defonce id-gen (atom -1))

(defn next-id []
  (swap! id-gen inc)
  @id-gen)

(defn new-line []
  (assoc line-prototype :id (next-id)))

(defonce lines (atom [(assoc (new-line) :lyric first-lyric-line)]))

(defn line-div-id [line]
  (str "line-div-" (:id line)))

(defn editor-id [line]
  (str "editor-line-" (:id line)))

(defn insert-new-line-before [line]
  (let [position (index-of @lines line)
        nl       (new-line)]
    (swap! lines add-at nl position)
    nl))

(defn insert-new-line [line]
  (let [position (inc (index-of @lines line))
        nl       (new-line)]
    (swap! lines add-at nl position)
    nl))

(defn index-of-id [lines id]
  (let [line (find-first #(= id (:id %)) lines)]
    (index-of lines line)))

(defn swap-line! [line field value & kvs]
  (swap! lines assoc (index-of-id @lines (:id line)) (apply updatem (concat [line field value] kvs))))

(defn find-end-offset [before after length]
  (let [length-before (count before)
        length-after  (count after)]
    (reduce #(let [index-before (- length-before %2 1)
                   index-after  (- length-after %2 1)]
               (if (and
                     (nil? %1)
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

(defn chord-prompt [line]
  (let [position      (-> js/window .getSelection (.getRangeAt 0) .-startOffset)
        current-mark  (model/find-val (:chord line) position)
        current-chord (:content current-mark)
        chord         (js/prompt input-chord-promp-message (if (nil? current-chord) "" current-chord))]
    (cond
      ; if the return of the prompt is nil -> cancel!
      (nil? chord) nil
      (and (not (nil? current-mark)) (= chord "")) (swap-line! line :chord #(model/delete-val % current-mark))
      (not= chord "") (swap-line! line :chord #(insert-val % (->Mark position chord))))))

(defn remove-line [line]
  (if (> (count @lines) 1)
    (reset! lines (->> @lines (filter #(not= line %)) vec))))

(defn insert-new-lines [line new-lines]
  (if (not (empty? new-lines))
    (let [l        (first new-lines)
          new-line (insert-new-line line)]
      (insert-new-lines new-line (rest new-lines))
      (changed-lyrics new-line "" l))))

(defn line-input [line]
  [:p.editor-line.col-md-11 
   {:content-editable true
    :id (editor-id line)
    :on-context-menu #(do
                        (.preventDefault %)
                        (chord-prompt line))
    :on-input #(let
                 [original-lyrics (:lyric line)
                  new-lines       (str/split (-> % .-target .-innerText) "\n")
                  new-lyrics      (first new-lines)]
                 (changed-lyrics line original-lyrics new-lyrics)
                 (insert-new-lines line (rest new-lines)))
    :on-key-down #(let [key (-> % .-key)]
                    (cond
                      (and (= "Backspace" key) (empty? (:lyric line)))
                      (do
                        (.preventDefault %)
                        (remove-line line))))
    ;                   :on-paste #(let [types (-> % .-clipboardData .-types)]
    ;                                (cond
    ;                                  (not= -1 (.indexOf types "text/plain"))
    ;                                    (let [text (-> % .-clipboardData (.getData "text/plain"))
    ;                                          lines (rest (str/split text "\n"))]
    ;                                      (doseq [l lines]
    ;                                        (let [nl (insert-new-line line)]
    ;                                         (changed-lyrics nl "" ))))))
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

(defn mark->txt [mark]
  [(apply str (repeat (:position mark) " ")) (:content mark)])

(defn incremental-positions
  ([marks] (incremental-positions 0 marks))
  ([offset marks]
   (if (empty? marks)
     marks
     (let [mark     (first marks)
           position (:position mark)
           content  (:content mark)]
       (cons (update mark :position #(- % offset)) (incremental-positions (+ position (count content)) (rest marks)))))))

(defn print-marks [marks]
  (reduce #(concat %1 (print-mark %2)) "" (incremental-positions (rb-tree->ordered-seq marks))))

(defn marks->txt [marks]
  (apply str (reduce #(concat %1 (mark->txt %2)) "" (incremental-positions (rb-tree->ordered-seq marks)))))

(defonce initial-focus-wrapper
  (with-meta identity
             {:component-did-mount #(.focus (reagent/dom-node %))}))

(defn line-input-row [line should-focus]
  [:div.row
   ;[:i.col-md-1.fa.fa-edit.fa-2x]
   (if should-focus [initial-focus-wrapper [line-input line]] [line-input line])
   ; TODO - if unique line, disable delete button - miguel
   [:a.text-center {:href "#" :on-click #(remove-line line)} [:i.col-md-1.fa.fa-remove.fa-2x.text-danger]]])

(defn print-line [line should-focus]
  [:div.container-fluid
   {:key (line-div-id line)}
   [:div.row
    ;[:i.col-md-1]
    [:p.chord-line.col-md-11 (print-marks (:chord line))]]
   [line-input-row line should-focus]])

(defn print-lines []
  (let [last-inserted-line (reduce #(if (> (:id %1) (:id %2)) %1 %2) nil @lines)]
    [:div (map #(print-line % (= last-inserted-line %)) @lines)]))

(defn line->txt [line]
  [(marks->txt (:chord line)) (:lyric line)])

; TODO - make model->txt and print-lines polymorphic for transformation - moliva - 23/6/2015
(defn model->txt [lines]
  (->> lines
       (mapcat line->txt)
       (str/join "\n")))

(defn export-song-link []
  (str "data:application/octet-stream;charset=utf-8," (cem/url-encode (model->txt @lines))))

(defn print-controls []
  [:div.pull-right
   [:a.btn.btn-success
    {:style {:margin "0 2px"}
     :href "#save"}
    "Save"]
   [:a.btn.btn-primary
    {:type "button"
     :style {:margin "0 2px"}
     :download default-song-name
     :href (export-song-link)}
    "Export"]
   [:a.btn.btn-info 
    {:disabled true
     :style {:margin "0 2px"}}
    "Import"]])

(defn print-control-instructions []
  [:p.jumbotron
   "Edit lines with lyrics" [:br]
   [:strong "enter | shift+enter"] " - for adding a new line after/before the current one" [:br]
   [:strong "alt+enter | right click"]  " - for adding a chord in the caret position" [:br]
   [:strong "tab | shift+tab"] " - focus next/previous line" [:br]
   [:strong "backspace on empty line | click x"] " - delete a line"])

(defn editor-page []
  [:div.container.main-body 
   [:h3 "Chords editor"]
   [print-control-instructions]
   [print-lines]
   [print-controls]])

