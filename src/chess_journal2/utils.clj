(ns chess-journal2.utils)

(defn sget [m k]
  (if (contains? (set (keys m)) k)
    (get m k)
    (throw (Exception. (format "Map %s does not have key %s"
                               m k)))))
