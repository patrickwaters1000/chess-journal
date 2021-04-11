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

;; TODO Currently the server is logging an exception at the end of each variation, trying to get moves for a fen that isn't available.

(defn resource [file]
  (str "/home/patrick/chess-journal2/resources/" file))

(defn read-edn-resource [file]
  (-> file resource slurp read-string))

(defn read-json-data [body]
  (-> body slurp json/parse-string walk/keywordize-keys))

(def fen->moves (read-edn-resource "fen_to_moves.edn"))

(def fen->note
  (atom (read-edn-resource "fen_to_notes.edn")))

(def rng (java.util.Random.))

(defn compute-san [data]
  (let [fen (sget data :fen)
        to (sget data :to)
        from (sget data :from)
        promote (get data :promote)]
    (try (chess/move-squares-to-san fen from to promote)
         (catch Exception e
           (println (.getMsg e))
           nil))))

(defn correct-move? [fen san]
  (let [moves (sget fen->moves fen)]
    (assert (set? moves))
    (contains? moves san)))

(defn get-moves [fen]
  (sget fen->moves fen))

(defn variation-continues? [fen]
  (let [moves (get fen->moves fen [])]
     (seq moves)))

(defn get-note [fen]
  (get @fen->note fen ""))

(defn update-fen->note! [fen note]
  (let [fen->note* (swap! fen->note assoc fen note)]
    (spit (resource "fen_to_notes.edn")
          fen->note*)))

(defroutes app
  (GET "/" [] (slurp "front/openings/dist/openingsTrainer.html"))
  (GET "/openingsTrainer.js" [] (slurp "front/openings/dist/openingsTrainer.js"))
  (POST "/move" {body :body}
        (let [data (read-json-data body)
              fen (sget data :fen)
              san (compute-san data)
              new-fen (chess/apply-move-san fen san)]
          (if (correct-move? fen san)
            (do (println "Move is correct.")
                (json/generate-string
                 {:correct true
                  :fen new-fen
                  :end (not (variation-continues? new-fen))}))
            (do (println "Move is not correct")
                (json/generate-string
                 {:correct false
                  :fen fen
                  :end (not (variation-continues? new-fen))})))))
  (POST "/opponent-moves" {body :body}
        (let [data (read-json-data body)
              fen (sget data :fen)
              sans (get-moves fen)
              fens (map (partial chess/apply-move-san fen) sans)
              notes (map get-note fens)
              idx (when (seq sans)
                    (.nextInt rng (count sans)))]
          (json/generate-string
           {:fens fens
            :sans sans
            :notes notes
            :selected idx ;; Is there a better way?
            })))
  (POST "/note" {body :body}
        (let [data (read-json-data body)
              fen (sget data :fen)
              note (sget data :note)]
          (println "Received note " note)
          (update-fen->note! fen note)
          (json/generate-string
           {:response-to-prevent-404-error 42}))))

;; lein run -m chess-journal2.openings.api

(defn -main []
  (println "Ready!")
  (run-server (rmp/wrap-params app)
              {:port 5000}))
