(ns songbook.handler
  (:require [compojure.core :refer [GET POST defroutes context]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.refresh :refer [wrap-refresh]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :refer [redirect]]
            ;[ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [prone.middleware :refer [wrap-exceptions]]
            [digest :refer [sha-256]]
            [environ.core :refer [env]]
            [songbook.db :as db]
            [songbook.pages :as pages]))

(defn handle-login [username password]
  (let [hashed (sha-256 password)
        user (db/get-user username)]
    (assoc (redirect "/") :session 
           (if (and (some? user) (= (:hashedPassword user) hashed))
             {:username username} 
             nil))))

(defroutes routes
  (GET "/" {session :session} (pages/application session pages/title (pages/home-page)))
  (GET "/login" {session :session} (pages/application session pages/title (pages/login-page)))
  (GET "/profile" {session :session} (pages/application session pages/title (pages/profile-page (if-some [username (:username session)] (db/get-user username)))))
  (POST "/try-login" {params :params} (handle-login (:username params) (:password params)))
  (context "/chords/:chords-id" [chords-id]
           (GET "/get" [] nil)
           (GET "/edit" [] nil)
           (GET "/delete" [] nil))
  (GET "/chords" [] nil)
  (GET "/chords/create" [] nil)
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
