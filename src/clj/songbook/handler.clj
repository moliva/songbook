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
  (assoc (redirect "/") 
         :session (if (and (= username "root") (= password "123"))
                    {:username username} 
                    nil)))

(defroutes routes
  (GET "/" {session :session} (pages/application session pages/title (pages/home-page)))
  (GET "/login" {session :session} (pages/application session pages/title (pages/login-page)))
  (GET "/profile" {session :session} (pages/application session pages/title (pages/profile-page (:username session))))
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
