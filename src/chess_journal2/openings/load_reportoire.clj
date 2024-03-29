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

(defn get-lines [reportoire variations]
  (reduce (fn [lines variation]
            (let [new-lines (get reportoire variation)]
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
          variations))

(defn build-fen->sans [lines]
  (->> lines
       (map split-san)
       (reduce add-line
               {})))

(def resources-dir "/home/patrick/chess-journal2/resources/")
(def target-file (str resources-dir "fen_to_moves.edn"))

(defn load-reportoire! [file variations]
  (let [data (-> (str resources-dir file)
                 slurp
                 read-string)
        lines (get-lines data variations)
        fen->sans (build-fen->sans lines)]
    (spit target-file fen->sans)))

(comment
  (load-reportoire! "white_reportoire.edn"
                    [:two-knights-caro-g6-variation
                     :two-knights-caro-dxe4-variation
                     :two-knights-caro-Bg4-variation
                     :two-knights-caro-Nf6-variation
                     :sicilian-rossolimo-sidelines
                     :sicilian-rossolimo-fianchetto-variation
                     :sicilian-rossolimo-e6-variation
                     :sicilian-rossolimo-d6-variation
                     :sicilian-rossolimo-moscow-Nd7
                     :sicilian-rossolimo-moscow-Bd7
                     :sicilian-delayed-alapin
                     :scandinavian-modern-variation
                     :scandinavian-Qe5+
                     :scandinavian-Qd6
                     :scandinavian-Qd8
                     :scandinavian-Qa5
                                        ;:latvian-gambit
                     :alekhines-defence
                     :pirc-defence
                     :modern-defence
                     :owen-defence
                     :st-george-defence
                     :nimzowitsch-defence])
  nil)
