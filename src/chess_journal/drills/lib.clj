(ns chess-journal.drills
  (:require [chess-journal.chess :as chess]
            [cheshire.core :as json]))

(defn safe-apply-move-san [fen san]
  (try (chess/apply-move-san fen san)
       (catch Exception e
         (throw (Exception.
                 (format "Cannot apply san %s to fen %s"
                         san fen))))))

(defn generate-frames
  [& {:keys [san-seq initial-ply question-comments answer-comments]}]
  (assert (pos? initial-ply))
  (assert (odd? (- (count san-seq) initial-ply))
          (format (str "Length of san seq is %s, initial ply is %s, "
                       "but their difference must be odd.")
                  (count san-seq) initial-ply))
  (let [color (if (even? initial-ply) "w" "b")
        initial-fen (->> (take (dec initial-ply) san-seq)
                         (reduce chess/apply-move-san
                                 chess/initial-fen))
        san-pairs (->> (drop (dec initial-ply) san-seq)
                       (partition 2))]
    (->> (map vector
              san-pairs
              (concat (or question-comments []) (repeat nil))
              (concat (or answer-comments []) (repeat nil)))
         (reduce (fn [{:keys [fen frames]}
                      [[san-1 san-2] question-comment answer-comment]]
                   (let [fen-1 (chess/apply-move-san fen san-1)
                         fen-2 (chess/apply-move-san fen-1 san-2)
                         answer-move (chess/get-move-to-and-from
                                      fen-1 san-2)]
                 {:fen fen-2
                  :frames (conj frames
                                {:active_color color
                                 :answer_move answer-move
                                 :question_comment question-comment
                                 :answer_comment answer-comment
                                 :fen0 fen
                                 :fen1 fen-1
                                 :fen2 fen-2})}))
                 {:fen initial-fen
                  :frames []})
         :frames)))

(defn generate-drill
  [& {:keys [drill-name
             description
             tags
             san-seq
             initial-ply
             question-comments
             answer-comments]
      :as opts}]
  {:name (or drill-name "")
   :description (or description "")
   :tags (or tags [])
   :frames (apply generate-frames
                  (mapcat identity opts))})

(def drills (atom []))

(defn add-drill! [& args]
  (let [new-drill (apply generate-drill args)]
    (swap! drills conj new-drill)))

(defn write-drills! [file]
  (spit file
        (format "export const drills = %s;"
                (json/generate-string
                 (shuffle @drills)))))

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
           "Bf4" "Ne5"
           "Be2"]
 :initial-ply 8
 :answer-comments
 ["Black is bunkering down like a Philidor defence, recover the pawn and play for positional pressure"
  ""
  ""
  "Fighting for control of e5. The bishop would be in the way on e3."
  "Actually, I do not understand this move! Future self, figure out why it is strong."])

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
 :answer-comments
 ["No tactics here. The point of this move is explained later. TODO: study deeper why Nb3 is weaker. Nb5 is fine, but not recommended since it does not actually create problems for Black in this line, and Black enjoys chasing the knight around for a while."
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
 :answer-comments
 ["No tactics here. The point of this move is explained later. TODO: study deeper why Nb3 is weaker. Nb5 is fine, but not recommended since it does not actually create problems for Black in this line, and Black enjoys chasing the knight around for a while."
  ""
  "The point of Bb5+ was to be able to play Ne2 without blocking in the bishop. White can achieve good positional pressure, in some cases getting knights on d5 and f5 at the same time."])

(write-drills! "/home/patrick/chess-journal/front/load-drills.js")
