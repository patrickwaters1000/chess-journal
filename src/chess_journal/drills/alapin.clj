(ns chess-journal.drills.alapin
  (:require [chess-journal.drills.lib :refer [generate-drill]]))

(def drills (atom []))

(defn add-drill! [& args]
  (swap! drills conj
         (apply generate-drill args)))

(add-drill!
 :name "Alapin 1"
 :description "This is the main line of the Alapin."
 :tags ["Sicilian defence" "Alapin"]
 :san-seq ["e4" "c5"
           "c3" "Nf6"
           "e5" "Nd5"
           "d4" "cxd4"
           "Nf3" "e6"
           "cxd4" "d6"
           "Bc4" "Nc6"
           "O-O" "Be7"
           "Qe2" "O-O"]
 :initial-ply 3
 :comments
 ["An active fighting defence. This is Black's strongest response to the Alapin."
  "Any move except 3. e5, and White has lost the first move advantage. White has a variety of options but, and Black can play a dragon setup against essentially all of them."
  ""
  ""
  "TODO Why is this a good move?"])

(add-drill!
 :name "Alapin 2"
 :description "This shows what happens if White delays playing d4 in the Alapin."
 :tags ["Sicilian defence" "Alapin"]
 :san-seq ["e4" "c5"
           "c3" "Nf6"
           "e5" "Nd5"
           "Nf3" "e6"
           "Bc4" "d6"
           "d4" "cxd4"]
 :initial-ply 3)

(add-drill!
 :name "Alapin 3"
 :description ""
 :tags ["Sicilian defence" "Alapin"]
 :san-seq ["e4" "c5"
           "c3" "Nf6"
           "e5" "Nd5"
           "Nf3" "d6"
           "exd6" "e6"
           "c4" "Nf6"
           "d4" "Bxd6"
           "dxc5" "Bxc5"
           "Qxd8" "Kxd8"
           "Nc3" "Ke7"]
 :initial-ply 7)

(add-drill!
 :name "Alapin dubious sac 1"
 :description "White may have chances to sac on f7 in the Alapin. These sacrifices are usually unsound, but can be quite difficult to defend."
 :tags ["Sicilian defence" "Alapin" "Defend sacrifice"]
 :san-seq ["e4" "c5"
           "c3" "Nf6"
           "e5" "Nd5"
           "Nf3" "d6"
           "d4" "cxd4"
           "cxd4" "Nc6"
           "Bc4" "Nb6"
           "Bxf7" "Kxf7"
           "e6+" "Kg8"
           "d5" "Nb4"
           "Nc3" "Qc7"
           "Ng5" "Qc4"
           "Qf3" "Nd3+"
           "Kd2" "Nf4"
           "Ke1" "Nbxd5"
           "Nxd5" "Qxd5"
           "Qxf4" "Bxe6"]
 :initial-ply 7)
