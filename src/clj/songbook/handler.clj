(ns songbook.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.refresh :refer [wrap-refresh]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :refer [redirect]]
            ;[ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]
            [songbook.pages :as pages]))

(defn handle-login [username password]
  (if (and (= username "root") (= password "123"))
    (assoc (redirect "/") :session {:username username})
    (assoc (redirect "/") :session nil)))

(defn authenticated? [username password]
  (and (= username "root") (= password "123")))

(defroutes routes
  (GET "/" {session :session} (pages/application session pages/title (pages/home-page)))
  (GET "/login" {session :session} (pages/application session pages/title (pages/login-page)))
  (POST "/try-login" {params :params} (handle-login (:username params) (:password params)))
  (resources "/")
  (not-found (pages/application nil "Not found" (pages/not-found-page))))

(def app
  (let [handler (-> routes
                    (wrap-defaults site-defaults)
                    ;(wrap-basic-authentication authenticated?)
                    wrap-session)]
    (if (env :dev)
      (-> handler
          wrap-exceptions 
          (wrap-reload {:cookie-attrs {:max-age 3600 :secure true}})
          wrap-refresh)
      handler)))
