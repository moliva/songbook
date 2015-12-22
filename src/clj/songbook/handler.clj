(ns songbook.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.refresh :refer [wrap-refresh]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]
            [songbook.pages :as pages]))

(defroutes routes
  (GET "/" [] (pages/application pages/title (pages/home-page)))
  (resources "/")
  (not-found (pages/application "Not found" (pages/not-found-page))))

(def app
  (let [handler (wrap-defaults routes site-defaults)]
    (if (env :dev) (wrap-refresh (wrap-reload (wrap-exceptions handler))) handler)))
