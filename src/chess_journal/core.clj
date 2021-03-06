(ns chess-journal.core
  (:require [clojure.string :as string]
            [compojure.core :refer :all]
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

(defn get-line-id->comment-text []
  (reduce (fn [acc {:keys [line_id text]}]
            (assoc acc line_id text))
          {}
          (db/get-all-comments)))

(def line-id->comment-text
  (atom nil))

(defn reset-line-id->comment-text! []
  (reset! line-id->comment-text (get-line-id->comment-text)))

(defn get-distinct-line-ids [line-id->variations]
  (let [san->line-id (reduce-kv (fn [acc k v]
                                  (assoc acc (:san v) k))
                                {}
                                line-id->variations)]
    (vals san->line-id)))

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
            distinct-line-ids (get-distinct-line-ids line-id->move)
            next-variations (as-> line-id->move _
                              (select-keys _ distinct-line-ids)
                              (dissoc _ line-id)
                              (map (fn [[l m]]
                                     {:san (:san m)
                                      :line_id l})
                                   _))]
        (if-not next-move
          new-acc
          (recur new-acc next-move next-variations))))))

(defn get-game-data [game-id]
  (let [game-data (db/get-game-info game-id)
        line-id (:line_id game-data)
        line-data (get-line-data line-id)]
    (assoc game-data
           :line
           {:id line-id
            :moves line-data})))

(defn sget [m k]
  (if (contains? (set (keys m)) k)
    (get m k)
    (throw (Exception. (format "Map %s does not have key %s"
                               m k)))))

;; Gotcha! Can't use select keys :to since the key will be "to"
(defroutes the-app
  (GET "/" [] (slurp "dist/chessJournal.html"))
  (GET "/chessJournal.js" [] (slurp "dist/chessJournal.js"))
  (GET "/games-metadata" []
       (->> (db/get-all-game-info)
            (sort-by :date)
            reverse
            json/generate-string))
  (GET "/game" req
       (let [params (sget req :params)
             game-id (Integer/parseInt (sget params "id"))]
         (println "Received request for game " game-id)
         (-> (get-game-data game-id)
             json/generate-string)))
  (GET "/line" req
       (let [params (get req :params)
             line-id (Integer/parseInt (get params "id"))]
         (-> {:id line-id
              :comment (get @line-id->comment-text line-id)
              :moves (get-line-data line-id)}
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
  (POST "/new-annotation" {body :body}
        (let [data (json/parse-string (slurp body))
              fen (sget data "fen")
              san-seq (sget data "san_seq")
              comment-text (sget data "comment_text")]
          (println "Adding new variation; data = " data)
          (db/ingest-comment! fen san-seq comment-text)
          (reset-multitree!)
          (reset-line-id->comment-text!)
          "ok"))
  (GET "/all-drills" []
       (json/generate-string
        (db/get-all-drills))))

(defn -main []
  (reset-line-id->comment-text!)
  (println "Building multitree...")
  (reset-multitree!)
  (println "Ready!")
  (run-server (rmp/wrap-params the-app) {:port 5000}))
