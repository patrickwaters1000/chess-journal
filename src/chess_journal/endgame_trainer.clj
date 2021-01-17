(ns chess-journal.endgame-trainer
  (:require [clojure.string :as string]
            [chess-journal.engine :as engine]
            [compojure.core :refer :all]
            [ring.middleware.params :as rmp]
            [org.httpkit.server :refer [run-server]]
            [clj-time.core :as t]
            [cheshire.core :as json]
            [chess-journal.chess :as chess]))

(defn sget [m k]
  (if (contains? (set (keys m)) k)
    (get m k)
    (throw (Exception. (format "Map %s does not have key %s"
                               m k)))))

(def engine* (atom nil))

(defroutes the-app
  (GET "/" [] (slurp "dist/endgameTrainer.html"))
  (GET "/endgameTrainer.js" [] (slurp "dist/endgameTrainer.js"))
  (POST "/move" {body :body}
        ;; Return the new fen if proposed move is legal
        (let [data (json/parse-string (slurp body))
              _ (println "request body: " (str data))
              fen (sget data "fen")
              to (sget data "to")
              from (sget data "from")
              promote (get data "promote")
              ;; what happens if not legal move??
              san (chess/move-squares-to-san fen from to promote)
              new-fen (chess/apply-move-san fen san)
              {:keys [active-color
                      full-move-counter]} (chess/parse-fen new-fen)]
          (json/generate-string
           {:fen new-fen
            :san san})))
  (POST "/engine" {body :body}
        ;; Get an engine move from the curent position,
        ;; return the new fen.
        (let [data (json/parse-string (slurp body))
              old-fen (sget data "fen")
              move (.getMove @engine* old-fen 2000)
              new-fen (chess/apply-move-map old-fen move)
              {:keys [from to promote]} move
              san (chess/move-squares-to-san
                   old-fen from to promote)]
          (json/generate-string
           {:fen new-fen
            :san san}))))

;; TODO ensure that engine is terminated on shutdown
(defn -main []
  (println "Starting chess engine...")
  (reset! engine* (engine/new-engine))
  (println "Ready!")
  (run-server (rmp/wrap-params the-app)
              {:port 5000}))
