(ns songbook.handler
  (:require [compojure.core :refer [GET POST DELETE defroutes context]]
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

(defn handle-create-user [data]
  (db/create-user 
    (dissoc 
      (assoc data :hashedPassword (sha-256 (:password data)) :chords [])
      :__anti-forgery-token :password))
  (redirect "/users"))

(defn handle-delete-user [username]
  (db/delete-user username)
  (redirect "/users"))

(defn handle-delete-chords [username chords-id]
  (db/delete-chords username chords-id)
  (redirect "/profile"))

(defn get-user [session]
  (if-some [username (:username session)] (db/get-user username)))

(defn handle-create-chords [params]
  ; TODO - implement create chords
  )

(defroutes routes
  (GET "/" {session :session} (pages/application session pages/title (pages/home-page)))
  (GET "/login" {session :session} (pages/application session pages/title (pages/login-page)))
  (POST "/login" {params :params} (handle-login (:username params) (:password params)))
  (GET "/profile" {session :session} (pages/application session pages/title (pages/profile-page (get-user session))))
  (context "/chords" []
           (POST "/" {params :params} (handle-create-chords params)))
  (context "/chords" []
           (GET "/" [] nil)
           (GET "/create" {session :session} (pages/application session pages/title (pages/chords-creation-page (get-user session))))
           (context "/:chords-id" [chords-id]
                    (GET "/" [] nil)
                    (GET "/edit" [] nil)
                    (GET "/delete" {session :session} (handle-delete-chords (:username session) chords-id))))
  (context "/users" []
           (POST "/" {params :params} (handle-create-user params))
           (context "/:username" [username]
                    (DELETE "/" [] (handle-delete-user username))))
  (context "/users" []
           (GET "/" {session :session} (pages/application session pages/title (pages/users-page (get-user session))))
           (GET "/create" {session :session} (pages/application session pages/title (pages/users-create-page)))
           (context "/:username" [username]
                    (GET "/" {session :session} (pages/application session pages/title (pages/user-page (db/get-user username))))
                    (GET "/edit" [] nil)
                    (GET "/delete" [] (handle-delete-user username))))
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
