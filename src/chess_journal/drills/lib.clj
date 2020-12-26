(ns chess-journal.drills.lib
  (:require [chess-journal.chess :as chess]
            [cheshire.core :as json]))

(defn safe-apply-move-san [fen san]
  (try (chess/apply-move-san fen san)
       (catch Exception e
         (throw (Exception.
                 (format "Cannot apply san %s to fen %s"
                         san fen))))))

(defn generate-frames
  [& {:keys [san-seq initial-ply comments]}]
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
              (concat (or comments []) (repeat nil)))
         (reduce (fn [{:keys [fen frames]}
                      [[san-1 san-2] comment]]
                   (let [fen-1 (chess/apply-move-san fen san-1)
                         fen-2 (chess/apply-move-san fen-1 san-2)
                         answer-move (chess/get-move-to-and-from
                                      fen-1 san-2)]
                 {:fen fen-2
                  :frames (conj frames
                                {:active_color color
                                 :answer_move answer-move
                                 :comment comment
                                 :fen0 fen
                                 :fen1 fen-1
                                 :fen2 fen-2})}))
                 {:fen initial-fen
                  :frames []})
         :frames)))

(defn generate-drill
  [& {:keys [name
             description
             tags
             san-seq
             initial-ply
             comments]
      :as opts}]
  {:name (or name "")
   :description (or description "")
   :tags (or tags [])
   :frames (apply generate-frames
                  (mapcat identity opts))})

