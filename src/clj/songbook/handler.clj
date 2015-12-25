(ns songbook.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.refresh :refer [wrap-refresh]]
            [ring.middleware.session :refer [wrap-session]]
            ;[ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]
            [songbook.pages :as pages]))

(defroutes routes
  (GET "/" [] (pages/application pages/title (pages/home-page)))
  (GET "/login" [] (pages/application pages/title (pages/login-page)))
  (POST "/try-login" [] (pages/application pages/title (pages/try-login-page)))
  (resources "/")
  (not-found (pages/application "Not found" (pages/not-found-page))))

(def app
  (let [handler (-> routes
                    (wrap-defaults site-defaults)
                    ;wrap-anti-forgery
                    wrap-session)]
    (if (env :dev)
      (-> handler
          wrap-exceptions 
          wrap-reload 
          wrap-refresh)
      handler)))
