(ns chess-journal.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [chess-journal.chess :as chess]
            [clj-time.core :as t]))

(def db {:user "patrick"
         :dbtype "postgresql"
         :dbname "chess_journal"})

;; NOTE jdbc db-specs must include the db, so I'm not sure how to
;; create a db programatically. To create the db, make sure you have
;; sufficient privliges in /etc/postgresql/10/main/pg_hba.conf, then
;; use psql -U postgres to boot up the client.  Then you can run the
;; create database query.

(def create-chess-journal-db-query "
create database chess_journal with owner patrick;")

(defn create-positions-table! []
  (jdbc/execute! db "
create table positions (
  id serial primary key,
  fen varchar unique);"))

(defn create-moves-table! []
  (jdbc/execute! db "
create table moves (
  id serial primary key,
  initial_position_id integer,
  final_position_id integer,
  san varchar,
  unique (initial_position_id, final_position_id),
  constraint fk_initial_position_id
    foreign key(initial_position_id)
    references positions(id),
  constraint fk_final_position_id
    foreign key(final_position_id)
    references positions(id));"))

;; NOTE it is not possible to decalare each element of an array to be
;; a foreign key.
(defn create-lines-table! []
  (jdbc/execute! db "
create table lines (
  id serial primary key,
  move_ids integer[] unique);"))

(defn create-games-table! []
  (jdbc/execute! db "
create table games (
  id serial primary key,
  date timestamp,
  white varchar,
  black varchar,
  result real,
  line_id integer,
  constraint fk_line_id
    foreign key(line_id)
    references lines(id));"))

(defn create-comments-table! []
  (jdbc/execute! db "
create table comments (
  id serial primary key,
  text varchar,
  position_id integer,
  line_id integer,
  date timestamp,
  constraint fk_position_id
    foreign key(position_id)
    references positions(id),
  constraint fk_line_id
    foreign key(line_id)
    references lines(id));"))



(defn reset-db! []
  (jdbc/execute! db "drop table if exists comments;")
  (jdbc/execute! db "drop table if exists games;")
  (jdbc/execute! db "drop table if exists lines;")
  (jdbc/execute! db "drop table if exists moves;")
  (jdbc/execute! db "drop table if exists positions;")
  (create-positions-table!)
  (create-moves-table!)
  (create-lines-table!)
  (create-games-table!)
  (create-comments-table!))

(comment
  (reset-db!)
  nil)

(defn insert-positions! [fens]
  (let [template "insert into positions(fen) values {{FENS}}"
        param (->> fens
                   (map #(format "('%s')", %))
                   (string/join ", "))
        query (string/replace template "{{FENS}}" param)]
    (jdbc/execute! db query)))

(defn insert-moves! [moves]
  (let [template "
insert into moves
  (initial_position_id, final_position_id, san)
select 
  p1.id, p2.id, t.san
from
  (values {{MOVES}}) as t (initial_fen, final_fen, san)
  left join positions p1 on t.initial_fen = p1.fen
  left join positions p2 on t.final_fen = p2.fen
on conflict do nothing;"
        param (->> moves
                   (map (juxt :initial-fen :final-fen :san))
                   (map #(apply (partial format "('%s', '%s', '%s')") %))
                   (string/join ", "))
        query (string/replace template "{{MOVES}}" param)]
    (jdbc/execute! db query)))

(defn insert-line-and-game! [metadata moves]
  (let [template "
with new_line as (
insert into lines (move_ids)
select array_agg(m.id)
from 
  (values {{MOVES}}) as t (initial_fen, final_fen)
  left join positions p1 on t.initial_fen = p1.fen
  left join positions p2 on t.final_fen = p2.fen
  left join moves m
    on p1.id = m.initial_position_id
    and p2.id = m.final_position_id
returning id)
insert into games (date, result, white, black, line_id)
select {{METADATA}}, id
from new_line"
        {:keys [date
                result
                white
                black]} metadata
        metadata-param (format "'%s', %s, '%s', '%s'"
                               date result white black)
        moves-param (->> moves
                         (map (juxt :initial-fen :final-fen))
                         (map #(apply (partial format "('%s', '%s')") %))
                         (string/join ", "))
        query (-> template
                  (string/replace "{{METADATA}}" metadata-param)
                  (string/replace "{{MOVES}}" moves-param))]
    (jdbc/execute! db query)))

(defn ingest-game! [metadata fens]
  (insert-positions! fens)
  (let [fen-pairs (partition 2 1 fens)
        sans (map #(apply (partial chess/diff-fens-as-san) %)
                  fen-pairs)
        moves (map (fn [[fen1 fen2] san]
                     {:initial-fen fen1
                      :final-fen fen2
                      :san san})
                   fen-pairs
                   sans)]
    (insert-moves! moves)
    (insert-line-and-game! metadata moves)))

(defn get-game-info [game-id]
  (let [game-template "
select white, black, result, date, line_id
from games
where id = {{GAME_ID}};"
        game-query (string/replace game-template
                                   "{{GAME_ID}}"
                                   (str game-id))
        game-result (first (jdbc/query db game-query))
        line-id (:line_id game-result)
        line-template "
select m.san
from lines l cross join unnest(move_ids) as t(move_id)
  left join moves m on t.move_id = m.id 
where l.id = {{LINE_ID}};"
        line-query (string/replace line-template
                                   "{{LINE_ID}}"
                                   (str line-id))
        line-results (jdbc/query db line-query)]
    (assoc game-result :san (map :san line-results))))

(defn get-moves [position-id]
  (let [template "
select m.san, m.final_position_id, l.id as line_id, g.id as game_id
from moves m 
  left join lines l cross join unnest(move_ids) as t(move_id)
    on m.id = t.move_id
  left join games g on g.line_id = l.id
where m.initial_position_id = {{POSITION_ID}}"
        query (string/replace template
                              "{{POSITION_ID}}"
                              (str position-id))]
    (jdbc/query db query)))

(defn get-position-id [fen]
  (let [template "select id from positions where fen = '{{FEN}}';"
        query (string/replace template "{{FEN}}" fen)]
    (-> (jdbc/query db query)
        first
        :id)))

(defn get-fen [position-id]
  (let [template "select fen from positions where id = '{{POS_ID}}';"
        query (string/replace template "{{POS_ID}}" (str position-id))]
    (-> (jdbc/query db query)
        first
        :fen)))

;;(get-position-id chess/initial-fen)
;; (get-fen 4)
(defn get-most-recent-game-id []
  (-> (jdbc/query db "
select id from games
order by date desc
limit 1")
      first
      :id))

(defn get-next-move [line-id position-id]
  (let [template "
select
  m.san as san,
  m.final_position_id as position_id
from
  lines l cross join unnest(move_ids) as t(move_id)
  left join moves m on t.move_id = m.id
where
  l.id = {{LINE_ID}}
  and m.initial_position_id = {{POSITION_ID}};"
        query (-> template
                  (string/replace "{{LINE_ID}}" (str line-id))
                  (string/replace "{{POSITION_ID}}" (str position-id)))]
    (jdbc/query db query)))

;;(get-next-move 1 20)
;;(get-most-recent-game-id)

(comment
  (def example-games-file
    (str "/home/patrick/Downloads/"
         "Theonian_vs_pat_hello_1_2020.10.11.pgn"))
  (def game (chess/read-game example-games-file))
  (def fens (:fens game))
  (def metadata (:metadata game))
  
  (insert-positions! fens)
  (def fen-pairs (partition 2 1 fens))
  (def sans (map #(apply (partial chess/diff-fens-as-san) %)
                 fen-pairs))
  (def moves (map (fn [[fen1 fen2] san]
                    {:initial-fen fen1
                     :final-fen fen2
                     :san san})
                  fen-pairs
                  sans))
  (ingest-game! metadata fens)
  
  (insert-moves! moves)
  (def line-id (insert-line! moves))
  (insert-line-and-game! metadata moves)
  (let [{:keys [metadata fens san]
         :as game} (chess/read-game example-games-file)]
    (chess/diff-fens-as-san
     chess/initial-fen
     (nth fens 0)
     ;;(nth fens 1)
     )
    ;;metadata
    ;(insert-game! game)
    )
  nil

  )

