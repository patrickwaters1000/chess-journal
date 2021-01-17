(ns chess-journal.chess
  (:require [clojure.string :as string]
            [clj-time.core :as t])
  (:import [java.lang Enum]
           [com.github.bhlangonijr.chesslib Board Square Side Piece]
           [com.github.bhlangonijr.chesslib.move
            MoveList Move MoveGenerator]
           [com.github.bhlangonijr.chesslib.game Game]
           [com.github.bhlangonijr.chesslib.pgn PgnHolder PgnIterator]))

;; Public:
;; read-game
;; apply-move-map
;; apply-move-san
;; diff-fens-as-san

(def initial-fen
  "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

(defn- get-metadata [^Game g]
  {:white (.. g getWhitePlayer getName)
   :black (.. g getBlackPlayer getName)
   :date (->> (string/split (.getDate g) #"\.")
              (map #(Integer/parseInt %))
              (apply t/date-time))
   :result (case (.name (.getResult g))
             "WHITE_WON" 1.0
             "BLACK_WON" 0.0
             "DRAW" 0.5)})

(defn- get-fen-seq
  ([moves] (get-fen-seq (Board.) moves))
  ([^Board board moves]
   (let [board* (.clone board)]
     (when (seq moves)
       (.doMove board* (first moves))
       (cons (.getFen board*)
             (lazy-seq (get-fen-seq board* (rest moves))))))))

(let [h (PgnHolder. "pgn/games.pgn")
        _ (.loadPgn h)]
  (first (.getGame h)))

(defn read-game [file]
  (let [h (PgnHolder. file)
        _ (.loadPgn h)
        game (first (.getGame h))
        moves (iterator-seq (.. game getHalfMoves iterator))]
    {:metadata (get-metadata game)
     :fens (cons initial-fen (get-fen-seq moves))}))

(defn parse-game [game]
  (let [moves (iterator-seq (.. game getHalfMoves iterator))]
    {:metadata (get-metadata game)
     :fens (cons initial-fen (get-fen-seq moves))}))

(defn read-games [file]
  (let [h (PgnHolder. file)
        _ (.loadPgn h)
        ;; Despite it's name, `getGame` returns an array containing
        ;; all the games in the file.
        games (.getGame h)]
    (mapv parse-game games)))

(defn get-promote-piece [to-square promote]
  (let [rank (Integer/parseInt (subs to-square 1 2))]
    (case rank
      8 (case promote
          "N" Piece/WHITE_KNIGHT
          "B" Piece/WHITE_BISHOP
          "R" Piece/WHITE_ROOK
          "Q" Piece/WHITE_QUEEN)
      1 (case promote
          "N" Piece/BLACK_KNIGHT
          "B" Piece/BLACK_BISHOP
          "R" Piece/BLACK_ROOK
          "Q" Piece/BLACK_QUEEN))))

(defn apply-move-map [fen {:keys [from to promote]}]
  (let [board (Board.)
        from-square (Square/valueOf (string/upper-case from))
        to-square (Square/valueOf (string/upper-case to))
        promotePiece (when promote
                       (get-promote-piece to promote))
        move (if promote
               (Move. from-square to-square promotePiece)
               (Move. from-square to-square))]
    (.loadFromFen board fen)
    (.doMove board move)
    (.getFen board)))

(defn apply-move-san [fen san]
  (let [board (Board.)
        move-list (MoveList. fen)]
    (.loadFromFen board fen)
    (.loadFromSan move-list san)
    (.doMove board (.removeFirst move-list))
    (.getFen board)))

(defn get-move-to-and-from [fen san]
  (let [board (Board.)
        move-list (MoveList. fen)]
    (.loadFromFen board fen)
    (.loadFromSan move-list san)
    {:from (str (.getFrom (first move-list)))
     :to (str (.getTo (first move-list)))}))

(comment
  (get-move-to-and-from initial-fen "e4")
  nil)

(defn- move-to-san [fen move]
  (let [move-list (MoveList. fen)]
    (.add move-list move)
    (string/trim (.toSan move-list))))

(defn move-squares-to-san [fen from to & [promote]]
  (let [from-square (Square/valueOf (string/upper-case from))
        to-square (Square/valueOf (string/upper-case to))
        promote-piece (when promote
                        (get-promote-piece to promote))
        move (if promote
               (Move. from-square to-square promote-piece)
               (Move. from-square to-square))]
    (move-to-san fen move)))

(defn diff-fens-as-san [fen-1 fen-2]
  (let [board (Board.)]
    (.loadFromFen board fen-1)
    (loop [moves (MoveGenerator/generateLegalMoves board)]
      (let [move (or (first moves)
                     (throw (Exception. "Fail")))
            board* (.clone board)]
        (.doMove board* move)
        (if (= fen-2 (.getFen board*))
          (move-to-san fen-1 move)
          (recur (rest moves)))))))

(defn parse-fen [fen]
  (let [[board
         active-color
         castling
         en-passant
         half-move-clock
         full-move-counter] (string/split fen #" ")]
    {:active-color active-color
     :full-move-counter full-move-counter}))

(comment
  ;; Move these to a test namespace
  
  (def initial-fen
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 0")
  
  (def another-fen
    (apply-move-san initial-fen "Nf3"))

  (diff-fens initial-fen another-fen))
