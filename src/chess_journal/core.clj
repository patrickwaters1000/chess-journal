(ns chess-journal.core
  (:require [clojure.string :as string]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults
                                              api-defaults]]
            [clj-time.core :as t]
            [chess-journal.chesslib :as chess]
            [chess-journal.db :as db]))

;; Proposed changes: store "lines" as sequences of moves in algebraic
;; notation (this pertains to games and comments); comments then need
;; a position field; games don't need to store comment-ids lists
;; But, there are tricky things
;;   1 Do we really need a positions table at all?
;;   2 We should store entities in human readable formats; thus a
;;     game's moves in algebraic notation, not a sequence of position
;;     ids.
;;   3 But then, how to join comments to games? (Is a fen hash good
;;     enough?)
;;   4 How to find all games with a given position?
;; So, it seems like the two contenders are:
;;   a Add both the moves and position ids as games columns.
;;   b Keep just the position ids column and write a diff function
;;   c Load an in memory representation of the db, computing fens
;;     from the move sequence as the game is loaded
;;   d Taking (c) even further, pgn notation already supports
;;     annotations and variations and multiple games. Should the "db"
;;     simply be a pgn file?
;;
;; Currently leaning toward (b)
;; Oh, there's another tricky thing!! Comments may pertain to a
;; position from a variation from a game. We have to recursively
;; query the db for additional comments
;;
;; Currently "comments" complect the notions of annotation and variation
;;
;; Points of contact with chesslib
;;   1 Importing a PGN
;;   2 Applying a move to a fen
;;     a client sends move in to/from form to server
;;     b applying san move from client (that's how the client represents
;;       lines)
;;   3 Diff'ing two fens
;;     a when loading a line, have input: position ids,
;;       output alg notation
;;
;; comments have text, line, date, tags
;; note: comments don't need a position id since they implicitly
;; pertain to the first position of their line.
;; line does not have to be a distinct entity,
;;   just a sequence of position ids
;; thus the basic entities are position, game and comment
;;
;;
;; What does the front end actually show?
;; * current state of board
;; * game in algebraic notation?
;; * list of comments (highlighted if applies to current position)
;;
;; What to send to client? (On game load)
;; current-fen (often initial state)
;; san for main line
;; comment-id->comment-text & metadata
;;
;; What state to store on server?
;; NO! game-id -- no
;; NO! comment-id
;; NO! move-idx
;;
;; stack of [[game-id, ply1], [comment-id-1, ply2], [sub-comment-id, ply3]..]
;; current position -- for functionality line adding a variation,
;;   we need game logic available in the UI. If the game logic stays
;;   in the backend, we already know there will be use cases for
;;   talking to the server every move.
;;
;; What to write:
;; Server side
;;   load game, send data to client
;;   update position upon move from client, respond
;;   add a comment
;;   update a comment
;;   delete a comment
;; Client side
;;   request game
;;   show sidebar with comments
;;   click a comment to view the line
;;   --> to follow line (request next fen)
;;   <-- to go back (there is a single stack, but it's on the server)
;;   button to add comment
;;     in "comment mode", clicking a square sends a move to the server
;;        and adds it to the variation
;;     back undoes the move and deletes it from the variation
;;     a text box allows entering a comment
;;     sumbit button
;;   edit/delete comment



(defn add-analysis-line [fen->variations comment]
  (let [{:keys [text position-ids]} comment]
    (first
     (reduce (fn [[fen->variations* pos-id1] pos-id2]
               [(update fen->variations*
                        fen*
                        (fnil conj [])
                        {:comment nil :move move})
                (apply-move fen* move)])
             [(update fen->variations
                       fen
                       (fnil conj [])
                       {:comment text :move (first line)})
              (apply-move fen move)]
             (rest line)))))

(reduce add-analysis-line
          {}
          (cons {:fen initial-position-fen
                 :text nil
                 :line (get game moves)}
                comments))


(defrecord Move
    [id ;; not needed
     fen ;; not needed
     san
     comment
     game-ids ;; maybe 
     ])
;; State
;; fen
;; stack [line, fen] pairs
;; fen -> moves
;; game-id -> metadata

(defn load-line [initial-fen san-seq]
  (loop [fen initial-fen
         san-seq san-seq]
    (when (seq san-seq)
      (let [san (first san-seq)
            fen (game/apply-move-san)]))))

(defn load-game [game-id]
  (let [{:keys [line metadata]} (db/load-game game-id)]
    (swap! state update :game-id->metadata assoc game-id metadata)
    (load-line! game/initial-fen line)))

(def state
  (atom nil))

(defn reset-state! []
  (reset! state
          {:game-id (db/get-most-recent-game-id)
           :position-id (db/get-position-id chess/initial-fen)
           :history []}))

(reset-state!)

(defn get-stuff-for-client []
  (let [{:keys [game-id position-id]} @state
        info (db/get-game-info game-id)
        fen (db/get-fen position-id)
        moves (db/get-moves position-id)]
    (assoc info
           :fen fen
           :moves moves)))

(defroutes app-routes
  (GET "/" []
       (do (reset-state!)
           (slurp "dist/chessJournal.html")))
  (GET "/info" []
       (json/generate-string (get-stuff-for-client)))
  (POST "/" [x]
        (do
          (if (= x "reload") 
            (reset! config start-config)
            (let [face ({"83" 0, "68" 1, 
                         "87" 2, "65" 3,
                         "88" 4, "70" 5}
                        x)] 
              (swap! config turn-face face)))
          (get-page @config)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      ;(middleware/wrap-json-body)
      ;(middleware/wrap-json-response)
      (wrap-defaults api-defaults))
                                        ;(wrap-defaults app-routes api-defaults)
  )
