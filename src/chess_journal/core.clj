(ns chess-journal.core
  (:require [clojure.string :as string]
            [compojure.core :refer :all]
            ;;[compojure.route :as route]
            ;;[ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [org.httpkit.server :refer [run-server]]
            [clj-time.core :as t]
            [cheshire.core :as json]
            [chess-journal.chess :as chess]
            [chess-journal.db :as db]))

(def state
  (atom nil))

(defn reset-state! []
  (reset! state
          {:game-id (db/get-most-recent-game-id)
           :position-id (db/get-position-id chess/initial-fen)
           :history []}))

(reset-state!)

(defn next-move! []
  (swap! state
         (fn [s]
           (let [{:keys [position-id history]} s
                 {:keys [line-id]} (last history)
                 {:keys [position-id]} (db/get-next-move
                                        line-id position-id)]
             (-> (assoc s :position-id position-id)
                 (update :history conj [line-id position-id]))))))

(defn get-stuff-for-client []
  (let [{:keys [game-id position-id]} @state
        info (db/get-game-info game-id)
        fen (db/get-fen position-id)
        moves (db/get-moves position-id)]
    (assoc info
           :fen fen
           :moves moves)))

(defroutes the-app
  (GET "/" []
       (do (reset-state!)
           (slurp "dist/chessJournal.html")))
  (GET "/chessJournal.js" []
       (slurp "dist/chessJournal.js"))
  (GET "/info" []
       (-> (get-stuff-for-client)
           json/generate-string))
  (POST "/next-move" []
        (do
          (next-move!)
          (-> (get-stuff-for-client)
              json/generate-string)))
  ;;(route/not-found "Not Found")
  )

(comment
  (def app
    (-> app-routes
        ;;(middleware/wrap-json-body)
        ;;(middleware/wrap-json-response)
        (wrap-defaults api-defaults)
        ;;(wrap-defaults app-routes api-defaults))
        )))

(defn -main []
  (println "Ready!")
  (run-server the-app {:port 5000}))
