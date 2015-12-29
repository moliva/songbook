(ns songbook.db
  (:require [somnium.congomongo :as mongo]))

(def conn
  (mongo/make-connection "songbook"
                         :host "127.0.0.1"
                         :port 27017))

(defn get-user [username]
  (mongo/with-mongo conn
    (mongo/fetch-one :users :where {:username username})))

(defn get-chords [user]
  (mongo/with-mongo conn
    (mongo/fetch :chords :where {:_id {:$in (:chords user)}})))
