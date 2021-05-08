(ns chess-journal2.openings.generate-reportoire
  "Functions for flattening an analysis tree pasted from
  Chess.com. The flattened analysis is a vector of strings, where each
  string is a valid variation in PGN format."
  (:require [clojure.string :as string]))

(defn drop-last [xs]
  (vec (take (dec (count xs)) xs)))

(defn update-last [s f & args]
  (assert vector? s)
  (let [idx (dec (count s))]
    (apply (partial update s idx f) args)))

(defn tokenize-analysis-str [analysis-str]
  (let [tokens (-> analysis-str
                   (string/replace "\n" " ")
                   (string/replace "(" " ( ")
                   (string/replace ")" " ) ")
                   (string/split #" "))]
    (->> tokens
         (remove #(= "" %))
         (remove #(re-matches #"\d+\.\.+" %)))))

(defn get-current-line [path]
  (let [beginning (->> (drop-last path)
                       (map drop-last)
                       (apply concat))
        end (last path)]
    (concat beginning end)))

(defn generate-lines [token-seq]
  (let [lines (atom [])]
    (loop [token-seq token-seq
           path [[]]]
      (let [token (first token-seq)
            new-token-seq (rest token-seq)
            new-path (case token
                       "(" (conj path [])
                       ")" (do (swap! lines conj (get-current-line path))
                               (drop-last path))
                       (update-last path conj token))]
        (if-not (seq new-token-seq)
          (conj @lines (get-current-line new-path))
          (recur new-token-seq new-path))))))

(defn flatten-analysis-str [analysis-str]
  (let [token-seq (tokenize-analysis-str analysis-str)
        lines (generate-lines token-seq)]
    (mapv #(string/join " " %) lines)))

(flatten-analysis-str
 "1. d4 Nf6 2. c4 e6 3. Nc3 Bb4 4. Bd2 O-O 5. a3 (5. Nf3 b6 6. e3 Bb7 7. Bd3 d6 8.
O-O c5) (5. g3 d5 6. cxd5 exd5 7. Bg2 c6) (5. e3 b6 6. Bd3 Bb7 7. f3 c5 8. Nge2
Nc6) 5... Bxc3 6. Bxc3 Ne4 7. Qc2 f5 8. e3 b6"
 )
