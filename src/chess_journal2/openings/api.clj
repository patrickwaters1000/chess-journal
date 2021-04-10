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

(def fen->note
  (atom (read-edn-resource "fen_to_notes.edn")))

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

(defn get-random-move [fen]
  (let [moves (sget fen->moves fen)]
    (first (shuffle moves))))

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
                  :end (not (variation-continues? fen))}))
            (do (println "Move is not correct")
                (json/generate-string
                 {:correct false
                  :fen fen
                  :end (not (variation-continues? fen))})))))
  (POST "/opponent-move" {body :body}
        (let [data (read-json-data body)
              fen (sget data :fen)
              continue (variation-continues? fen)
              san (when continue (get-random-move fen))
              new-fen (when continue (chess/apply-move-san fen san))]
          (if continue
            (do (println "Variation continues.")
                (json/generate-string
                 {:fen new-fen
                  :note (get-note new-fen)}))
            (do (println "Variation does not continue.")
                (json/generate-string
                 {:fen fen
                  :note (get-note fen)})))))
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
