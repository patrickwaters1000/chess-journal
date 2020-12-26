(ns chess-journal.drills.sicilian
  (:require [chess-journal.drills.lib :refer [generate-drill]]))

(def drills (atom []))

(defn add-drill! [& args]
  (swap! drills conj
         (apply generate-drill args)))

(def najdorf-start
  ["e4" "c5"
   "Nf3" "d6"
   "d4" "cxd4"
   "Nxd4" "Nf6"
   "Nc3" "a6"])

(add-drill!
 :name "Sicilian sideline 1"
 :tags ["Sicilian defence" "Early e5"]
 :san-seq ["e4" "c5"
           "Nf3" "d6"
           "d4" "cxd4"
           "Nxd4" "e5"
           "Bb5+" "Bd7"
           "Bxd7" "Qxd7"
           "Ne2"]
 :initial-ply 8
 :comments
 [""
  "No tactics here. The point of this move is explained later. TODO: study deeper why Nb3 is weaker. Nb5 is fine, but not recommended since it does not actually create problems for Black in this line, and Black enjoys chasing the knight around for a while."
  ""
  "The point of Bb5+ was to be able to play Ne2 without blocking in the bishop."])

(add-drill!
 :name "Sicilian sideline 2"
 :tags ["Sicilian defence" "Early e5"]
 :san-seq ["e4" "c5"
           "Nf3" "d6"
           "d4" "cxd4"
           "Nxd4" "Nf6"
           "Nc3" "e5"
           "Bb5+" "Bd7"
           "Bxd7" "Qxd7"
           "Ne2"]
 :initial-ply 10
 :comments
 [""
  "No tactics here. The point of this move is explained later. TODO: study deeper why Nb3 is weaker. Nb5 is fine, but not recommended since it does not actually create problems for Black in this line, and Black enjoys chasing the knight around for a while."
  ""
  "The point of Bb5+ was to be able to play Ne2 without blocking in the bishop. White can achieve good positional pressure, in some cases getting knights on d5 and f5 at the same time."])

(add-drill!
 :name "Najdorf English 1"
 :tags ["Sicilian defence" "Najdorf" "English attack"]
 :san-seq (concat najdorf-start
                  ["Be3" "e5"
                   "Nf3" "Be7"
                   "Bc4" "O-O"
                   "O-O" "Be6"])
 :initial-ply 11
 :comments
 [""
  "We prefer an early e5 unless there is a compelling reason not to play it."
  "Preparing to castle early."
  ""
  "Typical setup. Challenging the Bishop is a strong move and forces White to respond. He can take or retreat to b3."])

(add-drill!
 :name "Najdorf English 2"
 :tags ["Sicilian defence" "Najdorf" "English attack"]
 :san-seq (concat najdorf-start
                  ["Be3" "e5"
                   "Nf3" "Be7"
                   "Bc4" "O-O"
                   "O-O" "Be6"
                   "Bxe6" "fxe6"
                   "Na4" "Ng4"
                   "Qd3" "Nxe3"])
 :initial-ply 23
 :comments ["Find Black's most active defence against Nb6."])

(add-drill!
 :name "Najdorf English 3"
 :tags ["Sicilian defence" "Najdorf" "English attack"]
 :san-seq (concat najdorf-start
                  ["Be3" "e5"
                   "Nf3" "Be7"
                   "Bc4" "O-O"
                   "O-O" "Be6"
                   "Bxe6" "fxe6"
                   "Na4" "Nxe4"
                   "Qd3" "Nf6"
                   "Ng5"])
 :initial-ply 20
 :comments
 ["How can White attack weak squares on the Queenside?"
  "There are actually two ways to punish this greedy pawn grab. Find the tactic that wins the pawn on e6."])

(add-drill!
 :name "Najdorf English 4"
 :tags ["Sicilian defence" "Najdorf" "English attack"]
 :san-seq (concat najdorf-start
                  ["Be3" "e5"
                   "Nf3" "Be7"
                   "Bc4" "O-O"
                   "O-O" "Be6"
                   "Bxe6" "fxe6"
                   "Na4" "Nxe4"
                   "Nb6" "Ra7"
                   "Nd5" "Ra8"
                   "Bb6" "Qc8"
                   "Nc7"])
 :initial-ply 20
 :comments
 ["How can White attack weak squares on the Queenside?"
  "There are actually two ways to punish this greedy pawn grab. Find the tactic that embarasses White by invading b6."])

(add-drill!
 :name "Najdorf English 5"
 :tags ["Sicilian defence" "Najdorf" "English attack"]
 :san-seq (concat najdorf-start
                  ["Be3" "e5"
                   "Nb3" "Be6"
                   "h3" "Be7"
                   "Qf3" "O-O"
                   "O-O-O" "b5"])
 :initial-ply 11
 :question-comments
 [""
  "Now White won't be able to play Bc4"])

(add-drill!
 :name "Najdorf English 6"
 :tags ["Sicilian defence" "Najdorf" "English attack"]
 :san-seq (concat najdorf-start
                  ["Be3" "e5"
                   "Nb3" "Be6"
                   "f4" "exf4"
                   "Bxf4" "Nc6"
                   "Qd2" "d5"])
 :initial-ply 11
 :comments
 [""
  ""
  "Now White won't be able to play Bc4"])

(add-drill!
 :name "Najdorf Bg5 1"
 :tags ["Sicilian defence" "Najdorf" "6Bg5"]
 :san-seq (concat najdorf-start
                  ["Bg5" "Nbd7"
                   "f4" "Qc7"
                   "Qf3" "b5"
                   "O-O-O" "Bb7"
                   "g4" "e6"
                   "Bxf6" "b4"
                   "Nce2" "Nxf6"])
 :initial-ply 11
 :comments
 [""
  ""
  "Reinforcing e5"
  "The queen on f3 invites Bb7, so b5 is strong"])

(add-drill!
 :name "Najdorf Bg5 2"
 :tags ["Sicilian defence" "Najdorf" "6Bg5"]
 :san-seq (concat najdorf-start
                  ["Bg5" "Nbd7"
                   "f4" "Qc7"
                   "Bd3" "g6"
                   "Qf3" "Bg7"])
 :initial-ply 11
 :comments
 [""
  ""
  "Since White has not commited the queen to f3, the idea of b5 and Bb7 isn't as strong here."
  "Follow through with the fianchetto once started."])

(add-drill!
 :name "Najdorf Bg5 3"
 :tags ["Sicilian defence" "Najdorf" "6Bg5"]
 :san-seq (concat najdorf-start
                  ["Bg5" "Nbd7"
                   "f4" "Qc7"
                   "Bd3" "g6"
                   "Qf3" "b5"
                   "e5" "Bb7"
                   "exd6"])
 :initial-ply 16
 :comments
 [""
  "Black's plans are mixed up here; he should have finished his fianchetto"])

(add-drill!
 :name "Najdorf Bg5 3"
 :tags ["Sicilian defence" "Najdorf" "6Bg5"]
 :san-seq (concat najdorf-start
                  ["Bg5" "Nbd7"
                   "f4" "Qc7"
                   "Qe2" "e5"
                   "Nf5" "h6"])
 :initial-ply 11)
