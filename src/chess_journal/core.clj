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

;; This function would be simpler if but a move could only belong to
;; one line.
(defn get-multitree []
  (reduce (fn [acc move]
            (let [{line_id :line_id
                   fen :initial_fen
                   position_id :initial_position_id
                   active_color :initial_active_color
                   full_move_counter :initial_full_move_counter
                   move_id :move_id
                   san :san
                   next_fen :final_fen
                   next_position_id :final_position_id
                   next_active_color :final_active_color
                   next_full_move_counter :final_full_move_counter} move]
              (-> acc
                  (update position_id
                          (fnil update
                                {:fen fen
                                 :active_color active_color
                                 :full_move_counter full_move_counter
                                 :line-id->move {}})
                          :line-id->move
                          assoc
                          line_id
                          {:san san
                           :next_position_id next_position_id})
                  (update next_position_id
                          (fnil identity
                                {:fen next_fen
                                 :active_color next_active_color
                                 :full_move_counter next_full_move_counter
                                 :line-id->move {}})))))
          {}
          (db/get-all-moves)))

(def multitree
  (atom nil))

(defn reset-multitree! []
  (reset! multitree (get-multitree)))

(defn get-line-data [line-id]
  (let [multitree @multitree
        initial_position_id (db/get-line-start-position-id line-id)]
    (loop [acc []
           move {:san nil
                 :next_position_id initial_position_id}
           variations []]
      (let [{position_id :next_position_id} move
            {:as data
             :keys [line-id->move]} (get multitree position_id)
            position-data (select-keys data [:fen
                                             :active_color
                                             :full_move_counter])
            new-acc (conj acc (assoc position-data
                                     :san (:san move)
                                     :variations variations))
            next-move (get line-id->move line-id)
            next-variations (->> (dissoc line-id->move line-id)
                            (map (fn [[l m]]
                                   {:san (:san m)
                                    :line_id l})))]
        (if-not next-move
          new-acc
          (recur new-acc next-move next-variations))))))

(defn get-game-data [game-id]
  (let [game-data (db/get-game-info game-id)
        line-data (get-line-data (:line_id game-data))]
    (assoc game-data :line line-data)))

;;(db/get-line-start-position-id 2)
;;(get-line-data 1)

(defn sget [m k]
  (if (contains? (set (keys m)) k)
    (get m k)
    (throw (Exception. (format "Map %s does not have key %s"
                               m k)))))

;; Gotcha! Can't use select keys :to since the key will be "to"
(defroutes the-app
  (GET "/" [] (slurp "dist/chessJournal.html"))
  (GET "/chessJournal.js" [] (slurp "dist/chessJournal.js"))
  (GET "/game" []
       (-> (get-game-data 1)
           json/generate-string))
  (GET "/line" req
       (let [params (get req :params)
             line-id (Integer/parseInt (get params "id"))]
         (-> (get-line-data line-id)
             json/generate-string)))
  (POST "/move" {body :body}
        (let [data (json/parse-string (slurp body))
              _ (println "request body: " (str data))
              fen (sget data "fen")
              to (sget data "to")
              from (sget data "from")
              ;; what happens if not legal move??
              san (chess/move-squares-to-san fen from to)
              new-fen (chess/apply-move-san fen san)
              {:keys [active-color
                      full-move-counter]} (chess/parse-fen new-fen)]
          (json/generate-string
           {:fen new-fen
            :active_color active-color
            :full_move_counter full-move-counter
            :san san
            :variations []})))
  (POST "/add-comment" {body :body}
        (let [data (slurp body)
              {:keys [text position-id san]} data]
          (do (println (format "Received comment %s" data))
              (db/ingest-comment! position-id
                                  (subs 0 10 (str (t/now)))
                                  text
                                  (string/split san #" "))
              nil))))

(comment
  (+ 1 1)
  (chess/move-map-to-san
   "rnbqkbnr/pppp1ppp/4p3/8/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2"
   {:from "D1", :to "D3"})
  (chess/move-map-to-san
   "rnbqkbnr/pppp1ppp/4p3/8/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2"
   {:to "F6" :from "G8"}))

(defn -main []
  (println "Building multitree...")
  (reset-multitree!)
  (println "Ready!")
  (run-server (rmp/wrap-params the-app) {:port 5000}))

(comment
  @multitree
  (get-line-data 1)
  (def app
    (-> app-routes
        ;;(middleware/wrap-json-body)
        ;;(middleware/wrap-json-response)
        (rmp/wrap-params)
        (wrap-defaults api-defaults)
        ;;(wrap-defaults app-routes api-defaults))
        )))

(comment
  (defn move-counter-to-ply [{:keys [active-color full-move-counter]}]
    (+ (* 2 (dec full-move-counter))
       (if (= "b" active-color) 1 0)))

  (defn ply-to-move-counter [ply]
    {:full-move-counter (inc (quot ply 2))
     :active-color (if (even? ply) "w" "b")}))
