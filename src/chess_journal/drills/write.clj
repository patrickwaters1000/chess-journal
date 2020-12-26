(ns chess-journal.drills.write
  (:require [cheshire.core :as json]
            [chess-journal.drills.alapin :as alapin]
            [chess-journal.drills.caro-kann :as caro-kann]
            [chess-journal.drills.philidor :as philidor]
            [chess-journal.drills.sicilian :as sicilian]))

(defn write-drills! [file]
  (let [drills (concat @alapin/drills
                       @caro-kann/drills
                       @philidor/drills
                       @sicilian/drills)]
    (spit file
          (format "export const drills = %s;"
                  (json/generate-string
                   (shuffle drills))))))

(comment
  (def drills-file "/home/patrick/chess-journal/front/load-drills.js")
  (write-drills! drills-file)
  nil)


