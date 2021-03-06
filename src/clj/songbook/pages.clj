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
      (if (nil? user) 
        [:li [:a {:href "/login"} "Login"]]
        (list
          (if (:admin user) [:li [:a {:href "/users"} "Users"]])
          [:li [:a {:href "/profile"} (:displayName user)]]))]]]])

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

(defn users-create-page []
  [:div.container
   [:h3 "New User"]
   [:div.panel.panel-default
    [:div.panel-body
     [:form.form-horizontal {:action "/users" :method "post"}
      [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery/*anti-forgery-token*}]
      [:div.form-group
       [:label.col-sm-2.control-label {:for "username"} "Username"]
       [:div.col-sm-10 [:input.form-control  {:type "text" :name "username" :placeholder "Username"}]]]
      [:div.form-group
       [:label.col-sm-2.control-label {:for "password"} "Password"]
       [:div.col-sm-10 [:input.form-control  {:type "password" :name "password" :placeholder "Password"}]]]
      [:div.form-group
       [:label.col-sm-2.control-label {:for "displayName"} "Display Name"]
       [:div.col-sm-10 [:input.form-control  {:type "text" :name "displayName" :placeholder "Display Name"}]]]
      [:div.form-group
       [:label.col-sm-2.control-label {:for "avatarImage"} "Avatar"]
       [:div.col-sm-10 [:input {:type "file" :accept "image/*" :name "avatarImage"}]]]
      [:div.form-group
       [:label.col-sm-2.control-label {:for "admin"} "Administrator"]
       [:div.col-sm-10 [:input {:type "checkbox" :name "admin"}]]]
      [:div.form-group
       [:label.col-sm-2.control-label {:for "email"} "Email"]
       [:div.col-sm-10 [:input.form-control  {:type "email" :name "email" :placeholder "user@dom"}]]]
      [:div.form-group
       [:label.col-sm-2.control-label {:for "personalWebsite"} "Personal Website"]
       [:div.col-sm-10 [:input.form-control  {:type "url" :name "personalWebsite" :placeholder "http://www.mywebsite.com"}]]]
      [:div.form-group [:div.col-sm-12.centered-text [:button.btn.btn-success {:type "submit"} "Create"]]]]]]])

(defn users-page [current-user]
  (list
    [:div.container
     [:h3 "Users"]
     [:ul.list-group
      ; list all user chords being able to edit or delete any of them
      (for [user (db/get-users)] 
        (let [username (:username user)]
          [:li.list-group-item 
           [:a {:href (str "/users/" username)} (:displayName user)] " " [:em "(" username ")"] " "
           [:a {:href (str "/users/" username "/edit")} [:i.fa.fa-edit.fa-lg.text-primary]] " "
           [:a {:href (str "/users/" username "/delete")} [:i.fa.fa-remove.fa-lg.text-danger]]]))]
     [:a.btn.btn-success.pull-right  {:href "/users/create"} [:i.fa.fa-remove.fa-plus]]]))

(defn get-chords-url [chords-id]
  (str "/chords/" chords-id))

(defn user-page [user]
  [:div.container
   [:h1 (:displayName user)]
   [:p [:strong "Username "] (:username user)]
   [:p [:strong "Email "] (:email user)]
   [:p [:strong "Personal Website "] (:personalWebsite user)]
   [:h3 "Chords"]
   [:ul.list-group
    ; list all user chords being able to edit or delete any of them
    (for [chords (db/get-chords user)] 
      (let [chords-id (:id chords)]
        [:li.list-group-item 
         [:a {:href (get-chords-url chords-id)} (:name chords)]]))]])

(defn profile-page [user]
  [:div.container
   [:h1 (:displayName user)]
   [:h3 "Chords"]
   [:ul.list-group
    ; list all user chords being able to edit or delete any of them
    (for [chords (db/get-chords user)] 
      (let [chords-id (:id chords)]
        [:li.list-group-item 
         [:a {:href (get-chords-url chords-id)} (:name chords)] " "
         [:a {:href (str "/chords/" chords-id "/edit")} [:i.fa.fa-edit.fa-lg.text-primary]] " "
         [:a {:href (str "/chords/" chords-id "/delete")} [:i.fa.fa-remove.fa-lg.text-danger]]]))]
   [:a.btn.btn-success.pull-right  {:href "/chords/create"} [:i.fa.fa-remove.fa-plus]]])

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
       [:ul.list-group]]
      [:div#recommendations.col-md-6
       [:ul.list-group]]]]))

(defn login-page []
  [:div.container
   [:div.panel.panel-default.small-panel
    [:div.panel-body
     [:form.form-horizontal {:action "/login" :method "post"}
      [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery/*anti-forgery-token*}]
      [:div.form-group
       [:label.col-sm-2.control-label {:for "username"} "Username"]
       [:div.col-sm-10 [:input#username.form-control  {:type "text" :name "username" :placeholder "Username"}]]]
      [:div.form-group
       [:label.col-sm-2.control-label {:for "password"} "Password"]
       [:div.col-sm-10 [:input#password.form-control  {:type "password" :name "password" :placeholder "Password"}]]]
      [:div.form-group 
       [:div.col-sm-offset-2.col-sm-10 [:button.btn.btn-default {:type "submit"} "Login"]]]]]]])

(defn not-found-page []
  [:h1.centered-text "Not Found"])
