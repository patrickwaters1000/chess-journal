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
  (reduce (fn [acc row]
            (let [{:keys [line_id position_id]} row]
              (update acc
                      position_id
                      (fnil update
                            (select-keys row
                                         [:fen
                                          :full_move_counter
                                          :active_color]))
                      :line-id->move
                      (fnil assoc {})
                      line_id
                      (select-keys row
                                   [:move_id
                                    :san
                                    :next_position_id]))))
          {}
          (db/get-all-moves)))

(def multitree
  (atom nil))

(defn reset-multitree! []
  (reset! multitree (get-multitree)))

(defn get-line-data [line-id]
  (let [multitree @multitree]
    (loop [acc []
           position-id (db/get-line-start-position-id line-id)]
      (let [{:as data
             :keys [line-id->move]} (get multitree position-id)]
        (if-not data
          acc
          (let [next-move (get line-id->move line-id)
                variations (->> (dissoc line-id->move line-id)
                                (map (fn [[line-id move]]
                                       {:san (:san move)
                                        :line_id line-id})))]
            (recur (conj acc
                         (assoc (select-keys data
                                             [:fen
                                              :full_move_counter
                                              :active_color])
                                :san (:san next-move)
                                :variations variations))
                   (:next_position_id next-move))))))))

(defn get-game-data [game-id]
  (let [game-data (db/get-game-info game-id)
        line-data (get-line-data (:line_id game-data))]
    (assoc game-data :line line-data)))

;;(db/get-line-start-position-id 2)
;;(get-line-data 1)


(defroutes the-app
  (GET "/" [] (slurp "dist/chessJournal.html"))
  (GET "/chessJournal.js" [] (slurp "dist/chessJournal.js"))
  (GET "/game" []
       (-> (get-game-data 1)
           json/generate-string))
  (GET "/line" req
       (let [params (get req :params)
             line-id (get params "id")]
         (println "Getting line data for line " line-id)
         (println "The data is " (str (get-line-data line-id)))
         (-> (get-line-data line-id)
             json/generate-string)))
  (POST "/add-comment" {body :body}
        (let [data (slurp body)
              {:keys [text position-id san]} data]
          (do (println (format "Received comment %s" data))
              (db/ingest-comment! position-id
                                  (subs 0 10 (str (t/now)))
                                  text
                                  (string/split san #" "))
              nil))))

(defn -main []
  (println "Building multitree...")
  (reset-multitree!)
  (println "Ready!")
  (run-server (rmp/wrap-params the-app) {:port 5000}))

(comment
  (get-line-data 2)
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
