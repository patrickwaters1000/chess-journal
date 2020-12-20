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
  full_move_counter integer,
  active_color varchar,
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
  -- white_elo integer,
  -- black_elo integer,
  -- time_control varchar,
  -- termination varchar,
  -- event varchar,
  -- round varchar,
  -- site varchar,
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

(defn create-drills-table! []
  (jdbc/execute! db "
create table drills (
  id serial primary key,
  name text,
  tags text[],
  description text,
  line_id integer,
  prompts text[],
  annotations text[]
);"))

(defn reset-db! []
  (jdbc/execute! db "drop table if exists comments;")
  (jdbc/execute! db "drop table if exists games;")
  (jdbc/execute! db "drop table if exists lines;")
  (jdbc/execute! db "drop table if exists moves;")
  (jdbc/execute! db "drop table if exists positions;")
  (jdbc/execute! db "drop table if exists drills;")
  (create-positions-table!)
  (create-moves-table!)
  (create-lines-table!)
  (create-games-table!)
  (create-comments-table!)
  (create-drills-table!))

(defn get-position-id [fen]
  (let [template "select id from positions where fen = '{{FEN}}';"
        query (string/replace template "{{FEN}}" fen)]
    (-> (jdbc/query db query)
        first
        :id)))

(defn get-all-moves []
  (jdbc/query db "
select
  l.id as line_id,
  m.id as move_id,
  m.san,
  m.initial_position_id,
  m.final_position_id,
  p1.fen as initial_fen,
  p1.full_move_counter as initial_full_move_counter,
  p1.active_color as initial_active_color,
  p2.fen as final_fen,
  p2.full_move_counter as final_full_move_counter,
  p2.active_color as final_active_color
from 
  lines l cross join unnest(move_ids) as t(move_id)
  left join moves m on t.move_id = m.id
  left join positions p1 on m.initial_position_id = p1.id
  left join positions p2 on m.final_position_id = p2.id"))

(defn get-all-comments []
  (jdbc/query db "
select 
  line_id, 
  text
from 
  comments"))

(defn get-line-start-position-id [line-id]
  (let [template "
with first_move_id as (
  select move_ids[1] as move_id
  from lines l
  where l.id = {{LINE_ID}})
select m.initial_position_id
from
  first_move_id f 
  left join moves m on f.move_id = m.id"
        query (string/replace template "{{LINE_ID}}" (str line-id))]
    (-> (jdbc/query db query) first :initial_position_id)))

(comment
  (reset-db!)
  nil)

(defn get-fen [position-id]
  (let [template "select fen from positions where id = '{{POS_ID}}';"
        query (string/replace template "{{POS_ID}}" (str position-id))]
    (-> (jdbc/query db query)
        first
        :fen)))

(defn insert-positions! [fens]
  (let [template "
insert into positions(active_color, full_move_counter, fen) 
values {{FENS}}
on conflict do nothing;"
        param (->> fens
                   (map (fn [fen]
                          (let [{:keys [active-color
                                        full-move-counter]}
                                (chess/parse-fen fen)]
                            (format "('%s', %s, '%s')"
                                    active-color full-move-counter fen))))
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

(defn insert-line-and-comment! [position-id date text moves]
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
insert into comments (date, position_id, text, line_id)
select {{FIELDS}}, id
from new_line"
        fields-param (format "'%s', %s, '%s'"
                               date position-id text)
        moves-param (->> moves
                         (map (juxt :initial-fen :final-fen))
                         (map #(apply (partial format "('%s', '%s')") %))
                         (string/join ", "))
        query (-> template
                  (string/replace "{{FIELDS}}" fields-param)
                  (string/replace "{{MOVES}}" moves-param))]
    (println query)
    (jdbc/execute! db query)))

(defn insert-line-and-drill!
  [& {:keys [moves
             drill-name
             description
             tags
             prompts
             annotations]}]
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
insert into drills (name, description, tags, prompts, annotations, line_id)
select {{METADATA}}, id
from new_line"
        tags-str (->> tags
                      (map #(format "'%s'" %))
                      (string/join ", "))
        prompts-str (->> prompts
                         (map #(format "'%s'" %))
                         (string/join ", "))
        annotations-str (->> annotations
                             (map #(format "'%s'" %))
                             (string/join ", "))
        metadata-param (format "'%s', '%s', array[%s]::text[], array[%s]::text[], array[%s]::text[]"
                               drill-name
                               description
                               tags-str
                               prompts-str
                               annotations-str)
        moves-param (->> moves
                         (map (juxt :initial-fen :final-fen))
                         (map #(apply (partial format "('%s', '%s')") %))
                         (string/join ", "))
        query (-> template
                  (string/replace "{{METADATA}}" metadata-param)
                  (string/replace "{{MOVES}}" moves-param))]
    (println query)
    (jdbc/execute! db query)))

(defn insert-line! [moves]
  (let [template "
insert into lines (move_ids)
select array_agg(m.id)
from 
  (values {{MOVES}}) as t (initial_fen, final_fen)
  left join positions p1 on t.initial_fen = p1.fen
  left join positions p2 on t.final_fen = p2.fen
  left join moves m
    on p1.id = m.initial_position_id
    and p2.id = m.final_position_id"
        moves-param (->> moves
                         (map (juxt :initial-fen :final-fen))
                         (map #(apply (partial format "('%s', '%s')") %))
                         (string/join ", "))
        query (-> template
                  (string/replace "{{MOVES}}" moves-param))]
    (jdbc/execute! db query)))

(defn ingest-line! [fen san-seq]
  (let [fen-seq (reductions chess/apply-move-san fen san-seq)
        fen-pairs (partition 2 1 fen-seq)
        moves (map (fn [[fen1 fen2] san]
                     {:initial-fen fen1
                      :final-fen fen2
                      :san san})
                   fen-pairs
                   san-seq)]
    (insert-positions! fen-seq)
    (insert-moves! moves)
    (insert-line! moves)))

(defn ingest-drill!
  [& {:keys [drill-name
             description
             tags
             initial-fen
             san-seq
             prompts
             annotations]}]
  (let [fen-seq (reductions chess/apply-move-san initial-fen san-seq)
        fen-pairs (partition 2 1 fen-seq)
        moves (map (fn [[fen1 fen2] san]
                     {:initial-fen fen1
                      :final-fen fen2
                      :san san})
                   fen-pairs
                   san-seq)]
    (insert-positions! fen-seq)
    (insert-moves! moves)
    (insert-line-and-drill!
     :moves moves
     :drill-name drill-name
     :description description
     :tags tags
     :prompts prompts
     :annotations annotations)))

(defn get-drills-metadata [])

(defn get-drill [id]
  (jdbc/query db "
select
  d.id as drill_id,
  d.name as name,
  array_to_string(d.tags, ',') as tags,
  d.description as description,
  d.
"))

(comment
  (jdbc/execute! db "drop table example")
  (dbbc/execute! db )
  nil)

(def result (jdbc/query db "select * from drills"))

(require '[clojure.reflect :as r])
(r/reflect
 (-> (first result)
    :tags))
(.getArray
 (-> (first result)
    :tags))

(comment
  (ingest-drill! :drill-name "Basic Sicilian"
                 :description "Line defining the Sicilian defence"
                 :tags ["Openings" "Sicilian"]
                 :initial-fen chess/initial-fen
                 :san-seq ["e4" "c5"]
                 :prompts []
                 :annotations [])
  (ingest-drill! :drill-name "Basic Sicilian Najdorf"
                 :description "Line defining the Najdorf variation"
                 :tags ["Openings" "Sicilian" "Najdorf"]
                 :initial-fen chess/initial-fen
                 :san-seq ["e4" "c5"
                           "Nf3" "d6"
                           "d4" "cxd4"
                           "Nxd4" "Nf6"
                           "Nc3" "a6"]
                 :prompts []
                 :annotations [])
  nil)

(defn ingest-game! [metadata fens]
  (let [fen-pairs (partition 2 1 fens)
        sans (map #(apply (partial chess/diff-fens-as-san) %)
                  fen-pairs)
        moves (map (fn [[fen1 fen2] san]
                     {:initial-fen fen1
                      :final-fen fen2
                      :san san})
                   fen-pairs
                   sans)]
    (insert-positions! fens)
    (insert-moves! moves)
    (insert-line-and-game! metadata moves)))

(defn ingest-comment! [fen san-seq comment-text]
  (let [position-id (get-position-id fen)
        date (subs (str (t/now)) 0 10)
        fen-seq (reductions chess/apply-move-san
                            (get-fen position-id)
                            san-seq)
        move-seq (map (fn [[fen1 fen2] san]
                        {:initial-fen fen1
                         :final-fen fen2
                         :san san})
                      (partition 2 1 fen-seq)
                      san-seq)]
    (insert-positions! fen-seq)
    (insert-moves! move-seq)
    (insert-line-and-comment! position-id date comment-text move-seq)))

(defn get-line-sans [line-ids]
  (let [template "
select l.id as line_id, m.san
from lines l cross join unnest(move_ids) as t(move_id)
  left join moves m on t.move_id = m.id 
where l.id in ({{LINE_ID}});"
        param (->> (map str line-ids)
                   (string/join ", "))
        query (string/replace template "{{LINE_ID}}" param)]
    (jdbc/query db query)))

(defn get-all-game-info []
  (jdbc/query db "
select id, white, black, result, date, line_id 
from games"))

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
        line-results (get-line-sans [line-id])]
    (assoc game-result :san (map :san line-results))))

(defn map-vals [f k->v]
  (reduce-kv (fn [acc k v]
               (assoc acc k (f v)))
             {}
             k->v))

(defn get-comments [game-line-id]
  (let [template "
select 
  c.date, 
  c.text, 
  c.position_id, 
  c.line_id, 
  p.full_move_counter, 
  p.active_color
from 
  comments c
  left join positions p 
    on c.position_id = p.id
  left join moves m 
    on c.position_id = m.initial_position_id
  left join lines l 
    cross join unnest(move_ids) as t(move_id)
    on m.id = t.move_id
where l.id = {{LINE_ID}};"
        query (string/replace template "{{LINE_ID}}" (str game-line-id))
        results (jdbc/query db query)
        line-sans (->> (get-line-sans (map :line_id results))
                       (group-by :line_id)
                       (map-vals #(map :san %)))]
    (->> results
         (map #(assoc %
                      :san
                      (get line-sans
                           (:line_id %)))))))



(comment
  (get-line-sans [2])
  (->> (get-line-sans [2])
       (group-by :line_id)
       (map-vals #(map :san %))
       )
  (get-comments 1)
  (chess/apply-move-san (get-fen 6) "Be7")
  (reductions chess/apply-move-san
              (get-fen 6)
              ["Be7"])
  (ingest-comment! 6
                   "2020-10-31" "The white bishop does not really want to be on d2, because this leaves b2 and d4 undefended."
                   ["Be7" "e4" "Nf6" "e5" "Ne4"])
  nil)

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





;;(get-position-id chess/initial-fen)
;; (get-fen 4)
(defn get-most-recent-game []
  (-> (jdbc/query db "
select id, line_id from games
order by date desc
limit 1")
      first))

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
    (first (jdbc/query db query))))

(comment
  ;; Ingest games from REPL
  (def games-data
    (chess/read-games
     "pgn/chess_com_games_2020-11-09.pgn"))
  (->> games-data
       (map (juxt :metadata :fens))
       (run! #(apply ingest-game! %)))

  (def example-games-file
    (str "/home/patrick/Downloads/"
         "Theonian_vs_pat_hello_1_2020.10.11.pgn"))
  (let [game (chess/read-game example-games-file)
        fens (:fens game)
        metadata (:metadata game)]
    (ingest-game! metadata fens))
  (get-all-game-info)
nil)


