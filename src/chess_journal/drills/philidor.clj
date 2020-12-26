(ns chess-journal.drills.philidor
  (:require [chess-journal.drills.lib :refer [generate-drill]]))

(def drills (atom []))

(defn add-drill! [& args]
  (swap! drills conj
         (apply generate-drill args)))

(add-drill! 
 :name "Scotch gambit 1"
 :tags ["Scotch gambit" "Philidor defence"]
 :san-seq ["e4" "e5"
           "Nf3" "Nc6"
           "d4" "exd4"
           "Bc4" "d6"
           "Nxd4" "Nf6"
           "Nc3" "Be7"
           "O-O" "O-O"
           "Nxc6"]
 :initial-ply 8
 :answer-comments
 ["Black is bunkering down like a Philidor defence, recover the pawn and play for positional pressure"
  ""
  ""])

(add-drill!
 :name "Philidor defence 1"
 :tags ["Philidor defence" "Exchange variation" "Early Bg4"]
 :san-seq ["e4" "e5"
       "Nf3" "d6"
       "d4" "exd4"
       "Nxd4" "Nf6"
       "Nc3" "Bg4"
       "Be2" "Qd7"
       "f3" "Be6"
       "f4" "Bg4"
       "Be3"]
 :initial-ply 10
 :answer-comments
 ["e2 is a great square for the bishop in the Philidor defence."
  "g5 is coming eventually. Black would really be in trouble if he plays Bh5 here."
  "Note that Black an eventual d5 would provoke e5."
  ""]
 :question-comments
 [""
  "Black should have taken on e2. How can you make him regret it?"])

(add-drill!
 :name "Philidor defence 2"
 :tags ["Philidor defence" "Exchange variation" "Early Bg4"]
 :san-seq ["e4" "e5"
           "Nf3" "d6"
           "d4" "exd4"
           "Nxd4" "Nf6"
           "Nc3" "Bg4"
           "Be2" "Bxe2"
           "Qxe2" "Nbd7"
           "O-O" "Be7"
           "Nf5"]
 :initial-ply 10
 :answer-comments
 [""
  ""
  "No hurry, make Black suffer in the Philidor with positional pressure."
  "Excellent square for the knight!"])

(add-drill!
 :name "Philidor defence 3"
 :tags ["Philidor defence" "Exchange variation"]
 :san-seq ["e4" "e5"
           "Nf3" "d6"
           "d4" "Nf6"
           "Nc3" "exd4"
           "Nxd4" "Be7"
           "Be2" "Nd7"
           "Nf5" "O-O"
           "O-O" "Re8"
           "Be3" "Bf8"
           "Ng3" "c6"
           "Qd2"]
 :initial-ply 10
 :answer-comments
 [""
  "Always put the knight there!"
  ""
  "We want to play f4, so not Bf4."
  "Defending the pawn. With the rook on e8, White can run into tactics if he undefends the bishop on e3."
  "Defending the bishop. White now has much greater flexibility."])
