(ns songbook.pages
  (:require [hiccup.page :refer [html5 include-js include-css]]
            [hiccup.element :refer [link-to]]
            [ring.middleware.anti-forgery :as anti-forgery]
            [songbook.db :as db]
            [environ.core :refer [env]]))

(defonce title "Ultimate Songbook")

(defn navbar [user]
  [:nav.navbar.navbar-inverse.navbar-fixed-top
   [:div.container-fluid
    [:div.navbar-header
     [:button.navbar-toggle.collapsed {:data-toggle "collapse" :data-target "main-navbar" :aria-expanded "false"}
      [:span.sr-only "Toggle navigation"]
      [:span.icon-bar]
      [:span.icon-bar]
      [:span.icon-bar]]
     [:a.navbar-brand {:href "/"} "\u266B " title]]
    [:div#main-navbar.collapse.navbar-collapse 
     ; navbar site main content
     [:ul.nav.navbar-nav.navbar-right
      [:li (if (nil? user)
             [:a {:href "/login"} "Login"]
             [:a {:href "/profile"} (:displayName user)])]]]]])

(defn application [session title & contents]
  (html5
    [:head
     [:title title]
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))
     (include-css "/facss/fa.css")
     (include-css "/bootstrapcss/bootstrap.css")]
    [:body 
     [:div#container
      (navbar (if-some [username (:username session)] (db/get-user username)))
      [:div.content contents]
      [:footer.nav.navbar-static-bottom.centered-text
       [:p.navbar-text.center-text 
        [:a {:href "http://github.com/moliva" :target "_blank"} [:i.fa.fa-github.fa-lg]]
        " "
        [:a {:href "http://linkedin.com/in/olivamiguel" :target "_blank"} [:i.fa.fa-linkedin.fa-lg]]]]]
     (include-js "/js/vendor.min.js")]))

(defn chords-creation-page [user]
  (list
    [:div#app.container]
    (include-js "/js/vendor.min.js")  
    (include-js "/js/approot.js")))

(defn profile-page [user]
  (list
    [:div.container
     [:h1 (:displayName user)]
     [:h3 "Chords"]
     [:ul.list-group
      ; list all user chords being able to edit or delete any of them
      (for [chords (db/get-chords user)] 
        (let [chords-id (:id chords)]
          [:li.list-group-item 
           [:a {:href (str "chords/" chords-id "/get")} (:name chords)] " "
           [:a {:href (str "chords/" chords-id "/edit")} [:i.fa.fa-edit.fa-lg.text-primary]] " "
           [:a {:href (str "chords/" chords-id "/delete")} [:i.fa.fa-remove.fa-lg.text-danger]]]))]
     [:a.btn.btn-success.pull-right  {:href "chords/create"} [:i.fa.fa-remove.fa-plus]]]))

(defn home-page []
  (list 
    [:div#main
     [:h1.title title]
     [:div#search
      [:input#search-control.text.form-control {:type :search :placeholder "Search chords \u266B"}]
      [:a.btn.btn-success {:onclick "console.log(\"searching for\", document.getElementById(\"search-control\").value)"} "Search!"]]]
    [:div#content.container
     [:div.row
      [:div#feeds.col-md-6
       [:ul.list-group
        [:li.list-group-item 1] 
        [:li.list-group-item 1] 
        [:li.list-group-item 1] 
        [:li.list-group-item 1] 
        [:li.list-group-item 1] 
        [:li.list-group-item 1] 
        [:li.list-group-item 1] 
        [:li.list-group-item 1] 
        [:li.list-group-item 1] 
        [:li.list-group-item 2]]]
      [:div#recommendations.col-md-6
       [:ul.list-group
        [:li.list-group-item "a"]
        [:li.list-group-item "b"]]]]]))

(defn login-page []
  (list
    [:div.container
     [:div.panel.panel-default.small-panel
      [:div.panel-body
       [:form.form-horizontal {:action "/try-login" :method "post"}
        [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery/*anti-forgery-token*}]
        [:div.form-group
         [:label.col-sm-2.control-label {:for "username"} "Username"]
         [:div.col-sm-10 [:input#username.form-control  {:type "text" :name "username" :placeholder "Username"}]]]
        [:div.form-group
         [:label.col-sm-2.control-label {:for "password"} "Password"]
         [:div.col-sm-10 [:input#password.form-control  {:type "password" :name "password" :placeholder "Password"}]]]
        [:div.form-group [:div.col-sm-offset-2.col-sm-10 [:button.btn.btn-default {:type "submit"} "Login"]]]]]]]))

(defn not-found-page []
  [:h1 "Not Found"])
