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
  (assert (odd? (- (count san-seq) initial-ply)))
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
                (json/generate-string @drills))))

(add-drill!
 :name "French defence 1"
 :san-seq ["e4" "e6"
           "d4" "d5"]
 :initial-ply 1)

(add-drill!
 :name "French defence 2"
 :san-seq ["e4" "e6"
           "d4" "d5"
           "Nd2" "Nf6"
           "e5" "Nd7"
           "Bd3" "c5"
           "Nf3"
           ]
 :initial-ply 4)

(write-drills! "/home/patrick/chess-journal/front/load-drills.js")
