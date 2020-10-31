(defproject example "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cheshire "5.10.0"]
                 [compojure "1.1.8"]
                 [http-kit "2.1.16"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [clj-time "0.15.2"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [com.github.bhlangonijr/chesslib "1.1.20"]]
  :plugins [[lein-ring "0.12.4"]]
  :ring {:handler chess-journal.core/app}
  :repositories [["jitpack" "https://jitpack.io"]]
  :main ^:skip-aot example.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
