(ns songbook.handler
  (:require [compojure.core :refer [GET POST defroutes context]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.refresh :refer [wrap-refresh]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :refer [redirect]]
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

(defn get-user [session]
  (if-some [username (:username session)] (db/get-user username)))

(defroutes routes
  (GET "/" {session :session} (pages/application session pages/title (pages/home-page)))
  (GET "/login" {session :session} (pages/application session pages/title (pages/login-page)))
  (GET "/profile" {session :session} (pages/application session pages/title (pages/profile-page (get-user session))))
  (POST "/try-login" {params :params} (handle-login (:username params) (:password params)))
  (GET "/chords" [] nil)
  (GET "/chords/create" {session :session} (pages/application session pages/title (pages/chords-creation-page (get-user session))))
  (context "/chords/:chords-id" [chords-id]
           (GET "/" [] nil)
           (GET "/edit" [] nil)
           (GET "/delete" [] nil))
  (GET "/users" {session :session} (pages/application session pages/title (pages/users-page (get-user session))))
  (context "/users/:user-id" [user-id])
           (GET "/" [] nil)
           (GET "/edit" [] nil)
           ;(GET "/delete" [] nil) 
  (resources "/")
  (not-found (pages/application nil "Not found" (pages/not-found-page))))

(def app
  (let [handler (-> routes
                    (wrap-defaults site-defaults)
                    wrap-session)]
    (if (env :dev)
      (-> handler
          wrap-exceptions 
          (wrap-reload {:cookie-attrs {:max-age 3600 :secure true}})
          wrap-refresh)
      handler)))
