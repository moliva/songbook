(ns songbook.pages
  (:require [hiccup.page :refer [html5 include-js include-css]]
            [environ.core :refer [env]]))

(defonce title "Ultimate Songbook")

(defn application [title & contents]
  (html5
    [:html
     [:head
      [:title title]
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1"}]
      (include-css (if (env :dev) "css/site.css" "css/site.min.css"))
      (include-css "facss/fa.css")
      (include-css "bootstrapcss/bootstrap.css")]
     [:body
      [:div.container contents]]]))

;(include-js "js/app.js")
;(include-js "js/vendor.min.js")   

(defn home-page []
  (list [:h1#title title]
        [:div#main
         [:div.input-group
          [:input.text.form-control {:type :search :placeholder "Search chords"}]
          [:span.input-group-btn [:a.btn.btn-success "\u266B"]]]]
        [:div#content.row
         [:div#feeds.col-md-6
          [:ul.list-group
           [:li.list-group-item 1] 
           [:li.list-group-item 2]]]
         [:div#recommendations.col-md-6
          [:ul.list-group
           [:li.list-group-item "a"]
           [:li.list-group-item "b"]]]]))

(defn not-found-page []
  (list [:h1 "Not Found"]
        [:div#main]))
