(ns chess-journal2.openings.load-reportoire
  "Functions for converting reportoires from collections of lines to a
  map of fen to moves."
  (:require [clojure.java.io :as io]
            [chess-journal2.chess :as chess]
            [clojure.walk :as walk]))

(defn split-san [san]
  (->> (clojure.string/split san #" ")
       (remove #(re-matches #"\d+\." %))))

(defn add-line [fen->sans line]
  (first
   (reduce (fn [[fen->sans* fen] san]
             [(update fen->sans* fen (fnil conj #{}) san)
              (chess/apply-move-san fen san)])
           [fen->sans
            chess/initial-fen]
           line)))

(defn get-lines [reportoire paths]
  (reduce (fn [lines path]
            (let [new-lines (get-in reportoire path)]
              (if (vector? lines)
                (reduce (fn [lines* line]
                          (if (string? line)
                            (conj lines* line)
                            (throw (Exception.
                                    (str "Invalid line " line)))))
                        lines
                        new-lines)
                (throw (Exception.
                        (str "Expected a vector of lines but got "
                             new-lines))))))
          []
          paths))

(defn build-fen->sans [lines]
  (->> lines
       (map split-san)
       (reduce add-line
               {})))

(def resources-dir "/home/patrick/chess-journal2/resources/")
(def target-file (str resources-dir "fen_to_moves.edn"))

(defn load-reportoire! [file paths]
  (let [data (-> (str resources-dir file)
                 slurp
                 read-string)
        lines (get-lines data paths)
        fen->sans (build-fen->sans lines)]
    (spit target-file fen->sans)))

(comment
  (load-reportoire! "white_reportoire.edn"
                    [;[:sicilian-rossolimo :sidelines]
                     ;[:two-knights-caro :d4-variation]
                     [:latvian-gambit]
                     [:sicilian-delayed-alapin]])
  nil)
