(ns songbook.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.refresh :refer [wrap-refresh]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]))

(defonce title "Ultimate Songbook")

(def home-page
  (html
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
      [:div.container
       [:h1#title title]
       [:div#main
        [:div.input-group
         [:input.text.form-control {:type :search :placeholder "Search chords"}]
         [:span.input-group-btn [:a.btn.btn-success "\u266B"]]]]
       [:div#content.row
        [:div#feeds.col-md-6]
        [:div#recommendations.col-md-6]]]
      (include-js "js/app.js")
      (include-js "js/vendor.min.js")]]))

(defroutes routes
  (GET "/" [] home-page)
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-defaults routes site-defaults)]
    (if (env :dev) (wrap-refresh (wrap-reload (wrap-exceptions handler))) handler)))
