(ns kafka-proxy.core
  (:require [clojure.core.async :as async :refer [>! <! >!! <!! alts! go go-loop chan close! onto-chan put! timeout]])
  (:gen-class)
  (:import (org.apache.kafka.common.serialization StringSerializer StringDeserializer IntegerSerializer IntegerDeserializer)
           (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)
           (org.apache.kafka.clients.consumer KafkaConsumer)
           (org.apache.kafka.common TopicPartition)))

; TODO: env
(def brokers {"bootstrap.servers" "localhost:9092"})

(def marshalling-options {"key.serializer"     IntegerSerializer
                          "value.serializer"   StringSerializer
                          "key.deserializer"   IntegerDeserializer
                          "value.deserializer" StringDeserializer})

(def subscriber-options {"group.id",          "kafka-proxy"
                         "enable.auto.commit" "false"})

(def producer (KafkaProducer. (merge brokers marshalling-options)))

(defn topic-consumer
  [topic]
  (doto (KafkaConsumer. (merge brokers marshalling-options subscriber-options))
    (.subscribe [topic])))

(defn process-record
  [record]
  (println record))

(defn produce
  [topic k v]
  (.send producer (ProducerRecord. topic k v)))

(def SEEK_TO_END -1)

; TODO convert to go block
(defn consume
  ([topic]
   (consume topic 0))
  ([topic offset]
   (let [consumer (topic-consumer topic)
         partition (TopicPartition. topic 0)
         maybe-data (.poll consumer 100)                    ; assigns the partitions to the subscriber
         seek-effect (.seek consumer partition offset)] ; position to the offset provided

     (while true
       (let [records (.poll consumer 100)]
         (println "records")
         (doseq [record records]
           (clojure.pprint/pprint record)))))))



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
