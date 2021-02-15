(ns chess-journal.generate-openings-data
  "Generates javascript expressing our opening reportoire tree in a convenient form for the openings trainer app."
  (:require [clojure.edn :as edn]
            [chess-journal.chess :as chess]
            [chess-journal.utils :refer [sget]]
            [cheshire.core :as json]))

(def template "
export const fenToCorrectMoves = %s;
export const fenToOpponentMoves = %s;
export const fenXMoveToFen = %s;")

(defn write [file data]
  (spit file (format template
                     (json/generate-string
                      (sget data :fen->correct-moves))
                     (json/generate-string
                      (sget data :fen->opponent-moves))
                     (json/generate-string
                      (sget data :fen-x-move->fen)))))

(def example-reportoire
  {"e4" {"c5" {"Nf3" {"d6" {"d4" {"cxd4" {"Nxd4" {"Nf6" {"Nc3" {"a6" SicilianNajdorf}}}
                                          "Qxd4" {}
                                          // c3 would transpose to the More gambit
                                          }}
                            "Bb5+" {"Nc6" SicilianRossolimo}
                            "c3" {"Nf6" nil // transposes to Alapin
                                  }
                            "Bc4" {"Nf6" ItalianVsSicilian}
                            "Nc3" {"Nc6" nil // delayed closed Sicilian
                                   }}}
               "c3" {"Nf6" SicilianAlapin}
               "Nc3" {"Nc6" ClosedSicilian}
               "d4" {"cxd4" MoraGambit}
               "f4" {"d5" GrandPrixToiletVariation}
               "Bc4" {"e6" BowdlerAttack}
               ;; b3 and g3 are also possible
               }
         "d5" {"exd5" {"Nf6" ScandinavianModernVariation}}}
   "d4" {"Nf6" {"c4" {"e6" {"Nc3" {"Bb4" NimzoIndianDefence}
                            "Nf3" {;; If 4 Nc3, we can still transpose to the Nimzo
                                   "b6" QueensIndianDefence}
                            "g3" {;; This is the Catalan system. TODO figure out how to play it.
                                  "c5" nil
                                  "d5" nil
                                  }
                            "a3" {"b6" nil ;; 3 a3? is bad, but people play it. Go for a Queen's Indian setup.
                                  }}}
                "Bf4" {"d6" KingsIndianVsLondon}
                "Nf3" {;; NOTE Switching to 2 .. d6 would allow always playing the King's Indian vs the London.
                       "e6" {"c4" {;; Hold out for possibly transposing to the Nimzo.
                                   ;; Fall back to the Queen's Indian.
                                   "b6" nil}
                             "Bf4" {"b6" QueensIndianVsLondon}}}
                "e3" {"b6" {"e6" QueensIndianVsColle}}}}})

(defn build-openings-data [color reportoire]
  (let [fen->correct-moves (atom {})
        fen->opponent-moves (atom {})
        fen-x-move->fen (atom {})]
    (loop [lines [[]]]
      (let [line (first lines)
            fen (reduce chess/apply-move-san
                        chess/initial-fen
                        line)
            active (even? (+ (count line)
                             (case color :black 1 :white 0)))
            subtree (get-in reportoire line {})
            moves (keys subtree)
            move-to-and-from-pairs (for [m moves]
                                     (let [{:keys [from to]}
                                           (chess/get-move-to-and-from
                                            fen m)]
                                       [from to]))
            continuations (for [m moves] (conj line m))
            new-lines (concat continuations (rest lines))]
        (when (seq (vec move-to-and-from-pairs))
          (swap! (if active
                   fen->correct-moves
                   fen->opponent-moves)
                 assoc
                 fen
                 (vec move-to-and-from-pairs)))
        (doseq [[m m*] (map vector
                            moves
                            move-to-and-from-pairs)]
          (swap! fen-x-move->fen
                 assoc
                 (json/generate-string [fen m*])
                 (chess/apply-move-san fen m)))
        (if-not (seq new-lines)
          {:fen->correct-moves @fen->correct-moves
           :fen->opponent-moves @fen->opponent-moves
           :fen-x-move->fen @fen-x-move->fen}
          (recur new-lines))))))

(comment
  (build-openings-data
   :black
   example-reportoire)
  (-main))

(defn -main [& args]
  (->> example-reportoire
       (build-openings-data :black)
       (write "/home/patrick/chess-journal/front/openingsData.js")))
