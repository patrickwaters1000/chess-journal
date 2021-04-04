(ns chess-journal2.openings.api
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [compojure.core :refer :all]
            [ring.middleware.params :as rmp]
            [org.httpkit.server :refer [run-server]]
            [clj-time.core :as t]
            [cheshire.core :as json]
            [chess-journal2.chess :as chess]
            [chess-journal2.utils :refer [sget]]))

(defn resource [file]
  (str "/home/patrick/chess-journal2/resources/" file))

(defn read-edn-resource [file]
  (-> file resource slurp read-string))

(defn read-json-data [body]
  (-> body slurp json/parse-string walk/keywordize-keys))

(def fen->moves (read-edn-resource "fen_to_moves.edn"))

(def state
  (atom {:fen->note (read-edn-resource "fen_to_notes.edn")
         :san-seq []
         :frame-idx 0
         :locked-variation nil}))

(defn reset [state]
  (assoc state
         :san-seq []
         :frame-idx 0))

(defn get-fen [state]
  (let [san-seq (sget state :san-seq)
        frame-idx (sget state :frame-idx)]
    (reduce chess/apply-move-san
            chess/initial-fen
            (take frame-idx san-seq))))

(defn parse-move [state data]
  (let [fen (get-fen state)
        to (sget data :to)
        from (sget data :from)
        promote (get data :promote)]
    (try (chess/move-squares-to-san fen from to promote)
         (catch Exception e
           (println (.getMsg e))
           nil))))

(defn advance-to-latest-frame [state]
  (let [san-seq (sget state :san-seq)
        num-moves (count san-seq)]
    (assoc state :frame-idx num-moves)))

(defn correct-move? [state san]
  (let [fen (get-fen state)
        moves (sget fen->moves fen)]
    (assert (set? moves))
    (contains? moves san)))

(defn move [state san]
  (-> state
      (update :san-seq conj san)
      advance-to-latest-frame))

(defn get-random-move [state]
  (let [fen (get-fen state)
        moves (sget fen->moves fen)]
    (first (shuffle moves))))

(defn variation-continues? [state]
  (let [fen (get-fen state)
        moves (get fen->moves fen [])]
     (seq moves)))

(defn opponent-move [state]
  (let [san-seq (sget state :san-seq)
        frame-idx (sget state :frame-idx)
        locked-variation (sget state :locked-variation)
        num-moves (count san-seq)
        next-move (if (and (some? locked-variation)
                           (< num-moves (count locked-variation)))
                    (nth locked-variation (inc num-moves))
                    (get-random-move state))]
    (-> state
        (update :san-seq conj next-move)
        advance-to-latest-frame)))

(defn get-note [state]
  (let [fen->note (sget state :fen->note)
        fen (get-fen state)]
    (get fen->note fen "")))

(defn update-fen->note [state note]
  (let [fen (get-fen state)]
    (update state
            :fen->note
            assoc
            fen
            note)))

(defn write-fen->note! []
  (spit (resource "fen_to_notes.edn")
        (sget @state :fen->note)))

(defn lock-variation [state]
  (let [san-seq (sget state :san-seq)
        frame-idx (sget state :frame-idx)]
    (assoc state
           :locked-variation
           (take frame-idx san-seq))))

(defroutes app
  (GET "/" [] (slurp "front/openings/dist/openingsTrainer.html"))
  (GET "/openingsTrainer.js" [] (slurp "front/openings/dist/openingsTrainer.js"))
  (POST "/reset" _
        (swap! state reset))
  (POST "/move" {body :body}
        (let [data (read-json-data body)
              san (parse-move @state data)]
          (println "Attempting a move from state " @state)
          (if (and (some? san)
                   (correct-move? @state san))
            (do (swap! state move san)
                (println "Move is correct. After move state is " @state)
                (json/generate-string
                 {:correct true
                  :fen (get-fen @state)
                  :end (not (variation-continues? @state))}))
            (do (println "Move is not correct")
                (json/generate-string
                 {:correct false
                  :fen (get-fen @state)
                  :end (not (variation-continues? @state))})))))
  (POST "/opponent-move" _
       (if (variation-continues? @state)
         (do (println "Variation continues. Getting pone move from "
                      @state)
             (swap! state opponent-move)
             (println "Sending opponent move from state" @state)
             (json/generate-string
              {:fen (get-fen @state)
               :note (get-note @state)}))
         (do (println "Variation does not continue. State = " @state)
             (json/generate-string
              {:fen (get-fen @state)
               :note (get-note @state)}))))
  (GET "/note" _
       (json/generate-string
        {:note (get-note @state)}))
  (POST "/note" {body :body}
        (let [data (read-json-data body)
              note (sget data :note)]
          (println "Received note " note)
          (swap! state update-fen->note note)
          (println "After updating notes, state is " @state)
          (write-fen->note!)
          (json/generate-string
           {:cool-number 7})))
  (POST "/lock-variation" _
        (swap! state lock-variation)
        nil)
  (POST "/unlock-variation" _
        (swap! state assoc :locked-variation nil)
        nil))

(defn -main []
  (println "Ready!")
  (run-server (rmp/wrap-params app)
              {:port 5000}))
