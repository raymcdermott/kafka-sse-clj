(defproject com.opengrail/kafka-sse-clj "0.1.0-SNAPSHOT"
  :description "A HTTP Proxy for Kafka using Server-Sent-Events"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.385"]
                 [aleph "0.4.1"]
                 [compojure "1.5.1"]
                 [environ "1.1.0"]
                 [com.github.jkutner/env-keystore "0.1.2"]
                 [org.apache.kafka/kafka_2.10 "0.10.0.1" :exclusions [org.slf4j/slf4j-log4j12 org.slf4j/slf4j-api org.scala-lang/scala-library]]
                 [org.apache.kafka/kafka-clients "0.10.0.1" :exclusions [org.slf4j/slf4j-log4j12 org.slf4j/slf4j-api]]
                 [org.apache.kafka/kafka-tools "0.10.0.1" :exclusions [org.slf4j/slf4j-log4j12 org.slf4j/slf4j-api]]]
  :pedantic? :warn
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})