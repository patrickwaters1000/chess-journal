(ns chess-journal2.openings.generate-reportoire
  "Functions for flattening an analysis tree pasted from
  Chess.com. The flattened analysis is a vector of strings, where each
  string is a valid variation in PGN format."
  (:require [clojure.string :as string]))

(defn is-start-of-variation? [token]
  (= \( (.charAt token 0)))

(defn is-end-of-variation? [token]
  (= \) (.charAt token (dec (count token)))))

(defn drop-last [xs]
  (vec (take (dec (count xs)) xs)))

(defn get-current-variation [variations-stack final-token]
  (let [last-part-of-variation (conj (last variations-stack)
                                     (string/replace final-token #"\)" ""))
        beginning-of-variation (->> (drop-last variations-stack)
                                    (mapcat drop-last))]
    (string/join " " (concat beginning-of-variation last-part-of-variation))))

(defn flatten-analysis-tree [analysis-tree-str]
  (let [full-token-seq (string/split
                        (string/replace analysis-tree-str
                                        #"\n"
                                        " ")
                        #" ") 
        results (atom [])]
    (loop [token-seq full-token-seq
           variations-stack [[]]]
      (if-not (seq token-seq)
        @results
        (let [token (first token-seq)
              new-token-seq (rest token-seq)
              new-variations-stack
              (cond
                (is-start-of-variation? token) (let [starts-with-black-move? (re-find #"\.\.\." token)]
                                                 (conj variations-stack
                                                       (if starts-with-black-move?
                                                         []
                                                         [(string/replace token #"\(" "")])))
                (is-end-of-variation? token) (do (swap! results
                                                        conj
                                                        (get-current-variation variations-stack token))
                                                 (drop-last variations-stack))
                :else (update variations-stack (dec (count variations-stack)) conj token))]
          (recur new-token-seq
                 new-variations-stack))))))

