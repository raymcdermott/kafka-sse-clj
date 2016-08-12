(defproject kafka-proxy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/core.async "0.2.385"]
                 [org.apache.kafka/kafka_2.10 "0.10.0.1"]
                 [org.apache.kafka/kafka-clients "0.10.0.1"]]
  :main ^:skip-aot kafka-proxy.core
  :pedantic? :warn
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
