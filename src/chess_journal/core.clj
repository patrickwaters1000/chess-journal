(ns chess-journal.core
  (:require [clojure.string :as string]
            [compojure.core :refer :all]
            ;;[compojure.route :as route]
            ;;[ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.params :as rmp]
            [org.httpkit.server :refer [run-server]]
            [clj-time.core :as t]
            [cheshire.core :as json]
            [chess-journal.chess :as chess]
            [chess-journal.db :as db]))

(def state
  (atom nil))

(defn move-counter-to-ply [{:keys [active-color full-move-counter]}]
  (+ (* 2 (dec full-move-counter))
     (if (= "b" active-color) 1 0)))

(defn ply-to-move-counter [ply]
  {:full-move-counter (inc (quot ply 2))
   :active-color (if (even? ply) "w" "b")})

(defn reset-state! []
  (let [{game-id :id
         line-id :line_id} (db/get-most-recent-game)]
    (reset! state
            {:ply 0
             :game-id game-id
             :line-id line-id
             :position-id (db/get-position-id chess/initial-fen)
             :history []})))

(reset-state!)

(comment
  @state
  (next-move!)
  nil)

(defn next-move! []
  (swap! state
         (fn [s]
           (let [{:keys [position-id line-id]} s
                 {new-position-id :position_id} (db/get-next-move
                                                 line-id position-id)]
             (-> (assoc s :position-id new-position-id)
                 (update :history conj [line-id position-id])
                 (update :ply inc))))))

(defn drop-last [xs]
  (let [n (count xs)]
    (vec (take (dec n) xs))))

(defn prev-move! []
  (swap! state
         (fn [s]
           (let [{:keys [history]} s
                 [line-id position-id] (last history)]
             (-> (assoc s :line-id line-id :position-id position-id)
                 (update :history drop-last)
                 (update :ply dec))))))

(defn goto-move! [full-move-counter active-color]
  (let [old-ply (:ply @state)
        new-ply (move-counter-to-ply
                 {:full-move-counter full-move-counter
                  :active-color active-color})
        diff (Math/abs (- new-ply old-ply))]
    (cond
      (< old-ply new-ply) (dotimes [i diff] (next-move!))
      (> old-ply new-ply) (dotimes [i diff] (prev-move!)))))

(comment
  (count (db/get-comments 1))
  nil)

(defn get-stuff-for-client []
  (let [{:keys [game-id line-id position-id]} @state
        info (db/get-game-info game-id)
        fen (db/get-fen position-id)
        moves (db/get-moves position-id)
        comments (db/get-comments line-id)]
    (assoc info
           :fen fen
           :moves moves
           :comments comments)))

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
        (do (next-move!)
            (-> (get-stuff-for-client)
                json/generate-string)))
  (POST "/prev-move" []
        (do (prev-move!)
            (-> (get-stuff-for-client)
                json/generate-string)))
  (POST "/goto-move" req
        (let [params (get req :params)
              active-color (get params "activeColor")
              full-move-counter (Integer/parseInt
                                 (get params "fullMoveCounter"))]
          (do (goto-move! full-move-counter active-color)
              (-> (get-stuff-for-client)
                  json/generate-string))))
  (POST "/add-comment" {body :body}
        (let [data (slurp body)
              {:keys [text position-id san]} data]
          (do (println (format "Received comment %s" data))
              (db/ingest-comment! position-id
                                  (subs 0 10 (str (t/now)))
                                  text
                                  (string/split san #" "))
              (-> (get-stuff-for-client)
                  json/generate-string))))
  
  ;;(route/not-found "Not Found")
  )

(comment
  )



(comment
  (def app
    (-> app-routes
        ;;(middleware/wrap-json-body)
        ;;(middleware/wrap-json-response)
        (rmp/wrap-params)
        (wrap-defaults api-defaults)
        ;;(wrap-defaults app-routes api-defaults))
        )))

(defn -main []
  (println "Ready!")
  (run-server (rmp/wrap-params the-app) {:port 5000}))
