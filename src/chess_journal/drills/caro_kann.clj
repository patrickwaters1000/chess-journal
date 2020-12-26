(ns chess-journal.drills.caro-kann
  (:require [chess-journal.drills.lib :refer [generate-drill]]))

(def drills (atom []))

(defn add-drill! [& args]
  (swap! drills conj
         (apply generate-drill args)))

(add-drill!
 :name "Caro-Kann Karpov 1"
 :tags ["Caro-Kann defence" "Karpov variation"]
 :san-seq ["e4" "c6"
           "d4" "d5"
           "Nc3" "dxe4"
           "Nxe4" "Nd7"
           "Bd3" "Ngf6"
           "Ng5" "e6"
           "N1f3"]
 :initial-ply 2)

(add-drill!
 :name "Caro-Kann main line 1"
 :tags ["Caro-Kann defence"]
 :san-seq ["e4" "c6"
           "d4" "d5"
           "Nc3" "dxe4"
           "Nxe4" "Bf5"
           "Ng3" "Bg6"
           "h4" "h6"
           "Nf3" "Nd7"
           "Bd3" "Bxd3"
           "Qxd3" "Nf6"
           "Bd2"]
 :initial-ply 4)

(add-drill!
 :name "Caro-Kann Campomanes 1"
 :tags ["Caro-Kann defence"
        "Campomanes variation"]
 :san-seq ["e4" "c6"
           "d4" "d5"
           "Nc3" "Nf6"
           "e5" "Nfd7"
           "f4" "e6"
           "Nf3" "c5"
           "Be3"]
 :initial-ply 6)

(add-drill!
 :name "Caro-Kann with early e6 1"
 :tags ["Caro-Kann defence" "Sideline"]
 :initial-ply 6
 :san-seq ["e4" "c6"
           "d4" "d5"
           "Nc3" "e6"
           "Nf3" "Bb4"
           "Bd3" "dxe4"
           "Bxe4" "Nf6"
           "Bd3"])

(add-drill!
 :name "Caro-Kann with early e6 2"
 :tags ["Caro-Kann defence" "Sideline"]
 :initial-ply 6
 :san-seq ["e4" "c6"
           "d4" "d5"
           "Nc3" "e6"
           "Nf3" "Nf6"
           "Bd3" "Bb4"
           "O-O" "dxe4"
           "Nxe4" "Nxe4"
           "Bxe4"])

(add-drill!
 :name "Caro-Kann h6 sideline 1"
 :description "Demonstate how to punish Black for essentially wasting a move with an early h6 in the Caro-Kann."
 :tags ["Caro-Kann defence" "Sideline"]
 :initial-ply 6
 :san-seq ["e4" "c6"
           "d4" "d5"
           "Nc3" "h6"
           "Nf3" "Bg4"
           "Bd3" "dxe4"
           "Nxe4" "Bxf3"
           "Qxf3" "Qxd4"
           "Be3" "Qxb2"
           "O-O"])

(add-drill!
 :name "Caro-Kann Tartakower 1"
 :description "Besides the main line and Karpov variation, 3 .. Nf6 is the next most popular variation in the Caro-Kann. White always takes the knight, and then Black can choose the Tartakower or Bronstein-Larsen variations depending on which pawn he recaptures with."
 :tags ["Caro-Kann defence" "Tartakower variation"]
 :initial-ply 6
 :san-seq ["e4" "c5"
           "d4" "d5"
           "Nc3" "dxe4"
           "Nxe4" "Nf6"
           "Nxf6" "exf6"
           "c3" "Bd6"
           "Bd3" "O-O"
           "Qc2" "Re8+"
           "Ne2"])

(add-drill!
 :name "Caro-Kann Bronstein-Larsen 1"
 :description "Besides the main line and Karpov variation, 3 .. Nf6 is the next most popular variation in the Caro-Kann. White always takes the knight, and then Black can choose the Tartakower or Bronstein-Larsen variations depending on which pawn he recaptures with."
 :tags ["Caro-Kann defence" "Bronstein-Larsen variation"]
 :initial-ply 6
 :san-seq ["e4" "c5"
           "d4" "d5"
           "Nc3" "dxe4"
           "Nxe4" "Nf6"
           "Nxf6" "gxf6"
           "c3" "Bf5"
           "g3" "Qd5"
           "Bg2"])
